import assert from "node:assert/strict";
import test from "node:test";

import { categorizeArticle, normalizeCategory } from "../src/domain/categories.js";

test("normalizes Turkish display names and API aliases to one category", () => {
  assert.equal(normalizeCategory("YAPAY_ZEKA"), "Yapay Zeka");
  assert.equal(normalizeCategory("Yapay Zeka"), "Yapay Zeka");
  assert.equal(normalizeCategory("Kultur ve Sanat"), "Kultur ve Sanat");
  assert.equal(normalizeCategory("SAGLIK"), "Saglik");
  assert.equal(normalizeCategory("GİRİŞİMCİLİK"), "Girisimcilik");
});

test("categorizes natural Turkish text containing dotted and dotless i", () => {
  const result = categorizeArticle({
    title: "Faiz kararı sonrası piyasalarda hareketlilik",
    description: "Merkez Bankası politika faizini sabit tuttu.",
    requestedCategory: "Ekonomi"
  });

  assert.equal(result, "Finans");
});

test("categorizes from article content before using the requested feed category", () => {
  const result = categorizeArticle({
    title: "OpenAI yeni muhakeme modelini duyurdu",
    description: "Yeni yapay zeka modeli geliştiricilere API üzerinden sunuldu.",
    requestedCategory: "Son Dakika"
  });

  assert.equal(result, "Yapay Zeka");
});

test("does not classify the substring ai inside unrelated Turkish words", () => {
  const result = categorizeArticle({
    title: "Faiz karari sonrasi piyasalarda hareketlilik",
    description: "Merkez Bankasi politika faizini sabit tuttu.",
    requestedCategory: "Ekonomi"
  });

  assert.equal(result, "Finans");
});
