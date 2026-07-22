import assert from "node:assert/strict";
import test from "node:test";

import type { ReadyArticle } from "../src/domain/types.js";
import { buildNewsTopicMessage, topicForCategory } from "../src/notifications/firebase-news-notifier.js";

test("maps every publishable category to a stable FCM topic", () => {
  assert.equal(topicForCategory("Yapay Zeka"), "news_yapay_zeka");
  assert.equal(topicForCategory("Turkiye"), "news_turkiye");
  assert.equal(topicForCategory("unknown"), undefined);
});

test("uses the news headline in a category topic data message", () => {
  const message = buildNewsTopicMessage(article());

  assert.equal(message?.topic, "news_teknoloji");
  assert.equal(message?.data?.title, "Yeni yapay zeka işlemcisi duyuruldu");
  assert.equal(message?.data?.article_id, "article-1");
  assert.equal(message?.android?.priority, "HIGH");
  assert.equal(message?.android?.ttl, "300s");
});

function article(): ReadyArticle {
  return {
    id: "article-1",
    status: "READY",
    readyAt: 200,
    publishedAt: 100,
    imageUrl: null,
    sourceName: "Teknoloji Kaynağı",
    sourceUrl: "https://example.com/news",
    sourceCount: 1,
    sources: [{ name: "Teknoloji Kaynağı", url: "https://example.com/news", publishedAt: 100 }],
    analysisVersion: 1,
    title: "Yeni yapay zeka işlemcisi duyuruldu",
    summary: "Yeni işlemci geliştiriciler için duyuruldu.",
    category: "Teknoloji",
    whatHappened: "Şirket yeni işlemcisini duyurdu.",
    whyImportant: "Yeni donanım yerel yapay zeka uygulamalarını hızlandırabilir.",
    missingInformation: "Satış tarihi açıklanmadı.",
    verificationStatus: "SINGLE_SOURCE_REPORT",
    confidenceScore: 80,
    possibleImpacts: [],
    unverifiedClaims: [],
    contradictions: [],
    verifiedFacts: ["Yeni işlemci duyuruldu."]
  };
}
