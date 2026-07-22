import { mkdir, rename, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";

import { accountDeletionPage, privacyPage } from "../http/legal-pages.js";
import type { NewsStore } from "../pipeline/contracts.js";

export async function writeStaticSite(options: {
  store: NewsStore;
  outputDirectory: string;
  supportEmail: string;
  now?: () => number;
}): Promise<{ articleCount: number; generatedAt: number }> {
  const generatedAt = (options.now ?? Date.now)();
  const articles = await options.store.listReady({ limit: 80 });
  const feed = { articles, sharedAnalysis: true, generatedAt };

  await Promise.all([
    writeAtomic(
      join(options.outputDirectory, "v1", "news.json"),
      JSON.stringify(feed, null, 2) + "\n"
    ),
    writeAtomic(
      join(options.outputDirectory, "privacy", "index.html"),
      privacyPage(options.supportEmail)
    ),
    writeAtomic(
      join(options.outputDirectory, "account-deletion", "index.html"),
      accountDeletionPage(options.supportEmail)
    ),
    writeAtomic(
      join(options.outputDirectory, "health.json"),
      JSON.stringify({ ok: true, generatedAt }) + "\n"
    )
  ]);

  return { articleCount: articles.length, generatedAt };
}

async function writeAtomic(path: string, content: string): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
  const temporaryPath = `${path}.tmp`;
  await writeFile(temporaryPath, content, "utf8");
  await rename(temporaryPath, path);
}
