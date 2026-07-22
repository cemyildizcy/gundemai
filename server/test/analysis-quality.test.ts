import assert from "node:assert/strict";
import test from "node:test";

import { validateAnalysis } from "../src/domain/analysis-quality.js";
import type { AiAnalysis, ArticleCluster } from "../src/domain/types.js";

const cluster: ArticleCluster = {
  id: "cluster-1",
  title: "Merkez Bankasi politika faizini yuzde 50 seviyesinde sabit tuttu",
  description: "Karar metninde siki para politikasi durusunun surecegi belirtildi.",
  content: "Politika faizi yuzde 50 seviyesinde sabit tutuldu. Karar 22 Temmuz tarihinde aciklandi.",
  categoryHint: "Finans",
  publishedAt: 1_721_650_000_000,
  imageUrl: null,
  sources: [
    { name: "TCMB", url: "https://example.com/tcmb", publishedAt: 1_721_650_000_000 },
    { name: "AA", url: "https://example.com/aa", publishedAt: 1_721_650_060_000 }
  ]
};

const validAnalysis: AiAnalysis = {
  title: "Merkez Bankasi politika faizini yuzde 50'de sabit tuttu",
  summary: "TCMB politika faizini yuzde 50 seviyesinde degistirmedi. Karar metni siki para politikasinin devam edecegini belirtiyor.",
  category: "Finans",
  whatHappened: "Para Politikasi Kurulu politika faizini yuzde 50 seviyesinde sabit birakti.",
  whyImportant: "Karar kredi ve mevduat faizlerinin kisa vadede mevcut seviyelere yakin kalmasina yol acabilir.",
  missingInformation: "Bir sonraki faiz degisikliginin tarihi aciklanmadi.",
  verificationStatus: "MULTI_SOURCE_CONFIRMED",
  confidenceScore: 94,
  possibleImpacts: ["Kredi maliyetlerinde kisa vadede belirgin bir dusus beklenmeyebilir."],
  unverifiedClaims: [],
  contradictions: [],
  verifiedFacts: ["Politika faizi yuzde 50 seviyesinde sabit tutuldu."]
};

test("accepts a source-grounded and article-specific analysis", () => {
  const result = validateAnalysis(cluster, validAnalysis);
  assert.equal(result.ok, true);
  assert.deepEqual(result.reasons, []);
});

test("rejects the generic fallback phrases currently shown to users", () => {
  const result = validateAnalysis(cluster, {
    ...validAnalysis,
    whyImportant: "Sektorel gelismeler ve kamuoyu bilgilendirmesi acisindan onem tasimaktadir.",
    verifiedFacts: ["Haber bagimsiz kaynaklar tarafindan dogrulandi."]
  });

  assert.equal(result.ok, false);
  assert.ok(result.reasons.some((reason) => reason.includes("generic")));
});

test("rejects facts that cannot be traced to source text", () => {
  const result = validateAnalysis(cluster, {
    ...validAnalysis,
    verifiedFacts: ["Karar sonrasi dolar kuru yuzde 12 dustu."]
  });

  assert.equal(result.ok, false);
  assert.ok(result.reasons.some((reason) => reason.includes("grounded")));
});

test("rejects multi-source confirmation for a single source cluster", () => {
  const result = validateAnalysis({ ...cluster, sources: [cluster.sources[0]!] }, validAnalysis);

  assert.equal(result.ok, false);
  assert.ok(result.reasons.some((reason) => reason.includes("multi-source")));
});
