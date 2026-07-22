import type { RawArticle } from "../domain/raw-article.js";
import type { NewsCollector } from "../pipeline/contracts.js";
import { cleanSourceText, fetchText, parsePublishedAt, stableRawId } from "./shared.js";

export interface TelegramSource {
  handle: string;
  name: string;
  category: string;
}

export function parseTelegramPreview(html: string, source: TelegramSource): RawArticle[] {
  const blocks = html.split(/class=["'][^"']*tgme_widget_message_wrap[^"']*["']/i).slice(1);
  return blocks.flatMap((block): RawArticle[] => {
    const post = block.match(/data-post=["']([^"']+)["']/i)?.[1];
    const textHtml = block.match(/class=["'][^"']*tgme_widget_message_text[^"']*["'][^>]*>([\s\S]*?)<\/div>/i)?.[1];
    if (!post || !textHtml) return [];
    const content = cleanSourceText(textHtml);
    if (!content) return [];
    const url = `https://t.me/${post}`;
    const datetime = block.match(/datetime=["']([^"']+)["']/i)?.[1];
    const imageUrl = block.match(/background-image:\s*url\(['"]?([^'")]+)['"]?\)/i)?.[1] ?? null;
    const title = content.length > 120 ? `${content.slice(0, 117).trim()}...` : content;

    return [{
      id: stableRawId(url),
      title,
      description: content,
      content,
      categoryHint: source.category,
      imageUrl,
      url,
      sourceName: `${source.name} (Telegram)`,
      publishedAt: parsePublishedAt(datetime)
    }];
  }).slice(-20);
}

export class TelegramCollector implements NewsCollector {
  constructor(private readonly sources: TelegramSource[]) {}

  async collect(): Promise<RawArticle[]> {
    const results = await Promise.allSettled(this.sources.map(async (source) => {
      const html = await fetchText(`https://t.me/s/${encodeURIComponent(source.handle)}`);
      return parseTelegramPreview(html, source);
    }));
    return results.flatMap((result) => result.status === "fulfilled" ? result.value : []);
  }
}
