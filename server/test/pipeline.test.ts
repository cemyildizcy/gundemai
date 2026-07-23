import assert from "node:assert/strict";
import test from "node:test";

import { NewsPipeline, prioritizeClusters } from "../src/pipeline/news-pipeline.js";
import { InMemoryNewsStore } from "../src/storage/in-memory-news-store.js";
import type { AiAnalysis } from "../src/domain/types.js";
import type { RawArticle } from "../src/domain/raw-article.js";

const raw: RawArticle = {
  id: "raw-1",
  title: "TCMB politika faizini yuzde 50 seviyesinde sabit tuttu",
  description: "Merkez Bankasi faiz kararini acikladi.",
  content: "Politika faizi yuzde 50 seviyesinde sabit tutuldu.",
  categoryHint: "Finans",
  imageUrl: null,
  url: "https://example.com/tcmb",
  sourceName: "TCMB",
  publishedAt: 1_721_650_000_000
};

const valid: AiAnalysis = {
  title: "TCMB politika faizini yuzde 50 seviyesinde sabit tuttu",
  summary: "TCMB politika faizini yuzde 50 seviyesinde degistirmedi. Karar Merkez Bankasi tarafindan duyuruldu.",
  category: "Finans",
  whatHappened: "Para Politikasi Kurulu politika faizini yuzde 50 seviyesinde sabit birakti.",
  whyImportant: "Karar kredi ve mevduat faizlerinin kisa vadeli seyrini dogrudan etkileyebilir.",
  missingInformation: "Bir sonraki faiz degisikliginin tarihi aciklanmadi.",
  verificationStatus: "OFFICIAL_CONFIRMED",
  confidenceScore: 98,
  possibleImpacts: ["Kredi maliyetleri mevcut seviyelere yakin kalabilir."],
  unverifiedClaims: [],
  contradictions: [],
  verifiedFacts: ["Politika faizi yuzde 50 seviyesinde sabit tutuldu."]
};

test("writes a valid analyzed article to the ready feed", async () => {
  const store = new InMemoryNewsStore();
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw] }],
    analyzer: { analyze: async () => valid },
    store,
    now: () => 1_721_651_000_000
  });

  const result = await pipeline.run();

  assert.equal(result.published, 1);
  assert.equal(result.publishedArticles[0]?.id, (await store.listReady())[0]?.id);
  assert.equal(result.rejected, 0);
  assert.equal((await store.listReady()).length, 1);
});

test("stores a failed analysis outside the ready feed", async () => {
  const store = new InMemoryNewsStore();
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw] }],
    analyzer: {
      analyze: async () => ({
        ...valid,
        whyImportant: "Sektorel gelismeler ve kamuoyu bilgilendirmesi acisindan onem tasimaktadir."
      })
    },
    store
  });

  const result = await pipeline.run();

  assert.equal(result.published, 0);
  assert.equal(result.rejected, 1);
  assert.match(result.rejectionReasons[0], /generic filler text/i);
  assert.equal((await store.listReady()).length, 0);
  assert.equal(store.rejections.length, 1);
});

test("does not analyze an article already published with the current analysis version", async () => {
  const store = new InMemoryNewsStore();
  let analysisCalls = 0;
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw] }],
    analyzer: { analyze: async () => { analysisCalls += 1; return valid; } },
    store
  });

  await pipeline.run();
  await pipeline.run();

  assert.equal(analysisCalls, 1);
  assert.equal((await store.listReady()).length, 1);
});

test("does not repeatedly spend AI quota on a recently rejected cluster", async () => {
  const store = new InMemoryNewsStore();
  let analysisCalls = 0;
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw] }],
    analyzer: {
      analyze: async () => {
        analysisCalls += 1;
        throw new Error("generic analysis rejected");
      }
    },
    store,
    now: () => 1_721_651_000_000
  });

  await pipeline.run();
  const second = await pipeline.run();

  assert.equal(analysisCalls, 1);
  assert.equal(second.skipped, 1);
});

test("redacts an OpenRouter key from rejection diagnostics", async () => {
  const store = new InMemoryNewsStore();
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw] }],
    analyzer: {
      analyze: async () => {
        throw new Error(`provider failed with ${"sk-or-v1-"}${"this-must-not-leak"}`);
      }
    },
    store
  });

  const result = await pipeline.run();

  assert.doesNotMatch(result.rejectionReasons[0], /this-must-not-leak/);
  assert.match(result.rejectionReasons[0], /redacted-openrouter-key/);
});

test("defers new clusters after the configured AI budget is reached", async () => {
  const store = new InMemoryNewsStore();
  const secondRaw = {
    ...raw,
    id: "raw-2",
    title: "TCMB yeni bir para politikasi raporu yayimladi",
    url: "https://example.com/tcmb-rapor"
  };
  let analysisCalls = 0;
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw, secondRaw] }],
    analyzer: { analyze: async () => { analysisCalls += 1; return valid; } },
    store,
    maxNewArticles: 1
  });

  const result = await pipeline.run();

  assert.equal(analysisCalls, 1);
  assert.equal(result.published, 1);
  assert.equal(result.deferred, 1);
});

test("continues with the next candidate after a rejected analysis", async () => {
  const store = new InMemoryNewsStore();
  const secondRaw = {
    ...raw,
    id: "raw-success",
    title: "TCMB yeni para politikasi raporunu kamuoyuna sundu",
    url: "https://example.com/tcmb-success",
    publishedAt: raw.publishedAt - 1
  };
  let analysisCalls = 0;
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw, secondRaw] }],
    analyzer: {
      analyze: async () => {
        analysisCalls += 1;
        if (analysisCalls === 1) throw new Error("first candidate rejected");
        return valid;
      }
    },
    store,
    maxNewArticles: 1,
    maxAnalysisAttempts: 2
  });

  const result = await pipeline.run();

  assert.equal(analysisCalls, 2);
  assert.equal(result.rejected, 1);
  assert.equal(result.published, 1);
});

test("interleaves regular sources and Telegram while rotating categories", () => {
  const clusters = [
    cluster("rss-finance-1", "Finans", "RSS Finance", 400),
    cluster("rss-finance-2", "Finans", "RSS Finance", 300),
    cluster("rss-tech", "Teknoloji", "RSS Tech", 200),
    cluster("telegram-breaking", "Son Dakika", "BPT (Telegram)", 500),
    cluster("telegram-sport", "Spor", "BPT (Telegram)", 100)
  ];

  const result = prioritizeClusters(clusters);

  assert.deepEqual(result.map((item) => item.id), [
    "rss-finance-1",
    "telegram-breaking",
    "rss-tech",
    "telegram-sport",
    "rss-finance-2"
  ]);
});

test("enforces the shared daily AI budget across pipeline runs", async () => {
  const store = new InMemoryNewsStore();
  const secondRaw = {
    ...raw,
    id: "raw-daily-2",
    title: "TCMB yeni bir para politikasi raporu yayimladi",
    url: "https://example.com/tcmb-gunluk-kota"
  };
  let now = Date.UTC(2026, 6, 22, 12);
  let analysisCalls = 0;
  const pipeline = new NewsPipeline({
    collectors: [{ collect: async () => [raw, secondRaw] }],
    analyzer: { analyze: async () => { analysisCalls += 1; return valid; } },
    store,
    now: () => now,
    maxNewArticlesPerDay: 1
  });

  const first = await pipeline.run();
  const second = await pipeline.run();
  now += 24 * 60 * 60 * 1000;
  const third = await pipeline.run();

  assert.equal(first.published, 1);
  assert.equal(second.published, 0);
  assert.equal(second.deferred, 1);
  assert.equal(third.published, 1);
  assert.equal(analysisCalls, 2);
});

function cluster(id: string, categoryHint: string, sourceName: string, publishedAt: number) {
  return {
    id,
    title: `${id} guncel haber basligi`,
    description: `${id} guncel haber aciklamasi`,
    content: `${id} guncel haber icerigi`,
    categoryHint,
    publishedAt,
    imageUrl: null,
    sources: [{
      name: sourceName,
      url: `https://example.com/${id}`,
      publishedAt,
      headline: `${id} guncel haber basligi`
    }]
  };
}
