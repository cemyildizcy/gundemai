import { XMLParser } from "fast-xml-parser";

import type { RawArticle } from "../domain/raw-article.js";
import type { NewsCollector } from "../pipeline/contracts.js";
import { cleanSourceText, fetchText, parsePublishedAt, stableRawId } from "./shared.js";

export interface RssSource {
  name: string;
  url: string;
  category: string;
}

function arrayOf<T>(value: T | T[] | null | undefined): T[] {
  if (value == null) return [];
  return Array.isArray(value) ? value : [value];
}

function stringValue(value: unknown): string {
  if (typeof value === "string" || typeof value === "number") return String(value);
  if (value && typeof value === "object") {
    const data = value as Record<string, unknown>;
    return stringValue(data["#text"] ?? data["@_href"] ?? data["@_url"] ?? "");
  }
  return "";
}

function imageFrom(item: Record<string, unknown>): string | null {
  const enclosure = item.enclosure as Record<string, unknown> | undefined;
  const media = (item["media:content"] ?? item["media:thumbnail"]) as Record<string, unknown> | undefined;
  const direct = stringValue(enclosure?.["@_url"] ?? media?.["@_url"]);
  if (direct) return direct;
  const description = stringValue(item.description ?? item.summary ?? item["content:encoded"]);
  return description.match(/<img[^>]+src=["']([^"']+)["']/i)?.[1] ?? null;
}

export function parseRssFeed(xml: string, source: RssSource): RawArticle[] {
  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    processEntities: true,
    trimValues: true
  });
  const parsed = parser.parse(xml) as Record<string, unknown>;
  const rss = parsed.rss as Record<string, unknown> | undefined;
  const channel = rss?.channel as Record<string, unknown> | undefined;
  const feed = parsed.feed as Record<string, unknown> | undefined;
  const rawItems = arrayOf((channel?.item ?? feed?.entry) as Record<string, unknown> | Record<string, unknown>[] | undefined);

  return rawItems.flatMap((item): RawArticle[] => {
    const title = cleanSourceText(stringValue(item.title));
    const linkValue = item.link;
    const link = Array.isArray(linkValue)
      ? stringValue(linkValue.find((entry) => typeof entry === "object" && (entry as Record<string, unknown>)["@_rel"] !== "self") ?? linkValue[0])
      : stringValue(linkValue ?? item.guid);
    if (!title || !link.startsWith("http")) return [];
    const descriptionRaw = stringValue(item.description ?? item.summary ?? item["content:encoded"] ?? item.content);
    const description = cleanSourceText(descriptionRaw);
    const publishedAt = parsePublishedAt(stringValue(item.pubDate ?? item.published ?? item.updated ?? item["dc:date"]));

    return [{
      id: stableRawId(link),
      title,
      description: description || title,
      content: description || title,
      categoryHint: source.category,
      imageUrl: imageFrom(item),
      url: link,
      sourceName: source.name,
      publishedAt
    }];
  });
}

export class RssCollector implements NewsCollector {
  constructor(private readonly sources: RssSource[]) {}

  async collect(): Promise<RawArticle[]> {
    const results = await Promise.allSettled(this.sources.map(async (source) =>
      parseRssFeed(await fetchText(source.url), source)
    ));
    return results.flatMap((result) => result.status === "fulfilled" ? result.value : []);
  }
}
