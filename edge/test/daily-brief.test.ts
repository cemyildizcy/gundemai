import assert from "node:assert/strict";
import test from "node:test";

import {
  istanbulDateKey,
  parseDailyBriefSelection,
  type DailyBriefCandidate
} from "../src/daily-brief";

const candidates: DailyBriefCandidate[] = Array.from({ length: 5 }, (_, index) => ({
  id: `news-${index + 1}`,
  title: `Kritik haber ${index + 1}`,
  summary: `Kritik haber ${index + 1} hakkında kaynaklara dayalı ayrıntılı özet.`,
  category: index % 2 === 0 ? "Dunya" : "Ekonomi",
  why_important: `Bu gelişmenin ${index + 1}. somut etkisi geniş bir kitleyi ilgilendiriyor.`,
  published_at: Date.UTC(2026, 6, 30, 8 - index)
}));

test("daily brief date follows Europe/Istanbul rather than UTC midnight", () => {
  assert.equal(istanbulDateKey(Date.UTC(2026, 6, 29, 20, 59)), "2026-07-29");
  assert.equal(istanbulDateKey(Date.UTC(2026, 6, 29, 21, 1)), "2026-07-30");
});

test("daily brief accepts only supplied READY candidate ids and removes duplicates", () => {
  const parsed = parseDailyBriefSelection(JSON.stringify({
    summary: "Bugünün öne çıkan gelişmeleri ekonomi, teknoloji ve dünya gündemindeki somut etkileriyle özetlendi.",
    items: [
      { article_id: "news-1", brief: "İlk gelişme geniş bir kitleyi etkileyen somut sonuçları nedeniyle öne çıktı." },
      { article_id: "unknown", brief: "Bu madde aday listesinde bulunmadığı için kabul edilmemelidir." },
      { article_id: "news-1", brief: "Aynı haber ikinci kez listeye eklenmemelidir." },
      { article_id: "news-2", brief: "İkinci gelişme ekonomi gündemindeki doğrudan etkisi nedeniyle seçildi." },
      { article_id: "news-3", brief: "Üçüncü gelişme gün içindeki aciliyeti ve kapsamı nedeniyle seçildi." }
    ]
  }), candidates);

  assert.deepEqual(parsed.items.map((item) => item.articleId), ["news-1", "news-2", "news-3"]);
});

test("daily brief rejects a response with fewer than three grounded items", () => {
  assert.throws(() => parseDailyBriefSelection(JSON.stringify({
    summary: "Bugünün öne çıkan gelişmeleri doğrulanmış haber adaylarına dayanarak kısaca bir araya getirildi.",
    items: [
      { article_id: "news-1", brief: "İlk gelişme geniş bir kitleyi etkilediği için seçildi." },
      { article_id: "news-2", brief: "İkinci gelişme doğrudan etkisi nedeniyle seçildi." }
    ]
  }), candidates), /yeterli gecerli haber/);
});
