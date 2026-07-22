import assert from "node:assert/strict";
import test from "node:test";
import request from "supertest";

import { createHttpApp } from "../src/http/create-http-app.js";
import { InMemoryNewsStore } from "../src/storage/in-memory-news-store.js";
import type { ReadyArticle } from "../src/domain/types.js";

function ready(id: string, publishedAt: number): ReadyArticle {
  return {
    id,
    status: "READY",
    readyAt: publishedAt + 1,
    publishedAt,
    imageUrl: null,
    sourceName: "Kaynak",
    sourceUrl: `https://example.com/${id}`,
    sourceCount: 1,
    sources: [{ name: "Kaynak", url: `https://example.com/${id}`, publishedAt }],
    analysisVersion: 1,
    title: `Haber ${id}`,
    summary: "Yeterince uzun ve olaya ozel haber ozeti burada yer aliyor.",
    category: "Teknoloji",
    whatHappened: "Olaya ozel ve yeterince uzun aciklama burada yer aliyor.",
    whyImportant: "Bu gelisme urunun kullanim kosullarini dogrudan degistiriyor.",
    missingInformation: "Fiyat bilgisi kaynaklarda bulunmuyor.",
    verificationStatus: "SINGLE_SOURCE_REPORT",
    confidenceScore: 80,
    possibleImpacts: [],
    unverifiedClaims: [],
    contradictions: [],
    verifiedFacts: ["Urunun kullanim kosullari degisti."]
  };
}

test("returns the shared ready feed in deterministic newest-first order", async () => {
  const store = new InMemoryNewsStore();
  await store.saveReady(ready("old", 10));
  await store.saveReady(ready("new", 20));
  const app = createHttpApp({ store, cronSecret: "secret", supportEmail: "support@example.com", runPipeline: async () => ({ published: 0 }) });

  const response = await request(app).get("/v1/news").expect(200);

  assert.deepEqual(response.body.articles.map((article: { id: string }) => article.id), ["new", "old"]);
  assert.equal(response.body.sharedAnalysis, true);
});

test("rejects an ingestion request without the scheduler secret", async () => {
  const app = createHttpApp({
    store: new InMemoryNewsStore(),
    cronSecret: "secret",
    supportEmail: "support@example.com",
    runPipeline: async () => ({ published: 1 })
  });

  await request(app).post("/internal/ingest").expect(401);
});

test("runs ingestion once with the correct scheduler secret", async () => {
  let calls = 0;
  const app = createHttpApp({
    store: new InMemoryNewsStore(),
    cronSecret: "secret",
    supportEmail: "support@example.com",
    runPipeline: async () => { calls += 1; return { published: 3 }; }
  });

  const response = await request(app)
    .post("/internal/ingest")
    .set("Authorization", "Bearer secret")
    .expect(200);

  assert.equal(calls, 1);
  assert.equal(response.body.published, 3);
});

test("serves public privacy and account deletion pages", async () => {
  const app = createHttpApp({
    store: new InMemoryNewsStore(),
    cronSecret: "secret",
    supportEmail: "destek@gundemai.app",
    runPipeline: async () => ({ published: 0 })
  });

  const privacy = await request(app).get("/privacy").expect(200).expect("Content-Type", /html/);
  assert.match(privacy.text, /GündemAI Gizlilik Politikası/);
  assert.match(privacy.text, /\/account-deletion/);

  const deletion = await request(app).get("/account-deletion").expect(200).expect("Content-Type", /html/);
  assert.match(deletion.text, /destek@gundemai\.app/);
  assert.match(deletion.text, /hesap silme talebi/i);
});
