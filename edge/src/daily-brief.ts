import { AI_MODEL, runAiPrompt } from "./ai-client";
import { cleanText, truncate } from "./text";
import type { Env } from "./types";

const ISTANBUL_TIME_ZONE = "Europe/Istanbul";
const GENERATION_TIMEOUT_MS = 20 * 60 * 1000;
const RETRY_DELAY_MS = 60 * 60 * 1000;
const CANDIDATE_WINDOW_MS = 36 * 60 * 60 * 1000;
const MIN_CANDIDATES = 4;
const MAX_CANDIDATES = 24;
const MIN_ITEMS = 3;
const MAX_ITEMS = 5;

export interface DailyBriefCandidate {
  id: string;
  title: string;
  summary: string;
  category: string;
  why_important: string;
  published_at: number;
}

interface ModelBriefItem {
  articleId: string;
  summary: string;
}

interface DailyBriefRow {
  date_key: string;
  title: string;
  summary: string;
  items_json: string;
  generated_at: number;
}

export interface DailyBriefItem {
  articleId: string;
  title: string;
  summary: string;
  category: string;
  publishedAt: number;
}

export interface DailyBrief {
  dateKey: string;
  title: string;
  summary: string;
  items: DailyBriefItem[];
  generatedAt: number;
  shared: true;
}

export interface DailyBriefRunResult {
  generated: boolean;
  skipped: boolean;
  dateKey: string;
  itemCount: number;
  error?: string;
}

function parseObject(raw: string): Record<string, unknown> {
  const cleaned = raw.replace(/```json/gi, "").replace(/```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("Gunluk ozet AI yaniti JSON nesnesi icermiyor");
  const value: unknown = JSON.parse(cleaned.slice(start, end + 1));
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("Gunluk ozet AI yaniti nesne degil");
  }
  return value as Record<string, unknown>;
}

export function istanbulDateKey(now = Date.now()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: ISTANBUL_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(new Date(now));
}

export function parseDailyBriefSelection(
  raw: string,
  candidates: DailyBriefCandidate[]
): { summary: string; items: ModelBriefItem[] } {
  const value = parseObject(raw);
  const summary = typeof value.summary === "string"
    ? truncate(cleanText(value.summary), 420)
    : "";
  if (summary.length < 45) throw new Error("Gunluk ozet metni cok kisa");
  if (!Array.isArray(value.items)) throw new Error("Gunluk ozet maddeleri dizi degil");

  const candidateIds = new Set(candidates.map((candidate) => candidate.id));
  const seen = new Set<string>();
  const items: ModelBriefItem[] = [];
  for (const entry of value.items) {
    if (!entry || typeof entry !== "object" || Array.isArray(entry)) continue;
    const item = entry as Record<string, unknown>;
    const articleId = typeof item.article_id === "string" ? item.article_id.trim() : "";
    const brief = typeof item.brief === "string" ? truncate(cleanText(item.brief), 300) : "";
    if (!candidateIds.has(articleId) || seen.has(articleId) || brief.length < 25) continue;
    seen.add(articleId);
    items.push({ articleId, summary: brief });
    if (items.length === MAX_ITEMS) break;
  }
  if (items.length < MIN_ITEMS) throw new Error("Gunluk ozet yeterli gecerli haber secmedi");
  return { summary, items };
}

function promptFor(candidates: DailyBriefCandidate[]): ChatCompletionsMessagesInput {
  const candidateLines = candidates.map((candidate, index) => [
    `${index + 1}. article_id=${candidate.id}`,
    `Baslik: ${truncate(candidate.title, 220)}`,
    `Kategori: ${candidate.category}`,
    `Haber ozeti: ${truncate(candidate.summary, 380)}`,
    `Neden onemli: ${truncate(candidate.why_important, 320)}`,
    `Yayin zamani: ${new Date(candidate.published_at).toISOString()}`
  ].join("\n")).join("\n\n");
  return {
    messages: [
      {
        role: "system",
        content: [
          "GundemAI icin gunluk gundem editorusun.",
          "Bu ozet tum kullanicilar icin ortaktir; kisisellestirme yapma.",
          "Yalniz verilen READY haber adaylarini kullan ve en kritik 3 ila 5 gelismeyi sec.",
          "Kritikligi toplumsal etki, aciliyet, genis kitleyi ilgilendirme ve somut sonuc olcutleriyle degerlendir.",
          "Ayni olayi tekrarlama; gercek, isim, sayi, tarih, neden veya sonuc uydurma.",
          "Her maddeyi sade Turkceyle tek kisa cumlede ozetle.",
          "Sadece gecerli JSON dondur: summary ve items.",
          "items dizisindeki her nesne yalniz article_id ve brief alanlarini icersin."
        ].join(" ")
      },
      {
        role: "user",
        content: `Bugunun READY haber adaylari:\n\n${candidateLines}`
      }
    ],
    max_completion_tokens: 520,
    temperature: 0.15,
    response_format: { type: "json_object" },
    chat_template_kwargs: { enable_thinking: false }
  };
}

async function candidatesFor(env: Env, now: number): Promise<DailyBriefCandidate[]> {
  const result = await env.DB.prepare(`
    SELECT id, title, summary, category, why_important, published_at
    FROM news_items
    WHERE status = 'READY' AND published_at >= ?
      AND title IS NOT NULL AND summary IS NOT NULL AND why_important IS NOT NULL
    ORDER BY published_at DESC, confidence_score DESC
    LIMIT ?
  `).bind(now - CANDIDATE_WINDOW_MS, MAX_CANDIDATES).all<DailyBriefCandidate>();
  return result.results;
}

async function claimGeneration(env: Env, dateKey: string, now: number): Promise<boolean> {
  await env.DB.prepare(`
    INSERT OR IGNORE INTO daily_briefs(date_key, status, next_attempt_at)
    VALUES (?, 'RETRY', 0)
  `).bind(dateKey).run();
  const claimed = await env.DB.prepare(`
    UPDATE daily_briefs
    SET status = 'GENERATING', generation_started_at = ?, last_error = NULL
    WHERE date_key = ?
      AND (
        (status = 'RETRY' AND next_attempt_at <= ?)
        OR (status = 'GENERATING' AND generation_started_at < ?)
      )
    RETURNING date_key
  `).bind(now, dateKey, now, now - GENERATION_TIMEOUT_MS).first<{ date_key: string }>();
  return claimed?.date_key === dateKey;
}

export async function loadDailyBrief(env: Env, now = Date.now()): Promise<DailyBrief | null> {
  const dateKey = istanbulDateKey(now);
  const row = await env.DB.prepare(`
    SELECT date_key, title, summary, items_json, generated_at
    FROM daily_briefs
    WHERE date_key = ? AND status = 'READY'
  `).bind(dateKey).first<DailyBriefRow>();
  if (!row) return null;
  try {
    const items: unknown = JSON.parse(row.items_json);
    if (!Array.isArray(items) || items.length < MIN_ITEMS) return null;
    return {
      dateKey: row.date_key,
      title: row.title,
      summary: row.summary,
      items: items as DailyBriefItem[],
      generatedAt: row.generated_at,
      shared: true
    };
  } catch {
    return null;
  }
}

export async function ensureDailyBrief(
  env: Env,
  now = Date.now()
): Promise<DailyBriefRunResult> {
  const dateKey = istanbulDateKey(now);
  const existing = await loadDailyBrief(env, now);
  if (existing) {
    return {
      generated: false,
      skipped: true,
      dateKey,
      itemCount: existing.items.length
    };
  }

  const candidates = await candidatesFor(env, now);
  if (candidates.length < MIN_CANDIDATES) {
    return { generated: false, skipped: true, dateKey, itemCount: 0 };
  }
  if (!await claimGeneration(env, dateKey, now)) {
    return { generated: false, skipped: true, dateKey, itemCount: 0 };
  }

  try {
    const selected = await runAiPrompt(
      env,
      promptFor(candidates),
      (content) => parseDailyBriefSelection(content, candidates)
    );
    const byId = new Map(candidates.map((candidate) => [candidate.id, candidate]));
    const items: DailyBriefItem[] = selected.items.flatMap((item) => {
      const candidate = byId.get(item.articleId);
      return candidate ? [{
        articleId: candidate.id,
        title: candidate.title,
        summary: item.summary,
        category: candidate.category,
        publishedAt: candidate.published_at
      }] : [];
    });
    await env.DB.prepare(`
      UPDATE daily_briefs SET
        status = 'READY',
        title = ?,
        summary = ?,
        items_json = ?,
        model = ?,
        generated_at = ?,
        generation_started_at = NULL,
        next_attempt_at = 0,
        last_error = NULL
      WHERE date_key = ?
    `).bind(
      "Bugünün Gündemi",
      selected.summary,
      JSON.stringify(items),
      AI_MODEL,
      now,
      dateKey
    ).run();
    return { generated: true, skipped: false, dateKey, itemCount: items.length };
  } catch (error) {
    const message = truncate(error instanceof Error ? error.message : String(error), 600);
    await env.DB.prepare(`
      UPDATE daily_briefs SET
        status = 'RETRY',
        generation_started_at = NULL,
        next_attempt_at = ?,
        last_error = ?
      WHERE date_key = ?
    `).bind(now + RETRY_DELAY_MS, message, dateKey).run();
    return { generated: false, skipped: false, dateKey, itemCount: 0, error: message };
  }
}
