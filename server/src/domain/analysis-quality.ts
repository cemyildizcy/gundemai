import type { AiAnalysis, ArticleCluster } from "./types.js";
import { isCategoryName } from "./categories.js";

export interface QualityResult {
  ok: boolean;
  reasons: string[];
}

const GENERIC_PHRASES = [
  "sektorel gelismeler ve kamuoyu bilgilendirmesi acisindan onem tasimaktadir",
  "ilgili sektorde ve kamuoyunda genis yanki bulmasi beklenmektedir",
  "gundemdeki gelismeleri anlama ve kamuoyunu dogru bilgilendirme acisindan",
  "haber bagimsiz kaynaklar tarafindan dogrulandi",
  "haber akisina eklendi",
  "detaylar ve resmi kurum beyanlari anlik olarak takip ediliyor"
];

const STOPWORDS = new Set([
  "aciklandi", "ardindan", "bir", "bu", "da", "de", "icin", "ile", "olarak", "olan",
  "sonra", "tarafindan", "ve", "veya", "yeni", "karar", "metninde", "seviyesinde"
]);

function fold(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/ı/g, "i")
    .replace(/İ/g, "i")
    .replace(/â/g, "a")
    .toLowerCase()
    .replace(/[^a-z0-9%]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function substantiveTokens(value: string): Set<string> {
  return new Set(
    fold(value)
      .split(" ")
      .filter((token) => token.length >= 4 && !STOPWORDS.has(token))
  );
}

function isGrounded(fact: string, sourceText: string): boolean {
  const factNumbers = fold(fact).match(/\b\d+(?:[.,]\d+)?\b/g) ?? [];
  const sourceFolded = fold(sourceText);
  if (factNumbers.some((number) => !sourceFolded.includes(number))) return false;

  const factTokens = substantiveTokens(fact);
  if (factTokens.size === 0) return false;
  const sourceTokens = substantiveTokens(sourceText);
  const overlap = [...factTokens].filter((token) => sourceTokens.has(token)).length;
  return overlap / factTokens.size >= 0.4;
}

export function validateAnalysis(cluster: ArticleCluster, analysis: AiAnalysis): QualityResult {
  const reasons: string[] = [];
  const combinedAnalysis = fold([
    analysis.summary,
    analysis.whatHappened,
    analysis.whyImportant,
    ...analysis.verifiedFacts
  ].join(" "));

  if (GENERIC_PHRASES.some((phrase) => combinedAnalysis.includes(phrase))) {
    reasons.push("generic filler text is not publishable");
  }
  if (!isCategoryName(analysis.category)) reasons.push("category is not allowed");
  if (analysis.summary.trim().length < 40) reasons.push("summary is too short");
  if (analysis.whatHappened.trim().length < 30) reasons.push("whatHappened is too short");
  if (analysis.whyImportant.trim().length < 35) reasons.push("whyImportant is too short");
  if (analysis.verifiedFacts.length === 0) reasons.push("at least one verified fact is required");
  if (analysis.confidenceScore < 50 || analysis.confidenceScore > 100) reasons.push("confidence score is outside 50..100");

  const uniqueSourceIdentities = new Set(cluster.sources.map((source) => {
    let host: string;
    try {
      host = new URL(source.url).hostname.replace(/^www\./, "");
    } catch {
      host = source.url;
    }
    return `${host}|${fold(source.name)}`;
  }));
  if (analysis.verificationStatus === "MULTI_SOURCE_CONFIRMED" && uniqueSourceIdentities.size < 2) {
    reasons.push("multi-source confirmation requires at least two distinct source hosts");
  }
  if (analysis.verificationStatus === "SOURCES_CONFLICT" && analysis.contradictions.length === 0) {
    reasons.push("source conflict status requires a concrete contradiction");
  }
  if (analysis.verificationStatus === "UNVERIFIED_CLAIM" && analysis.unverifiedClaims.length === 0) {
    reasons.push("unverified claim status requires a concrete claim");
  }

  const sourceText = [
    cluster.title,
    cluster.description,
    cluster.content,
    ...cluster.sources.map((source) => source.headline ?? "")
  ].join(" ");
  if (analysis.verifiedFacts.some((fact) => !isGrounded(fact, sourceText))) {
    reasons.push("one or more verified facts are not grounded in source text");
  }

  return { ok: reasons.length === 0, reasons };
}
