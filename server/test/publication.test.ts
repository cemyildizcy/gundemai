import assert from "node:assert/strict";
import test from "node:test";

import { createReadyArticle } from "../src/domain/publication.js";
import type { AiAnalysis, ArticleCluster } from "../src/domain/types.js";

const cluster: ArticleCluster = {
  id: "cluster-2",
  title: "OpenAI yeni modelini API uzerinden erisime acti",
  description: "Model kodlama ve muhakeme gorevlerine odaklaniyor.",
  content: "OpenAI yeni modeli API uzerinden gelistiricilere sundu.",
  categoryHint: "Yapay Zeka",
  publishedAt: 1_721_650_000_000,
  imageUrl: null,
  sources: [{ name: "OpenAI", url: "https://example.com/openai", publishedAt: 1_721_650_000_000 }]
};

const analysis: AiAnalysis = {
  title: "OpenAI yeni modelini API uzerinden erisime acti",
  summary: "OpenAI kodlama ve muhakeme gorevlerine odaklanan yeni modelini gelistiricilere sundu.",
  category: "Yapay Zeka",
  whatHappened: "Yeni model OpenAI API kataloguna eklendi.",
  whyImportant: "API erisimi, gelistiricilerin modeli mevcut urunlerinde deneyebilmesini sagliyor.",
  missingInformation: "Modelin tum bolgelerdeki fiyatlandirmasi aciklanmadi.",
  verificationStatus: "SINGLE_SOURCE_REPORT",
  confidenceScore: 82,
  possibleImpacts: ["Kodlama araclarinda yeni entegrasyonlar yapilabilir."],
  unverifiedClaims: [],
  contradictions: [],
  verifiedFacts: ["Yeni model OpenAI API uzerinden gelistiricilere sunuldu."]
};

test("publishes only validated analysis as READY", () => {
  const result = createReadyArticle(cluster, analysis, 1_721_651_000_000);
  assert.equal(result.status, "READY");
  assert.equal(result.readyAt, 1_721_651_000_000);
  assert.equal(result.category, "Yapay Zeka");
});

test("refuses to publish an analysis containing generic filler", () => {
  assert.throws(
    () => createReadyArticle(cluster, {
      ...analysis,
      whyImportant: "Sektorel gelismeler ve kamuoyu bilgilendirmesi acisindan onem tasimaktadir."
    }, 1_721_651_000_000),
    /quality gate/i
  );
});
