import assert from "node:assert/strict";
import test from "node:test";

import { mapGNewsResponse, mapNewsApiResponse } from "../src/collectors/api-collectors.js";

test("maps GNews response without trusting the requested category", () => {
  const articles = mapGNewsResponse({
    articles: [{
      title: "OpenAI yeni modelini duyurdu",
      description: "Model API uzerinden erisime acildi.",
      content: "Yeni model gelistiricilere sunuldu.",
      url: "https://example.com/gnews",
      image: "https://example.com/image.jpg",
      publishedAt: "2024-07-22T10:00:00Z",
      source: { name: "GNews Source" }
    }]
  }, "Son Dakika");

  assert.equal(articles.length, 1);
  assert.equal(articles[0]?.categoryHint, "Son Dakika");
  assert.equal(articles[0]?.sourceName, "GNews Source");
});

test("drops NewsAPI entries without a usable title or URL", () => {
  const articles = mapNewsApiResponse({
    articles: [
      { title: "[Removed]", url: "https://example.com/removed", source: { name: "Removed" } },
      { title: "Gecerli haber", url: null, source: { name: "No URL" } },
      { title: "Gecerli haber", url: "https://example.com/valid", description: "Aciklama", source: { name: "Valid" } }
    ]
  }, "Teknoloji");

  assert.equal(articles.length, 1);
  assert.equal(articles[0]?.url, "https://example.com/valid");
});
