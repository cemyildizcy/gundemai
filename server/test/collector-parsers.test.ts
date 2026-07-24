import assert from "node:assert/strict";
import test from "node:test";

import { parseRssFeed } from "../src/collectors/rss-collector.js";
import { parseTelegramPreview } from "../src/collectors/telegram-collector.js";

test("parses RSS items and removes HTML from source content", () => {
  const xml = `<?xml version="1.0"?><rss><channel><item>
    <title>Merkez Bankasi &amp; faiz karari</title>
    <link>https://example.com/news</link>
    <description><![CDATA[<p>Politika faizi <strong>yuzde 50</strong> seviyesinde sabit tutuldu.</p>]]></description>
    <pubDate>Mon, 22 Jul 2024 10:00:00 GMT</pubDate>
    <enclosure url="https://example.com/image.jpg" type="image/jpeg" />
  </item></channel></rss>`;

  const articles = parseRssFeed(xml, {
    name: "Test RSS",
    url: "https://example.com/rss",
    category: "Finans"
  }, Date.UTC(2024, 6, 22, 12));

  assert.equal(articles.length, 1);
  assert.equal(articles[0]?.title, "Merkez Bankasi & faiz karari");
  assert.equal(articles[0]?.description, "Politika faizi yuzde 50 seviyesinde sabit tutuldu.");
  assert.equal(articles[0]?.imageUrl, "https://example.com/image.jpg");
});

test("parses public Telegram preview posts without generating fallback news", () => {
  const html = `
    <div class="tgme_widget_message_wrap">
      <div class="tgme_widget_message" data-post="kanal/42">
        <div class="tgme_widget_message_text js-message_text" dir="auto">Yeni model <b>API</b> uzerinden yayinlandi.</div>
        <a class="tgme_widget_message_date" href="https://t.me/kanal/42"><time datetime="2024-07-22T10:00:00+00:00"></time></a>
      </div>
    </div>`;

  const articles = parseTelegramPreview(html, {
    handle: "kanal",
    name: "Test Kanal",
    category: "Teknoloji"
  }, Date.UTC(2024, 6, 22, 12));

  assert.equal(articles.length, 1);
  assert.equal(articles[0]?.url, "https://t.me/kanal/42");
  assert.equal(articles[0]?.content, "Yeni model API uzerinden yayinlandi.");
});

test("drops stale and undated feed items instead of making them look new", () => {
  const xml = `<?xml version="1.0"?><rss><channel>
    <item><title>Eski haber</title><link>https://example.com/old</link>
      <pubDate>Mon, 01 Jul 2024 10:00:00 GMT</pubDate></item>
    <item><title>Tarihsiz haber</title><link>https://example.com/no-date</link></item>
  </channel></rss>`;

  const articles = parseRssFeed(xml, {
    name: "Test RSS",
    url: "https://example.com/rss",
    category: "Gundem"
  }, Date.UTC(2024, 6, 22, 12));

  assert.deepEqual(articles, []);
});
