import { NEWS_SOURCES, sourcesForShard } from "./sources";
import { cleanText, fold, parseDate, sha256, truncate } from "./text";
import type { Env, NewsSource, RawNewsItem } from "./types";

const FEED_LOOKBACK_MS = 36 * 60 * 60 * 1000;
const EVENT_MATCH_WINDOW_MS = 48 * 60 * 60 * 1000;
const MAX_ITEMS_PER_SOURCE = 25;
const URL_QUERY_CHUNK = 70;
const WRITE_BATCH_SIZE = 80;
const MAX_MEDIA_FETCHES_PER_SHARD = 1;
const SOURCE_FEED_URLS = new Set(NEWS_SOURCES.map((source) => new URL(source.url).toString()));
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
  const cleaned = cleanText(rawUrl);
  if (!cleaned) return "";
  try {
    const url = new URL(cleaned, baseUrl);
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

export function sanitizeImageUrl(rawUrl: string | null | undefined): string | null {
  if (!rawUrl) return null;
  try {
    const url = new URL(rawUrl);
    if (!["http:", "https:"].includes(url.protocol)) return null;
    url.hash = "";
    const normalized = url.toString();
    const host = url.hostname.toLowerCase().replace(/^www\./, "");
    const path = url.pathname.toLowerCase().replace(/\/+$/, "");
    const isFeedEndpoint = SOURCE_FEED_URLS.has(normalized) ||
      path.endsWith("/feed") ||
      path.endsWith(".rss") ||
      path.endsWith(".xml") ||
      path.includes("/rss/") ||
      host.startsWith("rss.") ||
      (host === "t.me" && path.startsWith("/s/"));
    return isFeedEndpoint ? null : normalized;
  } catch {
    return null;
  }
}

export function sanitizeVideoUrl(rawUrl: string | null | undefined): string | null {
  if (!rawUrl) return null;
  try {
    const url = new URL(rawUrl);
    if (!['http:', 'https:'].includes(url.protocol)) return null;
    const path = url.pathname.toLowerCase();
    const isDirectVideo = ['.mp4', '.m3u8', '.webm', '.mov'].some((extension) =>
      path.endsWith(extension)
    );
    if (!isDirectVideo) return null;
    url.hash = '';
    return url.toString();
  } catch {
    return null;
  }
}

export function extractArticleImage(html: string, articleUrl: string): string | null {
  const rawImage = firstAttribute(html, [
    /<meta\b[^>]*(?:property|name)=["'](?:og:image(?::url)?|twitter:image(?::src)?)["'][^>]*content=["']([^"']+)["'][^>]*>/i,
    /<meta\b[^>]*content=["']([^"']+)["'][^>]*(?:property|name)=["'](?:og:image(?::url)?|twitter:image(?::src)?)["'][^>]*>/i,
    /<link\b[^>]*rel=["']image_src["'][^>]*href=["']([^"']+)["'][^>]*>/i
  ]);
  return sanitizeImageUrl(canonicalUrl(rawImage, articleUrl));
}

export function extractArticleVideo(html: string, articleUrl: string): string | null {
  const rawVideo = firstAttribute(html, [
    /<meta\b[^>]*(?:property|name)=["'](?:og:video(?::url)?|twitter:player:stream)["'][^>]*content=["']([^"']+)["'][^>]*>/i,
    /<meta\b[^>]*content=["']([^"']+)["'][^>]*(?:property|name)=["'](?:og:video(?::url)?|twitter:player:stream)["'][^>]*>/i,
    /<(?:video|source)\b[^>]*\bsrc=["']([^"']+)["'][^>]*>/i
  ]);
  return sanitizeVideoUrl(canonicalUrl(rawVideo, articleUrl));
}

function tagAttribute(attributes: string, name: string): string {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return cleanText(
    attributes.match(new RegExp(`\\b${escaped}=["']([^"']+)["']`, 'i'))?.[1] ?? ''
  );
}

function rssMedia(block: string, baseUrl: string): { imageUrl: string | null; videoUrl: string | null } {
  let imageUrl: string | null = null;
  let videoUrl: string | null = null;
  const elements = [...block.matchAll(/<(enclosure|media:content|media:thumbnail)\b([^>]*)>/gi)];
  for (const element of elements) {
    const tagName = (element[1] ?? '').toLowerCase();
    const attributes = element[2] ?? '';
    const rawUrl = tagAttribute(attributes, 'url');
    const absoluteUrl = canonicalUrl(rawUrl, baseUrl);
    const type = tagAttribute(attributes, 'type').toLowerCase();
    const medium = tagAttribute(attributes, 'medium').toLowerCase();
    const directVideo = sanitizeVideoUrl(absoluteUrl);
    const isVideo = medium === 'video' || type.startsWith('video/') ||
      type.includes('mpegurl') || directVideo !== null;
    if (!videoUrl && isVideo) {
      videoUrl = directVideo;
      continue;
    }
    if (!imageUrl && (tagName === 'media:thumbnail' || type.startsWith('image/') || !isVideo)) {
      imageUrl = sanitizeImageUrl(absoluteUrl);
    }
  }
  if (!imageUrl) {
    imageUrl = sanitizeImageUrl(canonicalUrl(firstAttribute(block, [
      /<img\b[^>]*\bsrc=["']([^"']+)["'][^>]*>/i
    ]), baseUrl));
  }
  return { imageUrl, videoUrl };
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
      const { imageUrl, videoUrl } = rssMedia(block, source.url);

      return [{
        title: truncate(title, 300),
        description,
        categoryHint: source.category,
        imageUrl,
        videoUrl,
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
    const imageUrl = sanitizeImageUrl(canonicalUrl(
      block.match(/background-image:\s*url\(['"]?([^'")]+)['"]?\)/i)?.[1] ?? "",
      source.url
    ));
    return [{
      title: truncate(description, 180),
      description,
      categoryHint: source.category,
      imageUrl,
      videoUrl: null,
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
  const tokens = eventTokens(item.title);
  const day = new Date(item.publishedAt).toISOString().slice(0, 10);
  const signature = tokens.length >= 4
    ? `${day}|${[...new Set(tokens)].sort().slice(0, 10).join("|")}`
    : `${day}|${item.url}`;
  const digest = await sha256(signature);
  return { id: digest.slice(0, 24), eventKey: digest };
}

function eventTokens(title: string): string[] {
  return [...new Set(
    fold(title)
      .split(" ")
      .filter((token) => token.length >= 3 && !EVENT_STOPWORDS.has(token))
  )];
}

function numberTokens(title: string): Set<string> {
  return new Set(
    fold(title)
      .split(" ")
      .flatMap((token) => token.match(/^\d+/)?.[0] ?? [])
  );
}

function numbersAreCompatible(leftTitle: string, rightTitle: string): boolean {
  const left = numberTokens(leftTitle);
  const right = numberTokens(rightTitle);
  if (left.size === 0 || right.size === 0) return true;

  const smaller = left.size <= right.size ? left : right;
  const larger = smaller === left ? right : left;
  return [...smaller].every((token) => larger.has(token));
}

export function isSameNewsEvent(leftTitle: string, rightTitle: string): boolean {
  if (!numbersAreCompatible(leftTitle, rightTitle)) return false;

  const left = new Set(eventTokens(leftTitle));
  const right = new Set(eventTokens(rightTitle));
  const intersection = [...left].filter((token) => right.has(token)).length;
  const union = new Set([...left, ...right]).size;
  const smallerSize = Math.min(left.size, right.size);
  return intersection >= 4 &&
    smallerSize > 0 &&
    intersection / smallerSize >= 0.5 &&
    intersection / union >= 0.35;
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

async function fetchArticleMedia(
  item: RawNewsItem
): Promise<{ imageUrl: string | null; videoUrl: string | null }> {
  try {
    const response = await fetch(item.url, {
      headers: {
        Accept: "text/html,application/xhtml+xml;q=0.9",
        "User-Agent": "GundemAI-NewsBot/1.0 (+https://gundemai.web.app)"
      },
      redirect: "follow",
      signal: AbortSignal.timeout(6_000)
    });
    if (!response.ok) return { imageUrl: null, videoUrl: null };
    const contentType = response.headers.get("content-type") ?? "";
    if (!contentType.toLowerCase().includes("text/html")) {
      return { imageUrl: null, videoUrl: null };
    }
    const contentLength = Number.parseInt(response.headers.get("content-length") ?? "0", 10);
    if (Number.isFinite(contentLength) && contentLength > 2_000_000) {
      return { imageUrl: null, videoUrl: null };
    }
    const html = await response.text();
    const articleUrl = response.url || item.url;
    return {
      imageUrl: extractArticleImage(html, articleUrl),
      videoUrl: extractArticleVideo(html, articleUrl)
    };
  } catch {
    return { imageUrl: null, videoUrl: null };
  }
}

async function enrichMissingMedia(
  items: RawNewsItem[],
  limit: number
): Promise<RawNewsItem[]> {
  const targets = items.filter((item) =>
    !sanitizeImageUrl(item.imageUrl) || !sanitizeVideoUrl(item.videoUrl)
  )
    .slice(0, limit);
  if (targets.length === 0) return items;
  const resolved = await Promise.all(targets.map(async (item) => [
    item.url,
    await fetchArticleMedia(item)
  ] as const));
  const media = new Map(resolved);
  return items.map((item) => ({
    ...item,
    imageUrl: sanitizeImageUrl(item.imageUrl) ?? media.get(item.url)?.imageUrl ?? null,
    videoUrl: sanitizeVideoUrl(item.videoUrl) ?? media.get(item.url)?.videoUrl ?? null
  }));
}

async function persistItems(env: Env, items: RawNewsItem[], now: number): Promise<number> {
  const unique = [...new Map(items.map((item) => [item.url, item])).values()];
  const knownUrls = await existingUrls(env.DB, unique.map((item) => item.url));
  const unseenRaw = unique.filter((item) => !knownUrls.has(item.url));
  const knownMissing = unique.filter(
    (item) => knownUrls.has(item.url) &&
      (!sanitizeImageUrl(item.imageUrl) || !sanitizeVideoUrl(item.videoUrl))
  );
  const enriched = await enrichMissingMedia(
    [...unseenRaw, ...knownMissing],
    MAX_MEDIA_FETCHES_PER_SHARD
  );
  const enrichedByUrl = new Map(enriched.map((item) => [item.url, item]));
  const unseen = unseenRaw.map((item) => enrichedByUrl.get(item.url) ?? item);
  const backfilled = knownMissing
    .map((item) => enrichedByUrl.get(item.url) ?? item)
    .filter((item) => item.imageUrl || item.videoUrl);
  const recent = await env.DB.prepare(`
    SELECT id, event_key, raw_title, published_at
    FROM news_items
    WHERE published_at >= ?
    ORDER BY published_at DESC
    LIMIT 500
  `).bind(now - EVENT_MATCH_WINDOW_MS).all<{
    id: string;
    event_key: string;
    raw_title: string;
    published_at: number;
  }>();
  const candidates = [...recent.results];
  const prepared: PreparedItem[] = [];
  for (const item of unseen.sort((left, right) => left.publishedAt - right.publishedAt)) {
    const match = candidates.find((candidate) =>
      Math.abs(candidate.published_at - item.publishedAt) <= EVENT_MATCH_WINDOW_MS &&
      isSameNewsEvent(candidate.raw_title, item.title)
    );
    const identity = match
      ? { id: match.id, eventKey: match.event_key }
      : await eventIdentity(item);
    prepared.push({ ...item, ...identity });
    candidates.push({
      id: identity.id,
      event_key: identity.eventKey,
      raw_title: item.title,
      published_at: item.publishedAt
    });
  }

  const statements: D1PreparedStatement[] = [];
  for (const item of backfilled.filter((candidate) => candidate.imageUrl)) {
    statements.push(env.DB.prepare(`
      UPDATE news_items
      SET image_url = ?
      WHERE id = (
        SELECT article_id
        FROM news_sources
        WHERE url = ?
        LIMIT 1
      )
      AND (
        image_url IS NULL OR
        TRIM(image_url) = '' OR
        LOWER(image_url) LIKE '%/feed%' OR
        LOWER(image_url) LIKE '%/rss/%' OR
        LOWER(image_url) LIKE '%.rss%' OR
        LOWER(image_url) LIKE '%.xml%' OR
        LOWER(image_url) LIKE 'https://rss.%' OR
        LOWER(image_url) LIKE 'https://t.me/s/%'
      )
    `).bind(item.imageUrl, item.url));
  }
  for (const item of backfilled.filter((candidate) => candidate.videoUrl)) {
    statements.push(env.DB.prepare(`
      UPDATE news_items
      SET video_url = COALESCE(video_url, ?)
      WHERE id = (
        SELECT article_id
        FROM news_sources
        WHERE url = ?
        LIMIT 1
      )
    `).bind(item.videoUrl, item.url));
  }
  for (const item of prepared) {
    statements.push(env.DB.prepare(`
      INSERT INTO news_items (
        id, event_key, raw_title, raw_description, category_hint, image_url, video_url,
        published_at, discovered_at, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
      ON CONFLICT(id) DO UPDATE SET
        published_at = MAX(news_items.published_at, excluded.published_at),
        image_url = COALESCE(news_items.image_url, excluded.image_url),
        video_url = COALESCE(news_items.video_url, excluded.video_url)
    `).bind(
      item.id,
      item.eventKey,
      item.title,
      item.description,
      item.categoryHint,
      item.imageUrl,
      item.videoUrl,
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
