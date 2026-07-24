import { createHash } from "node:crypto";

import { categorizeArticle } from "./categories.js";
import type { RawArticle } from "./raw-article.js";
import type { ArticleCluster } from "./types.js";

const EVENT_WINDOW_MS = 48 * 60 * 60 * 1000;
const STOPWORDS = new Set([
  "acikladi", "aciklandi", "ardindan", "bir", "bu", "da", "de", "icin", "ile", "son",
  "sonrasi", "tarafindan", "ve", "veya", "yeni"
]);

function fold(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/ı/g, "i")
    .replace(/İ/g, "i")
    .replace(/â/g, "a")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function tokens(title: string): Set<string> {
  return new Set(fold(title).split(" ").filter((token) => token.length >= 3 && !STOPWORDS.has(token)));
}

function sameEvent(left: RawArticle, right: RawArticle): boolean {
  if (Math.abs(left.publishedAt - right.publishedAt) > EVENT_WINDOW_MS) return false;
  const leftTokens = tokens(left.title);
  const rightTokens = tokens(right.title);
  const intersection = [...leftTokens].filter((token) => rightTokens.has(token)).length;
  const union = new Set([...leftTokens, ...rightTokens]).size;
  if (intersection >= 4) return true;
  return union > 0 && intersection / union >= 0.5;
}

function stableId(items: RawArticle[]): string {
  const anchor = [...items].sort((left, right) =>
    left.publishedAt - right.publishedAt || left.url.localeCompare(right.url)
  )[0];
  if (!anchor) throw new Error("Cannot identify an empty article cluster");
  const signature = fold(anchor.url || anchor.title);
  return createHash("sha256").update(signature).digest("hex").slice(0, 24);
}

function uniqueText(values: string[]): string {
  return [...new Set(values.map((value) => value.trim()).filter(Boolean))].join("\n\n");
}

function toCluster(items: RawArticle[]): ArticleCluster {
  const sorted = [...items].sort((a, b) => {
    const sourceDifference = b.content.length - a.content.length;
    return sourceDifference !== 0 ? sourceDifference : a.publishedAt - b.publishedAt;
  });
  const primary = sorted[0];
  if (!primary) throw new Error("Cannot create an empty article cluster");

  const description = uniqueText(sorted.map((item) => item.description));
  const content = uniqueText(sorted.map((item) => item.content || item.description));
  const category = categorizeArticle({
    title: primary.title,
    description: `${description} ${content}`,
    requestedCategory: primary.categoryHint
  });

  return {
    id: stableId(sorted),
    title: primary.title,
    description,
    content,
    categoryHint: category,
    publishedAt: Math.max(...sorted.map((item) => item.publishedAt)),
    imageUrl: sorted.find((item) => item.imageUrl)?.imageUrl ?? null,
    sources: sorted
      .map((item) => ({
        name: item.sourceName,
        url: item.url,
        publishedAt: item.publishedAt,
        headline: item.title
      }))
      .sort((a, b) => a.publishedAt - b.publishedAt)
  };
}

export function clusterRawArticles(rawArticles: RawArticle[]): ArticleCluster[] {
  const deduplicated = [...new Map(rawArticles.map((article) => [article.url, article])).values()]
    .filter((article) => article.title.trim() && article.url.trim())
    .sort((a, b) => a.publishedAt - b.publishedAt || a.url.localeCompare(b.url));
  const groups: RawArticle[][] = [];

  for (const article of deduplicated) {
    const match = groups.find((group) => group.some((candidate) => sameEvent(article, candidate)));
    if (match) match.push(article);
    else groups.push([article]);
  }

  return groups.map(toCluster).sort((a, b) => b.publishedAt - a.publishedAt);
}
