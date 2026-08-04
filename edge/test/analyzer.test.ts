import assert from "node:assert/strict";
import test from "node:test";

import { parseModelAnalysis } from "../src/analyzer";

test("AI analysis requires and returns a source-grounded Turkish headline", () => {
  const analysis = parseModelAnalysis(JSON.stringify({
    title_tr: "OpenAI, geliştiriciler için yeni bir akıl yürütme modeli tanıttı",
    summary: "OpenAI, geliştiricilerin kullanımına yönelik yeni bir akıl yürütme modelini duyurdu.",
    what_happened: "Şirket, geliştiricilere yönelik yeni akıl yürütme modelini kullanıma sundu.",
    why_important: "Yeni model, geliştiricilerin karmaşık görevleri uygulamalarında çözmesine yardımcı olabilir.",
    missing_information: "Modelin tüm kullanım koşulları kaynakta belirtilmedi.",
    possible_impacts: [],
    unverified_claims: [],
    contradictions: []
  }));

  assert.equal(
    analysis.translatedTitle,
    "OpenAI, geliştiriciler için yeni bir akıl yürütme modeli tanıttı"
  );
});

test("AI analysis without a Turkish headline is rejected", () => {
  assert.throws(
    () => parseModelAnalysis(JSON.stringify({
      summary: "OpenAI, geliştiricilerin kullanımına yönelik yeni bir akıl yürütme modelini duyurdu.",
      what_happened: "Şirket, geliştiricilere yönelik yeni akıl yürütme modelini kullanıma sundu.",
      why_important: "Yeni model, geliştiricilerin karmaşık görevleri uygulamalarında çözmesine yardımcı olabilir.",
      missing_information: "Modelin tüm kullanım koşulları kaynakta belirtilmedi.",
      possible_impacts: [],
      unverified_claims: [],
      contradictions: []
    })),
    /title_tr/
  );
});
