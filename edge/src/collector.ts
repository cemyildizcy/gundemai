import { sourcesForShard } from "./sources";
import { cleanText, fold, parseDate, sha256, truncate } from "./text";
import type { Env, NewsSource, RawNewsItem } from "./types";

const FEED_LOOKBACK_MS = 36 * 60 * 60 * 1000;
const MAX_ITEMS_PER_SOURCE = 25;
const URL_QUERY_CHUNK = 70;
const WRITE_BATCH_SIZE = 80;
const EVENT_STOPWORDS = new Set([
  "acikladi", "aciklandi", "ardindan", "bir", "bu", "da", "de", "icin", "ile",
  "olarak", "olan", "son", "sonra", "tarafindan", "ve", "veya", "yeni"
]);

interface PreparedItem extends RawNewsItem {
  id: string;
  eventKey: string;
}

export interface CollectionResult {
  shard: number;
  sourceCount: number;
  healthySources: number;
  fetchedItems: number;
  insertedItems: number;
  errors: string[];
}

function firstTag(block: string, names: string[]): string {
  for (const name of names) {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const match = block.match(new RegExp(`<${escaped}(?:\\s[^>]*)?>([\\s\\S]*?)<\\/${escaped}>`, "i"));
    if (match?.[1]) return match[1];
  }
  return "";
}

function firstAttribute(block: string, expressions: RegExp[]): string {
  for (const expression of expressions) {
    const value = block.match(expression)?.[1];
    if (value) return cleanText(value);
  }
  return "";
}

function canonicalUrl(rawUrl: string, baseUrl: string): string {
  try {
    const url = new URL(cleanText(rawUrl), baseUrl);
    const parameterNames: string[] = [];
    url.searchParams.forEach((_, key) => parameterNames.push(key));
    for (const key of parameterNames) {
      if (/^(utm_|fbclid$|gclid$)/i.test(key)) url.searchParams.delete(key);
    }
    url.hash = "";
    return url.toString();
  } catch {
    return "";
  }
}

export function parseRss(xml: string, source: NewsSource, now = Date.now()): RawNewsItem[] {
  const itemBlocks = [...xml.matchAll(/<item\b[^>]*>([\s\S]*?)<\/item>/gi)].map((match) => match[1] ?? "");
  const entryBlocks = [...xml.matchAll(/<entry\b[^>]*>([\s\S]*?)<\/entry>/gi)].map((match) => match[1] ?? "");

  return [...itemBlocks, ...entryBlocks]
    .flatMap((block): RawNewsItem[] => {
      const title = cleanText(firstTag(block, ["title"]));
      const rawLink = firstTag(block, ["link", "guid"]) || firstAttribute(block, [
        /<link\b[^>]*\bhref=["']([^"']+)["'][^>]*>/i
      ]);
      const url = canonicalUrl(rawLink, source.url);
      if (!title || !url.startsWith("http")) return [];

      const descriptionHtml = firstTag(block, ["description", "summary", "content:encoded", "content"]);
      const description = truncate(cleanText(descriptionHtml) || title, 4_000);
      const dateText = cleanText(firstTag(block, ["pubDate", "published", "updated", "dc:date"]));
      const publishedAt = parseDate(dateText, now);
      const imageUrl = canonicalUrl(firstAttribute(block, [
        /<enclosure\b[^>]*\burl=["']([^"']+)["'][^>]*>/i,
        /<media:(?:content|thumbnail)\b[^>]*\burl=["']([^"']+)["'][^>]*>/i,
        /<img\b[^>]*\bsrc=["']([^"']+)["'][^>]*>/i
      ]), source.url) || null;

      return [{
        title: truncate(title, 300),
        description,
        categoryHint: source.category,
        imageUrl,
        url,
        sourceName: source.name,
        publishedAt
      }];
    })
    .filter((item) => item.publishedAt >= now - FEED_LOOKBACK_MS && item.publishedAt <= now + 60 * 60 * 1000)
    .sort((left, right) => right.publishedAt - left.publishedAt)
    .slice(0, MAX_ITEMS_PER_SOURCE);
}

export function parseTelegram(html: string, source: NewsSource, now = Date.now()): RawNewsItem[] {
  const blocks = html.split(/class=["'][^"']*tgme_widget_message_wrap[^"']*["']/i).slice(1);
  return blocks.flatMap((block): RawNewsItem[] => {
    const post = block.match(/data-post=["']([^"']+)["']/i)?.[1];
    const textHtml = block.match(
      /class=["'][^"']*tgme_widget_message_text[^"']*["'][^>]*>([\s\S]*?)<\/div>/i
    )?.[1];
    if (!post || !textHtml) return [];
    const description = truncate(cleanText(textHtml), 4_000);
    if (!description) return [];
    const publishedAt = parseDate(block.match(/datetime=["']([^"']+)["']/i)?.[1] ?? "", now);
    const imageUrl = canonicalUrl(
      block.match(/background-image:\s*url\(['"]?([^'")]+)['"]?\)/i)?.[1] ?? "",
      source.url
    ) || null;
    return [{
      title: truncate(description, 180),
      description,
      categoryHint: source.category,
      imageUrl,
      url: `https://t.me/${post}`,
      sourceName: source.name,
      publishedAt
    }];
  })
    .filter((item) => item.publishedAt >= now - FEED_LOOKBACK_MS && item.publishedAt <= now + 60 * 60 * 1000)
    .sort((left, right) => right.publishedAt - left.publishedAt)
    .slice(0, MAX_ITEMS_PER_SOURCE);
}

async function fetchSource(source: NewsSource, now: number): Promise<RawNewsItem[]> {
  const response = await fetch(source.url, {
    headers: {
      Accept: source.kind === "rss"
        ? "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.5"
        : "text/html,application/xhtml+xml",
      "User-Agent": "GundemAI-NewsBot/1.0 (+https://gundemai.web.app)"
    },
    redirect: "follow",
    signal: AbortSignal.timeout(15_000)
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const body = await response.text();
  return source.kind === "rss" ? parseRss(body, source, now) : parseTelegram(body, source, now);
}

async function eventIdentity(item: RawNewsItem): Promise<{ id: string; eventKey: string }> {
  const tokens = fold(item.title)
    .split(" ")
    .filter((token) => token.length >= 3 && !EVENT_STOPWORDS.has(token));
  const day = new Date(item.publishedAt).toISOString().slice(0, 10);
  const signature = tokens.length >= 4
    ? `${day}|${[...new Set(tokens)].sort().slice(0, 10).join("|")}`
    : `${day}|${item.url}`;
  const digest = await sha256(signature);
  return { id: digest.slice(0, 24), eventKey: digest };
}

function chunks<T>(values: T[], size: number): T[][] {
  const result: T[][] = [];
  for (let index = 0; index < values.length; index += size) result.push(values.slice(index, index + size));
  return result;
}

async function existingUrls(db: D1Database, urls: string[]): Promise<Set<string>> {
  const existing = new Set<string>();
  for (const group of chunks(urls, URL_QUERY_CHUNK)) {
    const placeholders = group.map(() => "?").join(",");
    const result = await db.prepare(
      `SELECT url FROM news_sources WHERE url IN (${placeholders})`
    ).bind(...group).all<{ url: string }>();
    for (const row of result.results) existing.add(row.url);
  }
  return existing;
}

async function persistItems(env: Env, items: RawNewsItem[], now: number): Promise<number> {
  const unique = [...new Map(items.map((item) => [item.url, item])).values()];
  const knownUrls = await existingUrls(env.DB, unique.map((item) => item.url));
  const unseen = unique.filter((item) => !knownUrls.has(item.url));
  const prepared: PreparedItem[] = await Promise.all(unseen.map(async (item) => ({
    ...item,
    ...await eventIdentity(item)
  })));

  const statements: D1PreparedStatement[] = [];
  for (const item of prepared) {
    statements.push(env.DB.prepare(`
      INSERT INTO news_items (
        id, event_key, raw_title, raw_description, category_hint, image_url,
        published_at, discovered_at, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
      ON CONFLICT(id) DO UPDATE SET
        published_at = MAX(news_items.published_at, excluded.published_at),
        image_url = COALESCE(news_items.image_url, excluded.image_url)
    `).bind(
      item.id,
      item.eventKey,
      item.title,
      item.description,
      item.categoryHint,
      item.imageUrl,
      item.publishedAt,
      now
    ));
    statements.push(env.DB.prepare(`
      INSERT OR IGNORE INTO news_sources(article_id, url, name, headline, published_at)
      VALUES (?, ?, ?, ?, ?)
    `).bind(item.id, item.url, item.sourceName, item.title, item.publishedAt));
  }

  for (const group of chunks(statements, WRITE_BATCH_SIZE)) await env.DB.batch(group);
  return prepared.length;
}

export async function collectShard(env: Env, shard: number, now = Date.now()): Promise<CollectionResult> {
  const sources = sourcesForShard(shard);
  const settled = await Promise.allSettled(sources.map((source) => fetchSource(source, now)));
  const items: RawNewsItem[] = [];
  const errors: string[] = [];
  settled.forEach((result, index) => {
    if (result.status === "fulfilled") items.push(...result.value);
    else errors.push(`${sources[index]?.name ?? `source-${index}`}: ${String(result.reason).slice(0, 160)}`);
  });
  const insertedItems = await persistItems(env, items, now);
  return {
    shard,
    sourceCount: sources.length,
    healthySources: settled.length - errors.length,
    fetchedItems: items.length,
    insertedItems,
    errors
  };
}
