import assert from "node:assert/strict";
import test from "node:test";

import { categoryFor } from "../src/categories";
import { parseRss, parseTelegram } from "../src/collector";
import type { NewsSource } from "../src/types";

const NOW = Date.UTC(2026, 6, 23, 9, 0, 0);

test("RSS and Atom entries are parsed into the shared queue format", () => {
  const source: NewsSource = {
    kind: "rss",
    name: "Test",
    url: "https://example.com/feed.xml",
    category: "Teknoloji"
  };
  const rss = `<?xml version="1.0"?>
    <rss><channel><item>
      <title><![CDATA[Yeni yapay zeka modeli duyuruldu]]></title>
      <link>https://example.com/haber?utm_source=test&amp;id=1</link>
      <description><![CDATA[<p>Model bugün kullanıma açıldı.</p>]]></description>
      <pubDate>Thu, 23 Jul 2026 08:30:00 GMT</pubDate>
      <enclosure url="https://example.com/image.jpg" />
    </item></channel></rss>`;
  const result = parseRss(rss, source, NOW);
  assert.equal(result.length, 1);
  assert.equal(result[0]?.title, "Yeni yapay zeka modeli duyuruldu");
  assert.equal(result[0]?.url, "https://example.com/haber?id=1");
  assert.equal(result[0]?.description, "Model bugün kullanıma açıldı.");
  assert.equal(result[0]?.imageUrl, "https://example.com/image.jpg");
});

test("old RSS entries are ignored during the first durable collection", () => {
  const source: NewsSource = {
    kind: "rss",
    name: "Test",
    url: "https://example.com/feed.xml",
    category: "Dunya"
  };
  const rss = `<rss><channel><item>
    <title>Eski haber</title><link>https://example.com/old</link>
    <pubDate>Mon, 20 Jul 2026 08:30:00 GMT</pubDate>
  </item></channel></rss>`;
  assert.deepEqual(parseRss(rss, source, NOW), []);
});

test("Telegram preview posts retain their real post URL and timestamp", () => {
  const source: NewsSource = {
    kind: "telegram",
    name: "Test (Telegram)",
    url: "https://t.me/s/test",
    category: "Son Dakika"
  };
  const html = `<div class="tgme_widget_message_wrap">
    <div data-post="test/42">
      <div class="tgme_widget_message_text">Önemli <b>gelişme</b> açıklandı.</div>
      <time datetime="2026-07-23T08:45:00+00:00"></time>
    </div>
  </div>`;
  const result = parseTelegram(html, source, NOW);
  assert.equal(result.length, 1);
  assert.equal(result[0]?.url, "https://t.me/test/42");
  assert.equal(result[0]?.description, "Önemli gelişme açıklandı.");
});

test("deterministic category overrides a broad source hint", () => {
  assert.equal(
    categoryFor("Merkez Bankası faiz kararını açıkladı", "TCMB toplantısı tamamlandı", "Son Dakika"),
    "Finans"
  );
});
