import assert from "node:assert/strict";
import test from "node:test";

import { categoryFor } from "../src/categories";
import {
  extractArticleImage,
  extractArticleVideo,
  isSameNewsEvent,
  parseRss,
  parseTelegram,
  sanitizeImageUrl,
  sanitizeVideoUrl
} from "../src/collector";
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
  assert.equal(result[0]?.videoUrl, null);
});

test("RSS video enclosures are preserved as video instead of broken images", () => {
  const source: NewsSource = {
    kind: "rss",
    name: "Video Test",
    url: "https://example.com/feed.xml",
    category: "Dunya"
  };
  const rss = `<rss><channel><item>
    <title>Video haber yayımlandı</title>
    <link>https://example.com/news/video</link>
    <pubDate>Thu, 23 Jul 2026 08:30:00 GMT</pubDate>
    <enclosure url="https://cdn.example.com/news/video.mp4" type="video/mp4" />
  </item></channel></rss>`;

  const result = parseRss(rss, source, NOW);

  assert.equal(result[0]?.videoUrl, "https://cdn.example.com/news/video.mp4");
  assert.equal(result[0]?.imageUrl, null);
});

test("RSS media content can provide both a video and its thumbnail", () => {
  const source: NewsSource = {
    kind: "rss",
    name: "Media Test",
    url: "https://example.com/feed.xml",
    category: "Teknoloji"
  };
  const rss = `<rss xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
    <title>Görüntülü teknoloji haberi</title>
    <link>https://example.com/news/media</link>
    <pubDate>Thu, 23 Jul 2026 08:30:00 GMT</pubDate>
    <media:thumbnail url="https://cdn.example.com/news/poster.jpg" />
    <media:content url="https://cdn.example.com/news/stream.m3u8" type="application/x-mpegURL" medium="video" />
  </item></channel></rss>`;

  const result = parseRss(rss, source, NOW);

  assert.equal(result[0]?.imageUrl, "https://cdn.example.com/news/poster.jpg");
  assert.equal(result[0]?.videoUrl, "https://cdn.example.com/news/stream.m3u8");
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
  assert.equal(result[0]?.imageUrl, null);
});

test("an RSS item without media does not reuse the feed URL as an image", () => {
  const source: NewsSource = {
    kind: "rss",
    name: "Test",
    url: "https://example.com/feed.xml",
    category: "Teknoloji"
  };
  const rss = `<rss><channel><item>
    <title>Görselsiz yeni haber</title>
    <link>https://example.com/news/without-image</link>
    <pubDate>Thu, 23 Jul 2026 08:30:00 GMT</pubDate>
  </item></channel></rss>`;

  const result = parseRss(rss, source, NOW);

  assert.equal(result.length, 1);
  assert.equal(result[0]?.imageUrl, null);
});

test("article metadata supplies an image when the feed has none", () => {
  const html = `<html><head>
    <meta property="og:image" content="/media/story-cover.jpg?size=large&amp;quality=90">
  </head></html>`;

  assert.equal(
    extractArticleImage(html, "https://example.com/news/story"),
    "https://example.com/media/story-cover.jpg?size=large&quality=90"
  );
});

test("article metadata supplies only direct playable video URLs", () => {
  const html = `<html><head>
    <meta property="og:video" content="https://cdn.example.com/story/master.m3u8">
  </head></html>`;

  assert.equal(
    extractArticleVideo(html, "https://example.com/news/story"),
    "https://cdn.example.com/story/master.m3u8"
  );
  assert.equal(sanitizeVideoUrl("https://youtube.com/watch?v=123"), null);
  assert.equal(sanitizeVideoUrl("https://cdn.example.com/story/poster.jpg"), null);
});

test("feed and Telegram listing endpoints are never accepted as images", () => {
  assert.equal(sanitizeImageUrl("https://example.com/feed"), null);
  assert.equal(sanitizeImageUrl("https://rss.example.com/latest"), null);
  assert.equal(sanitizeImageUrl("https://example.com/news.xml"), null);
  assert.equal(sanitizeImageUrl("https://t.me/s/example"), null);
  assert.equal(
    sanitizeImageUrl("https://cdn.example.com/images/story.jpg"),
    "https://cdn.example.com/images/story.jpg"
  );
});

test("deterministic category overrides a broad source hint", () => {
  assert.equal(
    categoryFor("Merkez Bankası faiz kararını açıkladı", "TCMB toplantısı tamamlandı", "Son Dakika"),
    "Finans"
  );
});

test("similar headlines from separate sources represent one event", () => {
  assert.equal(
    isSameNewsEvent(
      "Merkez Bankası politika faizini yüzde 50 seviyesinde sabit tuttu",
      "TCMB politika faizini yüzde 50'de sabit bıraktı"
    ),
    true
  );
  assert.equal(
    isSameNewsEvent(
      "Merkez Bankası politika faizini yüzde 50 seviyesinde sabit tuttu",
      "Milli takım Avrupa Şampiyonası kadrosunu açıkladı"
    ),
    false
  );
});

test("unrelated South Korea business stories stay in separate events", () => {
  assert.equal(
    isSameNewsEvent(
      "Güney Kore’nin En Büyük Ticaret Şirketi Duyurdu: Küresel Ödemeler İçin Bu Altcoin’i Seçtiler",
      "Güney Kore’nin En Büyük Bankası JPMorgan ile Bir İlke İmza Atıyor: 10 Ülke Listede"
    ),
    false
  );
});

test("numbered entries in a recurring series stay in separate events", () => {
  assert.equal(
    isSameNewsEvent(
      "Who am I? Guess Premier League star No 5",
      "Who am I? Guess Premier League star No 6"
    ),
    false
  );
});

test("daily market bulletins for different dates stay in separate events", () => {
  assert.equal(
    isSameNewsEvent(
      "Bitcoin ve Altcoinler Ne Durumda: Piyasalara Genel Bakış (25 Temmuz)",
      "Bitcoin ve Altcoinler Ne Durumda: Piyasalara Genel Bakış (26 Temmuz)"
    ),
    false
  );
});

test("unrelated first-half financial stories stay in separate events", () => {
  assert.equal(
    isSameNewsEvent(
      "TKYB 2026'nın ilk yarısında 5 milyar liranın üzerinde kar elde etti",
      "Türkiye, 2026'nın ocak-haziran döneminde 91 ülkeye 18 bin 301 ton kurutulmuş domates ihraç etti. Bu ihracattan ise toplam 60 milyon 989 bin dolar gelir elde edildi."
    ),
    false
  );
});
