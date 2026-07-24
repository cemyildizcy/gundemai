import assert from "node:assert/strict";
import test from "node:test";

import { clusterRawArticles } from "../src/domain/clustering.js";
import type { RawArticle } from "../src/domain/raw-article.js";

const baseTime = 1_721_650_000_000;

function article(overrides: Partial<RawArticle>): RawArticle {
  return {
    id: "a",
    title: "TCMB politika faizini yuzde 50 seviyesinde sabit tuttu",
    description: "Merkez Bankasi faiz kararini acikladi",
    content: "Politika faizi yuzde 50 seviyesinde sabit tutuldu.",
    categoryHint: "Finans",
    imageUrl: null,
    url: "https://example.com/a",
    sourceName: "Kaynak A",
    publishedAt: baseTime,
    ...overrides
  };
}

test("merges reports about the same event into one source cluster", () => {
  const clusters = clusterRawArticles([
    article({}),
    article({
      id: "b",
      title: "Merkez Bankasi politika faizini yuzde 50'de sabit birakti",
      sourceName: "Kaynak B",
      url: "https://example.com/b",
      publishedAt: baseTime + 60_000
    })
  ]);

  assert.equal(clusters.length, 1);
  assert.equal(clusters[0]?.sources.length, 2);
});

test("keeps unrelated stories in separate clusters", () => {
  const clusters = clusterRawArticles([
    article({}),
    article({
      id: "c",
      title: "Milli takim Avrupa Sampiyonasi kadrosunu acikladi",
      description: "Teknik direktor turnuva kadrosunu duyurdu",
      content: "Turnuva kadrosunda 26 futbolcu bulunuyor.",
      categoryHint: "Spor",
      sourceName: "Spor Kaynagi",
      url: "https://example.com/c"
    })
  ]);

  assert.equal(clusters.length, 2);
});

test("uses stable cluster ids regardless of source order", () => {
  const first = article({});
  const second = article({ id: "b", sourceName: "Kaynak B", url: "https://example.com/b" });
  const id1 = clusterRawArticles([first, second])[0]?.id;
  const id2 = clusterRawArticles([second, first])[0]?.id;
  assert.equal(id1, id2);
});

test("keeps the cluster id when a later source joins the same event", () => {
  const first = article({});
  const second = article({
    id: "b",
    sourceName: "Kaynak B",
    url: "https://example.com/b",
    publishedAt: baseTime + 60_000
  });

  const initialId = clusterRawArticles([first])[0]?.id;
  const enrichedId = clusterRawArticles([first, second])[0]?.id;

  assert.equal(enrichedId, initialId);
});
