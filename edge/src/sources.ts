import type { NewsSource } from "./types";

export const NEWS_SOURCES: NewsSource[] = [
  { kind: "rss", name: "TRT Haber Son Dakika", url: "https://www.trthaber.com/sondakika_articles.rss", category: "Son Dakika" },
  { kind: "rss", name: "TRT Haber Gundem", url: "https://www.trthaber.com/gundem_articles.rss", category: "Turkiye" },
  { kind: "rss", name: "Anadolu Ajansi Guncel", url: "https://www.aa.com.tr/tr/rss/default?cat=guncel", category: "Turkiye" },
  { kind: "rss", name: "NTV Gundem", url: "https://www.ntv.com.tr/gundem.rss", category: "Turkiye" },
  { kind: "rss", name: "BBC Turkce", url: "https://feeds.bbci.co.uk/turkce/rss.xml", category: "Dunya" },
  { kind: "rss", name: "ShiftDelete.Net", url: "https://shiftdelete.net/feed", category: "Teknoloji" },
  { kind: "rss", name: "Webtekno", url: "https://www.webtekno.com/rss.xml", category: "Teknoloji" },
  { kind: "rss", name: "DonanimHaber", url: "https://www.donanimhaber.com/rss/tum/", category: "Teknoloji" },
  { kind: "rss", name: "TechCrunch AI", url: "https://techcrunch.com/category/artificial-intelligence/feed/", category: "Yapay Zeka" },
  { kind: "rss", name: "OpenAI News", url: "https://openai.com/news/rss.xml", category: "Yapay Zeka" },
  { kind: "rss", name: "MIT Technology Review AI", url: "https://www.technologyreview.com/topic/artificial-intelligence/feed", category: "Yapay Zeka" },
  { kind: "rss", name: "The Verge AI", url: "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml", category: "Yapay Zeka" },
  { kind: "rss", name: "Google AI", url: "https://blog.google/technology/ai/rss/", category: "Yapay Zeka" },
  { kind: "rss", name: "Bloomberg HT", url: "https://www.bloomberght.com/rss", category: "Finans" },
  { kind: "rss", name: "NTV Ekonomi", url: "https://www.ntv.com.tr/ekonomi.rss", category: "Ekonomi" },
  { kind: "rss", name: "TRT Haber Ekonomi", url: "https://www.trthaber.com/ekonomi_articles.rss", category: "Ekonomi" },
  { kind: "rss", name: "Koin Bulteni", url: "https://koinbulteni.com/feed", category: "Kripto" },
  { kind: "rss", name: "TRT Haber Spor", url: "https://www.trthaber.com/spor_articles.rss", category: "Spor" },
  { kind: "rss", name: "Anadolu Ajansi Spor", url: "https://www.aa.com.tr/tr/rss/default?cat=spor", category: "Spor" },
  { kind: "rss", name: "BBC Sport", url: "https://feeds.bbci.co.uk/sport/rss.xml", category: "Spor" },
  { kind: "rss", name: "Evrim Agaci", url: "https://evrimagaci.org/rss.xml", category: "Bilim" },
  { kind: "rss", name: "Webrazzi", url: "https://webrazzi.com/feed/", category: "Girisimcilik" },
  { kind: "rss", name: "Anadolu Ajansi Kultur", url: "https://www.aa.com.tr/tr/rss/default?cat=kultur", category: "Kultur ve Sanat" },
  { kind: "rss", name: "NTV Saglik", url: "https://www.ntv.com.tr/saglik.rss", category: "Saglik" },
  { kind: "rss", name: "Anadolu Ajansi Saglik", url: "https://www.aa.com.tr/tr/rss/default?cat=saglik", category: "Saglik" },
  { kind: "rss", name: "BBC World", url: "https://feeds.bbci.co.uk/news/world/rss.xml", category: "Dunya" },
  { kind: "rss", name: "DW Turkce", url: "https://rss.dw.com/xml/rss-tur-all", category: "Dunya" },
  { kind: "telegram", name: "Pusholder (Telegram)", url: "https://t.me/s/pusholder", category: "Son Dakika" },
  { kind: "telegram", name: "BPT (Telegram)", url: "https://t.me/s/bpthaber", category: "Son Dakika" },
  { kind: "telegram", name: "Solcu Gazete (Telegram)", url: "https://t.me/s/solcugazete", category: "Son Dakika" },
  { kind: "telegram", name: "Anadolu Ajansi (Telegram)", url: "https://t.me/s/anadoluajansi", category: "Turkiye" },
  { kind: "telegram", name: "Webtekno (Telegram)", url: "https://t.me/s/webtekno", category: "Teknoloji" }
];

export const SOURCE_SHARD_COUNT = 5;

export function sourcesForShard(shard: number): NewsSource[] {
  return NEWS_SOURCES.filter((_, index) => index % SOURCE_SHARD_COUNT === shard);
}
