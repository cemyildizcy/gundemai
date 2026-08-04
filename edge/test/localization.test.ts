import assert from "node:assert/strict";
import test from "node:test";

import { choosePublishedTitle } from "../src/localization";

test("an English source headline is replaced by the Turkish model headline", () => {
  assert.equal(
    choosePublishedTitle(
      "OpenAI launches a new reasoning model for developers",
      "OpenAI, geliştiriciler için yeni bir akıl yürütme modeli tanıttı",
      "OpenAI geliştiricilere yönelik yeni bir akıl yürütme modeli yayımladı."
    ),
    "OpenAI, geliştiriciler için yeni bir akıl yürütme modeli tanıttı"
  );
});

test("a legacy English headline falls back to the first Turkish analysis sentence", () => {
  assert.equal(
    choosePublishedTitle(
      "Who am I? Guess Premier League star No 6",
      "",
      "Premier Lig'in altıncı yıldızını tahmin etmeye dayalı içerik yayımlandı. Oyuncunun adı kaynakta açıklanmadı."
    ),
    "Premier Lig'in altıncı yıldızını tahmin etmeye dayalı içerik yayımlandı"
  );
});

test("the observed legacy OpenAI headline uses its Turkish analysis sentence", () => {
  assert.equal(
    choosePublishedTitle(
      "Scientific computing in the age of agentic AI",
      "",
      "Yeni bir saha raporu, bilim insanlarının yazılım geliştirme ve keşif süreçlerini hızlandırmak için yapay zekâ kodlama ajanlarını kullandığını ortaya koyuyor."
    ),
    "Yeni bir saha raporu, bilim insanlarının yazılım geliştirme ve keşif süreçlerini hızlandırmak için yapay zekâ kodlama ajanlarını kullandığını ortaya koyuyor"
  );
});

test("an existing Turkish source headline is preserved verbatim", () => {
  assert.equal(
    choosePublishedTitle(
      "Merkez Bankası faiz kararını açıkladı",
      "TCMB faiz kararını duyurdu",
      "Merkez Bankası yeni faiz kararını kamuoyuyla paylaştı."
    ),
    "Merkez Bankası faiz kararını açıkladı"
  );
});

test("an uncertain language-neutral headline is not rewritten from analysis", () => {
  assert.equal(
    choosePublishedTitle(
      "OpenAI GPT-5",
      "",
      "OpenAI yeni GPT-5 modelini tanıttı."
    ),
    "OpenAI GPT-5"
  );
});
