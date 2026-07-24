import { categoryFor } from "./categories";
import { cleanText, fold, jsonStringArray, truncate } from "./text";
import type { Env, QueueRow, SourceRow } from "./types";

export const AI_MODEL = "@cf/google/gemma-4-26b-a4b-it";
const DEFAULT_OPENROUTER_MODELS = [
  "openrouter/free",
  "nvidia/nemotron-3-super-120b-a12b:free"
];
const PROCESSING_TIMEOUT_MS = 20 * 60 * 1000;
const GENERIC_PHRASES = [
  "sektorel gelismeler ve kamuoyu bilgilendirmesi acisindan onem tasimaktadir",
  "ilgili sektorde ve kamuoyunda genis yanki bulmasi beklenmektedir",
  "kamuoyunu bilgilendirme acisindan onemlidir",
  "sektorel gelismeler acisindan onemlidir"
];

interface ModelAnalysis {
  summary: string;
  whatHappened: string;
  whyImportant: string;
  missingInformation: string;
  possibleImpacts: string[];
  unverifiedClaims: string[];
  contradictions: string[];
}

export interface AnalysisRunResult {
  claimed: number;
  published: number;
  failed: number;
  notificationCandidates: string[];
  errors: string[];
}

function parseObject(raw: string): Record<string, unknown> {
  const cleaned = raw.replace(/```json/gi, "").replace(/```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("AI yaniti JSON nesnesi icermiyor");
  const value: unknown = JSON.parse(cleaned.slice(start, end + 1));
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("AI yaniti nesne degil");
  return value as Record<string, unknown>;
}

function requiredText(value: unknown, field: string, minLength: number): string {
  if (typeof value !== "string") throw new Error(`${field} metin degil`);
  const text = truncate(cleanText(value), 900);
  if (text.length < minLength) throw new Error(`${field} cok kisa`);
  return text;
}

function validateNoGenericText(analysis: ModelAnalysis): void {
  const combined = fold(`${analysis.summary} ${analysis.whatHappened} ${analysis.whyImportant}`);
  if (GENERIC_PHRASES.some((phrase) => combined.includes(phrase))) {
    throw new Error("AI genel gecer kalip metin uretti");
  }
}

function parseAnalysis(raw: string): ModelAnalysis {
  const value = parseObject(raw);
  const analysis: ModelAnalysis = {
    summary: requiredText(value.summary, "summary", 35),
    whatHappened: requiredText(value.what_happened, "what_happened", 30),
    whyImportant: requiredText(value.why_important, "why_important", 35),
    missingInformation: typeof value.missing_information === "string"
      ? truncate(cleanText(value.missing_information), 600) || "Kaynaklarda belirtilmedi."
      : "Kaynaklarda belirtilmedi.",
    possibleImpacts: jsonStringArray(value.possible_impacts),
    unverifiedClaims: jsonStringArray(value.unverified_claims),
    contradictions: jsonStringArray(value.contradictions)
  };
  validateNoGenericText(analysis);
  return analysis;
}

function promptFor(row: QueueRow, sources: SourceRow[]): ChatCompletionsMessagesInput {
  const sourceLines = sources.slice(0, 5)
    .map((source, index) => `${index + 1}. ${source.name}: ${truncate(source.headline, 220)}`)
    .join("\n");
  return {
    messages: [
      {
        role: "system",
        content: [
          "GundemAI icin Turkce haber editorusun.",
          "Yalniz verilen kaynak metnindeki bilgileri kullan; isim, sayi, tarih, neden veya sonuc uydurma.",
          "Her haber icin olaya ozel cumleler yaz. 'sektorel gelismeler' ve 'kamuoyu bilgilendirmesi' gibi kaliplar kullanma.",
          "Bilinmeyen ayrintiyi missing_information alanina yaz.",
          "Sadece gecerli JSON dondur: summary, what_happened, why_important, missing_information, possible_impacts, unverified_claims, contradictions.",
          "Son uc alan her zaman metin dizisi olsun; yoksa []."
        ].join(" ")
      },
      {
        role: "user",
        content: [
          `Baslik: ${truncate(row.raw_title, 300)}`,
          `Kaynak metni: ${truncate(row.raw_description, 1_800)}`,
          `Kaynaklar:\n${sourceLines}`
        ].join("\n\n")
      }
    ],
    max_completion_tokens: 420,
    temperature: 0.2,
    response_format: { type: "json_object" },
    chat_template_kwargs: { enable_thinking: false }
  };
}

async function callAi(env: Env, row: QueueRow, sources: SourceRow[]): Promise<ModelAnalysis> {
  const errors: string[] = [];
  try {
    const output = await env.AI.run(AI_MODEL, promptFor(row, sources));
    const content = output.choices[0]?.message.content;
    if (!content) throw new Error("Workers AI bos yanit verdi");
    return parseAnalysis(content);
  } catch (error) {
    errors.push(`Cloudflare: ${error instanceof Error ? error.message : String(error)}`);
  }

  if (env.OPENROUTER_API_KEY) {
    const configuredModels = env.OPENROUTER_MODELS
      ?.split(",")
      .map((model) => model.trim())
      .filter(Boolean);
    for (const model of configuredModels?.length ? configuredModels : DEFAULT_OPENROUTER_MODELS) {
      try {
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
          method: "POST",
          headers: {
            Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
            "Content-Type": "application/json",
            "HTTP-Referer": "https://gundemai.web.app",
            "X-Title": "GundemAI"
          },
          body: JSON.stringify({
            model,
            ...promptFor(row, sources),
            stream: false
          }),
          signal: AbortSignal.timeout(25_000)
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${truncate(await response.text(), 200)}`);
        }
        const output = await response.json<{
          choices?: Array<{ message?: { content?: string | null } }>;
        }>();
        const content = output.choices?.[0]?.message?.content;
        if (!content) throw new Error("bos yanit");
        return parseAnalysis(content);
      } catch (error) {
        errors.push(`${model}: ${error instanceof Error ? error.message : String(error)}`);
      }
    }
  }
  throw new Error(truncate(errors.join(" | "), 600));
}

function nextRetryAt(error: unknown, attempts: number, now: number): number {
  const message = String(error).toLowerCase();
  if (message.includes("quota") || message.includes("rate") || message.includes("429") || message.includes("limit")) {
    const nextDay = new Date(now);
    nextDay.setUTCHours(24, 5, 0, 0);
    return nextDay.getTime();
  }
  return now + Math.min(6 * 60, 15 * 2 ** Math.min(attempts, 5)) * 60 * 1000;
}

async function claimRows(env: Env, limit: number, now: number): Promise<QueueRow[]> {
  await env.DB.prepare(`
    UPDATE news_items
    SET status = 'RETRY', processing_started_at = NULL, next_attempt_at = ?
    WHERE status = 'PROCESSING' AND processing_started_at < ?
  `).bind(now, now - PROCESSING_TIMEOUT_MS).run();

  const claimed = await env.DB.prepare(`
    UPDATE news_items
    SET status = 'PROCESSING', attempts = attempts + 1, processing_started_at = ?
    WHERE id IN (
      SELECT id FROM news_items
      WHERE status IN ('PENDING', 'RETRY') AND next_attempt_at <= ?
      ORDER BY
        CASE WHEN published_at >= ? THEN 0 ELSE 1 END,
        published_at DESC,
        discovered_at ASC
      LIMIT ?
    )
    RETURNING id, raw_title, raw_description, category_hint, image_url,
      published_at, attempts
  `).bind(now, now, now - 6 * 60 * 60 * 1000, limit).all<QueueRow>();
  return claimed.results;
}

async function sourcesFor(env: Env, articleId: string): Promise<SourceRow[]> {
  const result = await env.DB.prepare(`
    SELECT article_id, name, url, headline, published_at
    FROM news_sources WHERE article_id = ? ORDER BY published_at ASC
  `).bind(articleId).all<SourceRow>();
  return result.results;
}

async function markReady(
  env: Env,
  row: QueueRow,
  sources: SourceRow[],
  analysis: ModelAnalysis,
  now: number
): Promise<void> {
  const category = categoryFor(row.raw_title, row.raw_description, row.category_hint);
  const verificationStatus = sources.length >= 2 ? "MULTI_SOURCE_CONFIRMED" : "SINGLE_SOURCE_REPORT";
  const confidenceScore = sources.length >= 2 ? 82 : 70;
  await env.DB.prepare(`
    UPDATE news_items SET
      status = 'READY',
      title = ?,
      summary = ?,
      category = ?,
      what_happened = ?,
      why_important = ?,
      missing_information = ?,
      verification_status = ?,
      confidence_score = ?,
      possible_impacts = ?,
      unverified_claims = ?,
      contradictions = ?,
      verified_facts = ?,
      ready_at = ?,
      next_attempt_at = 0,
      processing_started_at = NULL,
      last_error = NULL
    WHERE id = ?
  `).bind(
    row.raw_title,
    analysis.summary,
    category,
    analysis.whatHappened,
    analysis.whyImportant,
    analysis.missingInformation,
    verificationStatus,
    confidenceScore,
    JSON.stringify(analysis.possibleImpacts),
    JSON.stringify(analysis.unverifiedClaims),
    JSON.stringify(analysis.contradictions),
    JSON.stringify([row.raw_title]),
    now,
    row.id
  ).run();
}

async function markRetry(env: Env, row: QueueRow, error: unknown, now: number): Promise<string> {
  const message = error instanceof Error ? error.message : String(error);
  await env.DB.prepare(`
    UPDATE news_items SET
      status = 'RETRY',
      next_attempt_at = ?,
      processing_started_at = NULL,
      last_error = ?
    WHERE id = ?
  `).bind(nextRetryAt(error, row.attempts, now), truncate(message, 600), row.id).run();
  return `${row.id}: ${message}`;
}

export async function analyzePending(env: Env, now = Date.now()): Promise<AnalysisRunResult> {
  const requested = Number.parseInt(env.ANALYSES_PER_RUN ?? "3", 10);
  const limit = Number.isFinite(requested) ? Math.max(1, Math.min(requested, 4)) : 3;
  const rows = await claimRows(env, limit, now);
  const notificationCandidates: string[] = [];
  const errors: string[] = [];

  for (const row of rows) {
    try {
      const sources = await sourcesFor(env, row.id);
      if (sources.length === 0) throw new Error("Haberin kaynak kaydi bulunamadi");
      const analysis = await callAi(env, row, sources);
      await markReady(env, row, sources, analysis, now);
      notificationCandidates.push(row.id);
    } catch (error) {
      errors.push(await markRetry(env, row, error, now));
    }
  }

  return {
    claimed: rows.length,
    published: notificationCandidates.length,
    failed: errors.length,
    notificationCandidates,
    errors
  };
}
