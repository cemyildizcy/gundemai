import { validateAnalysis } from "../domain/analysis-quality.js";
import { categorizeArticle, isCategoryName } from "../domain/categories.js";
import type { AiAnalysis, ArticleCluster, VerificationStatus } from "../domain/types.js";
import type { NewsAnalyzer } from "../pipeline/contracts.js";

export interface AiTextProvider {
  name: string;
  generate(input: { system: string; user: string }): Promise<string>;
}

const VERIFICATION_STATUSES = new Set<VerificationStatus>([
  "OFFICIAL_CONFIRMED",
  "MULTI_SOURCE_CONFIRMED",
  "SINGLE_SOURCE_REPORT",
  "UNVERIFIED_CLAIM",
  "DEVELOPING_STORY",
  "SOURCES_CONFLICT",
  "INSUFFICIENT_INFORMATION"
]);

export function extractJsonObject(raw: string): string {
  const cleaned = raw.replace(/```json/gi, "").replace(/```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("Model response does not contain a JSON object");
  return cleaned.slice(start, end + 1);
}

function requiredString(value: unknown, field: string): string {
  if (typeof value !== "string" || !value.trim()) throw new Error(`Missing string field: ${field}`);
  return value.trim();
}

function stringArray(value: unknown, field: string): string[] {
  if (value === null || value === undefined) return [];
  const values = Array.isArray(value) ? value : [value];
  const strings = values.flatMap((item) => {
    if (typeof item === "string") return [item];
    if (!item || typeof item !== "object" || Array.isArray(item)) return [];
    return Object.values(item as Record<string, unknown>)
      .filter((nested): nested is string => typeof nested === "string");
  });
  if (strings.length === 0 && values.length > 0) {
    throw new Error(`Invalid string array field: ${field}`);
  }
  return [...new Set(strings.map((item) => item.trim()).filter(Boolean))];
}

function confidenceScore(value: unknown): number {
  const parsed = typeof value === "string"
    ? Number(value.trim().replace(/%$/, ""))
    : value;
  if (typeof parsed !== "number" || !Number.isFinite(parsed)) {
    throw new Error("Invalid confidence_score");
  }
  return Math.round(parsed > 0 && parsed <= 1 ? parsed * 100 : parsed);
}

function parseAnalysis(raw: string): AiAnalysis {
  const value: unknown = JSON.parse(extractJsonObject(raw));
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Analysis JSON must be an object");
  const data = value as Record<string, unknown>;
  const category = requiredString(data.category, "category");
  const verificationStatus = requiredString(data.verification_status, "verification_status") as VerificationStatus;

  if (!isCategoryName(category)) throw new Error(`Unsupported category: ${category}`);
  if (!VERIFICATION_STATUSES.has(verificationStatus)) throw new Error(`Unsupported verification status: ${verificationStatus}`);

  return {
    title: requiredString(data.title, "title"),
    summary: requiredString(data.summary, "summary"),
    category,
    whatHappened: requiredString(data.what_happened, "what_happened"),
    whyImportant: requiredString(data.why_important, "why_important"),
    missingInformation: requiredString(data.missing_information, "missing_information"),
    verificationStatus,
    confidenceScore: confidenceScore(data.confidence_score),
    possibleImpacts: stringArray(data.possible_impacts, "possible_impacts"),
    unverifiedClaims: stringArray(data.unverified_claims, "unverified_claims"),
    contradictions: stringArray(data.contradictions, "contradictions"),
    verifiedFacts: stringArray(data.verified_facts, "verified_facts")
  };
}

export function buildAnalysisPrompt(cluster: ArticleCluster): { system: string; user: string } {
  const system = [
    "You are the server-side editorial analysis engine for GundemAI.",
    "Return exactly one valid JSON object and no markdown.",
    "Write every user-facing field in natural Turkish.",
    "Use only facts present in the supplied source material.",
    "Never invent a number, date, name, quote, cause, impact, or official confirmation.",
    "verified_facts must contain concrete source-grounded facts, not statements about how many sources reported the story.",
    "why_important must explain a specific consequence of this exact event. Do not use generic public-interest or sector-development phrases.",
    "If a detail is unknown, place it in missing_information instead of guessing.",
    "Select one category from: Son Dakika, Yapay Zeka, Teknoloji, Turkiye, Dunya, Ekonomi, Finans, Kripto, Spor, Transfer, Bilim, Oyun, Girisimcilik, Kultur ve Sanat, Saglik.",
    "Use OFFICIAL_CONFIRMED only when an official source is included; MULTI_SOURCE_CONFIRMED only for at least two independent sources; otherwise prefer SINGLE_SOURCE_REPORT.",
    "confidence_score must be an integer from 50 to 100.",
    "possible_impacts, unverified_claims, contradictions, and verified_facts must always be JSON arrays of strings. Use [] when there are no items.",
    "Required keys: title, summary, category, what_happened, why_important, missing_information, verification_status, confidence_score, possible_impacts, unverified_claims, contradictions, verified_facts."
  ].join("\n");
  const sourceLines = cluster.sources.slice(0, 8).map((source, index) =>
    `${index + 1}. ${promptText(source.name, 120)} | ${source.url} | ${promptText(source.headline ?? "", 300)}`
  ).join("\n");
  const user = [
    `Deterministic category hint: ${cluster.categoryHint}`,
    `Title: ${promptText(cluster.title, 300)}`,
    `Description:\n${promptText(cluster.description, 2_000)}`,
    `Content:\n${promptText(cluster.content, 6_000)}`,
    `Sources:\n${sourceLines}`
  ].join("\n\n");
  return { system, user };
}

function promptText(value: string, maxLength: number): string {
  if (value.length <= maxLength) return value;
  return `${value.slice(0, Math.max(0, maxLength - 3)).trimEnd()}...`;
}

export class ServerNewsAnalyzer implements NewsAnalyzer {
  constructor(private readonly providers: AiTextProvider[]) {
    if (providers.length === 0) throw new Error("At least one AI provider is required");
  }

  async analyze(cluster: ArticleCluster): Promise<AiAnalysis> {
    const prompt = buildAnalysisPrompt(cluster);
    const deterministicCategory = categorizeArticle({
      title: cluster.title,
      description: `${cluster.description} ${cluster.content}`,
      requestedCategory: cluster.categoryHint
    });
    const failures: string[] = [];

    for (const provider of this.providers) {
      try {
        const parsed = parseAnalysis(await provider.generate(prompt));
        const analysis = { ...parsed, category: deterministicCategory };
        const quality = validateAnalysis(cluster, analysis);
        if (!quality.ok) throw new Error(quality.reasons.join("; "));
        return analysis;
      } catch (error) {
        failures.push(`${provider.name}: ${error instanceof Error ? error.message : String(error)}`);
      }
    }

    throw new Error(`No provider produced a publishable analysis. ${failures.join(" | ")}`);
  }
}
