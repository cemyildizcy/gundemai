import type { RawArticle } from "../domain/raw-article.js";
import type { NewsCollector } from "../pipeline/contracts.js";
import { cleanSourceText, parsePublishedAt, stableRawId } from "./shared.js";

interface ApiArticle {
  title?: string | null;
  description?: string | null;
  content?: string | null;
  url?: string | null;
  image?: string | null;
  urlToImage?: string | null;
  publishedAt?: string | null;
  source?: { name?: string | null } | null;
}

interface ApiResponse {
  articles?: ApiArticle[] | null;
}

function mapResponse(response: ApiResponse, categoryHint: string): RawArticle[] {
  return (response.articles ?? []).flatMap((article): RawArticle[] => {
    const title = cleanSourceText(article.title);
    const url = article.url?.trim() ?? "";
    if (!title || title === "[Removed]" || !url.startsWith("http")) return [];
    const description = cleanSourceText(article.description) || title;
    const content = cleanSourceText(article.content) || description;
    return [{
      id: stableRawId(url),
      title,
      description,
      content,
      categoryHint,
      imageUrl: article.image ?? article.urlToImage ?? null,
      url,
      sourceName: cleanSourceText(article.source?.name) || "Haber Kaynagi",
      publishedAt: parsePublishedAt(article.publishedAt)
    }];
  });
}

export function mapGNewsResponse(response: ApiResponse, categoryHint: string): RawArticle[] {
  return mapResponse(response, categoryHint);
}

export function mapNewsApiResponse(response: ApiResponse, categoryHint: string): RawArticle[] {
  return mapResponse(response, categoryHint);
}

async function fetchApi(url: URL, timeoutMs = 12_000): Promise<ApiResponse> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { signal: controller.signal });
    const body = await response.text();
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${body.slice(0, 200)}`);
    return JSON.parse(body) as ApiResponse;
  } finally {
    clearTimeout(timeout);
  }
}

export class GNewsCollector implements NewsCollector {
  constructor(private readonly apiKey: string) {}

  async collect(): Promise<RawArticle[]> {
    const url = new URL("https://gnews.io/api/v4/top-headlines");
    url.searchParams.set("lang", "tr");
    url.searchParams.set("country", "tr");
    url.searchParams.set("max", "10");
    url.searchParams.set("apikey", this.apiKey);
    return mapGNewsResponse(await fetchApi(url), "Son Dakika");
  }
}

export class NewsApiCollector implements NewsCollector {
  constructor(private readonly apiKey: string) {}

  async collect(): Promise<RawArticle[]> {
    const url = new URL("https://newsapi.org/v2/top-headlines");
    url.searchParams.set("country", "tr");
    url.searchParams.set("pageSize", "20");
    url.searchParams.set("apiKey", this.apiKey);
    return mapNewsApiResponse(await fetchApi(url), "Son Dakika");
  }
}
