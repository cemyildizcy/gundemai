import assert from "node:assert/strict";
import test from "node:test";

import { ServerNewsAnalyzer, extractJsonObject } from "../src/ai/server-news-analyzer.js";
import type { ArticleCluster } from "../src/domain/types.js";

const cluster: ArticleCluster = {
  id: "ai-1",
  title: "OpenAI yeni muhakeme modelini API uzerinden yayinladi",
  description: "Model kodlama ve uzun baglam gorevlerine odaklaniyor.",
  content: "OpenAI yeni muhakeme modelini gelistiriciler icin API uzerinden erisime acti.",
  categoryHint: "Yapay Zeka",
  publishedAt: 1_721_650_000_000,
  imageUrl: null,
  sources: [{
    name: "OpenAI",
    url: "https://example.com/openai",
    publishedAt: 1_721_650_000_000,
    headline: "OpenAI yeni muhakeme modelini yayinladi"
  }]
};

const validJson = JSON.stringify({
  title: "OpenAI yeni muhakeme modelini API uzerinden yayinladi",
  summary: "OpenAI kodlama ve uzun baglam gorevlerine odaklanan yeni modelini API uzerinden erisime acti.",
  category: "Son Dakika",
  what_happened: "Yeni muhakeme modeli gelistiricilerin kullanimina API uzerinden sunuldu.",
  why_important: "API erisimi gelistiricilerin modeli mevcut kodlama urunlerinde deneyebilmesini sagliyor.",
  missing_information: "Modelin tum bolgelerdeki fiyatlandirmasi kaynaklarda yer almiyor.",
  verification_status: "SINGLE_SOURCE_REPORT",
  confidence_score: 84,
  possible_impacts: ["Kodlama araclarinda yeni model entegrasyonlari yapilabilir."],
  unverified_claims: [],
  contradictions: [],
  verified_facts: ["OpenAI yeni modeli API uzerinden gelistiricilere sundu."]
});

test("extracts one JSON object from fenced model output", () => {
  assert.equal(extractJsonObject(`\`\`\`json\n${validJson}\n\`\`\``), validJson);
});

test("uses article content classification instead of a generic model category", async () => {
  const analyzer = new ServerNewsAnalyzer([
    { name: "test", generate: async () => validJson }
  ]);

  const result = await analyzer.analyze(cluster);

  assert.equal(result.category, "Yapay Zeka");
});

test("uses the collector category hint when text has no category signal", async () => {
  const neutralCluster: ArticleCluster = {
    ...cluster,
    id: "neutral-1",
    title: "Yeni karar kurul toplantısının ardından açıklandı",
    description: "Kararın ayrıntıları kurumun internet sitesinde yayımlandı.",
    content: "Kurul toplantısının ardından yeni karar metni kamuoyuyla paylaşıldı.",
    categoryHint: "Saglik",
    sources: [{
      name: "Kurum",
      url: "https://example.com/karar",
      publishedAt: cluster.publishedAt,
      headline: "Yeni karar kurul toplantısının ardından açıklandı"
    }]
  };
  const response = JSON.stringify({
    ...JSON.parse(validJson),
    title: neutralCluster.title,
    summary: "Kurul toplantısının ardından alınan yeni karar kurumun internet sitesinde kamuoyuyla paylaşıldı.",
    category: "Finans",
    what_happened: "Kurum kurul toplantısından sonra hazırlanan karar metnini internet sitesinde yayımladı.",
    why_important: "Karar metninin yayımlanması ilgili uygulamanın bundan sonraki adımlarını doğrudan belirliyor.",
    verified_facts: ["Yeni karar kurul toplantısının ardından açıklandı."]
  });
  const analyzer = new ServerNewsAnalyzer([{ name: "test", generate: async () => response }]);

  const result = await analyzer.analyze(neutralCluster);

  assert.equal(result.category, "Saglik");
});

test("falls back to the next provider when the first response is generic", async () => {
  const calls: string[] = [];
  const analyzer = new ServerNewsAnalyzer([
    {
      name: "generic",
      generate: async () => {
        calls.push("generic");
        return JSON.stringify({
          ...JSON.parse(validJson),
          why_important: "Sektorel gelismeler ve kamuoyu bilgilendirmesi acisindan onem tasimaktadir."
        });
      }
    },
    { name: "valid", generate: async () => { calls.push("valid"); return validJson; } }
  ]);

  const result = await analyzer.analyze(cluster);

  assert.deepEqual(calls, ["generic", "valid"]);
  assert.match(result.whyImportant, /API erisimi/);
});

test("fails closed when every provider returns an invalid analysis", async () => {
  const analyzer = new ServerNewsAnalyzer([
    { name: "invalid", generate: async () => "not-json" }
  ]);

  await assert.rejects(() => analyzer.analyze(cluster), /no provider produced a publishable analysis/i);
});
