import assert from "node:assert/strict";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

import type { ReadyArticle } from "../src/domain/types.js";
import { InMemoryNewsStore } from "../src/storage/in-memory-news-store.js";
import { writeStaticSite } from "../src/static/write-static-site.js";

test("writes a shared feed and public legal pages", async () => {
  const directory = await mkdtemp(join(tmpdir(), "gundemai-hosting-"));
  try {
    const store = new InMemoryNewsStore();
    await store.saveReady(readyArticle());

    const result = await writeStaticSite({
      store,
      outputDirectory: directory,
      supportEmail: "destek@gundemai.app",
      now: () => 1234
    });

    const feed = JSON.parse(await readFile(join(directory, "v1", "news.json"), "utf8"));
    const privacy = await readFile(join(directory, "privacy", "index.html"), "utf8");
    const deletion = await readFile(join(directory, "account-deletion", "index.html"), "utf8");

    assert.equal(result.articleCount, 1);
    assert.equal(feed.generatedAt, 1234);
    assert.equal(feed.sharedAnalysis, true);
    assert.equal(feed.articles[0].id, "article-1");
    assert.match(privacy, /GündemAI Gizlilik Politikası/);
    assert.match(deletion, /destek@gundemai\.app/);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

function readyArticle(): ReadyArticle {
  return {
    id: "article-1",
    status: "READY",
    readyAt: 101,
    publishedAt: 100,
    imageUrl: null,
    sourceName: "Kaynak",
    sourceUrl: "https://example.com/article-1",
    sourceCount: 1,
    sources: [{ name: "Kaynak", url: "https://example.com/article-1", publishedAt: 100 }],
    analysisVersion: 1,
    title: "Test haberi",
    summary: "Test haberi için yeterince açıklayıcı ortak özet.",
    category: "Teknoloji",
    whatHappened: "Haberde belirli bir teknoloji gelişmesi duyuruldu.",
    whyImportant: "Gelişme ürün kullanıcılarını doğrudan etkileyebilir.",
    missingInformation: "Uygulama tarihi henüz açıklanmadı.",
    verificationStatus: "SINGLE_SOURCE_REPORT",
    confidenceScore: 80,
    possibleImpacts: [],
    unverifiedClaims: [],
    contradictions: [],
    verifiedFacts: ["Gelişme kaynak tarafından duyuruldu."]
  };
}
