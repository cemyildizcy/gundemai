import { AI_MODEL } from "./analyzer";
import type { Env, ReadyRow, SourceRow } from "./types";

interface LegacyArticle {
  id: string;
  status: "READY";
  title: string;
  summary: string;
  category: string;
  imageUrl: string | null;
  sourceName: string;
  sourceUrl: string;
  sourceCount: number;
  sources: Array<{ name: string; url: string; publishedAt: number; headline?: string }>;
  publishedAt: number;
  readyAt: number;
  whatHappened: string;
  whyImportant: string;
  missingInformation: string;
  verificationStatus: string;
  confidenceScore: number;
  possibleImpacts: string[];
  unverifiedClaims: string[];
  contradictions: string[];
  verifiedFacts: string[];
  analysisVersion: number;
}

function parseArray(value: string): string[] {
  try {
    const parsed: unknown = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
}

async function readyRows(env: Env, category: string | null, limit: number): Promise<ReadyRow[]> {
  const categoryClause = category ? "AND category = ?" : "";
  const statement = env.DB.prepare(`
    SELECT id, title, summary, category, image_url, published_at, ready_at,
      what_happened, why_important, missing_information, verification_status,
      confidence_score, possible_impacts, unverified_claims, contradictions, verified_facts
    FROM news_items
    WHERE status = 'READY' ${categoryClause}
    ORDER BY published_at DESC, ready_at DESC
    LIMIT ?
  `);
  const result = category
    ? await statement.bind(category, limit).all<ReadyRow>()
    : await statement.bind(limit).all<ReadyRow>();
  return result.results;
}

async function sourceRows(env: Env, articleIds: string[]): Promise<SourceRow[]> {
  if (articleIds.length === 0) return [];
  const placeholders = articleIds.map(() => "?").join(",");
  const result = await env.DB.prepare(`
    SELECT article_id, name, url, headline, published_at
    FROM news_sources
    WHERE article_id IN (${placeholders})
    ORDER BY published_at ASC
  `).bind(...articleIds).all<SourceRow>();
  return result.results;
}

function toArticle(row: ReadyRow, sources: SourceRow[]): LegacyArticle {
  const articleSources = sources
    .filter((source) => source.article_id === row.id)
    .map((source) => ({
      name: source.name,
      url: source.url,
      publishedAt: source.published_at,
      headline: source.headline
    }));
  const primary = articleSources[0] ?? {
    name: "GundemAI",
    url: "https://gundemai.web.app",
    publishedAt: row.published_at,
    headline: row.title
  };
  const verificationStatus = articleSources.length >= 2 &&
    row.verification_status === "SINGLE_SOURCE_REPORT"
    ? "MULTI_SOURCE_CONFIRMED"
    : row.verification_status;
  const confidenceScore = verificationStatus === "MULTI_SOURCE_CONFIRMED"
    ? Math.max(82, row.confidence_score)
    : row.confidence_score;
  return {
    id: row.id,
    status: "READY",
    title: row.title,
    summary: row.summary,
    category: row.category,
    imageUrl: row.image_url,
    sourceName: primary.name,
    sourceUrl: primary.url,
    sourceCount: Math.max(1, articleSources.length),
    sources: articleSources.length > 0 ? articleSources : [primary],
    publishedAt: row.published_at,
    readyAt: row.ready_at,
    whatHappened: row.what_happened,
    whyImportant: row.why_important,
    missingInformation: row.missing_information,
    verificationStatus,
    confidenceScore,
    possibleImpacts: parseArray(row.possible_impacts),
    unverifiedClaims: parseArray(row.unverified_claims),
    contradictions: parseArray(row.contradictions),
    verifiedFacts: parseArray(row.verified_facts),
    analysisVersion: 3
  };
}

async function legacyArticles(env: Env): Promise<LegacyArticle[]> {
  if (!env.LEGACY_FEED_URL) return [];
  try {
    const response = await fetch(env.LEGACY_FEED_URL, {
      headers: { Accept: "application/json" },
      cf: { cacheTtl: 120, cacheEverything: true }
    });
    if (!response.ok) return [];
    const value = await response.json<{ articles?: LegacyArticle[] }>();
    return Array.isArray(value.articles) ? value.articles : [];
  } catch {
    return [];
  }
}

export async function buildFeed(env: Env, requestUrl: URL): Promise<Response> {
  const requestedLimit = Number.parseInt(requestUrl.searchParams.get("limit") ?? "100", 10);
  const limit = Number.isFinite(requestedLimit) ? Math.max(1, Math.min(requestedLimit, 100)) : 100;
  const category = requestUrl.searchParams.get("category")?.trim() || null;
  const rows = await readyRows(env, category, limit);
  const sources = await sourceRows(env, rows.map((row) => row.id));
  const current = rows.map((row) => toArticle(row, sources));
  const legacy = current.length < limit ? await legacyArticles(env) : [];
  const merged = [...current, ...legacy]
    .filter((article) => !category || article.category === category)
    .filter((article, index, articles) => articles.findIndex((candidate) =>
      candidate.id === article.id || candidate.sourceUrl === article.sourceUrl
    ) === index)
    .sort((left, right) => right.publishedAt - left.publishedAt)
    .slice(0, limit);

  return Response.json(
    { articles: merged, sharedAnalysis: true, generatedAt: Date.now() },
    {
      headers: {
        "Cache-Control": "public, max-age=60, stale-while-revalidate=300",
        "Access-Control-Allow-Origin": "*"
      }
    }
  );
}

export async function buildHealth(env: Env): Promise<Response> {
  const counts = await env.DB.prepare(`
    SELECT status, COUNT(*) AS count FROM news_items GROUP BY status
  `).all<{ status: string; count: number }>();
  const state = await env.DB.prepare(
    "SELECT * FROM pipeline_state WHERE singleton = 1"
  ).first<Record<string, unknown>>();
  return Response.json({
    ok: true,
    schedule: "*/3 * * * *",
    model: AI_MODEL,
    queue: Object.fromEntries(counts.results.map((row) => [row.status.toLowerCase(), row.count])),
    pipeline: state,
    checkedAt: Date.now()
  }, {
    headers: { "Cache-Control": "no-store", "Access-Control-Allow-Origin": "*" }
  });
}
