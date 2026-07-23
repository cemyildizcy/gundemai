import type { RssSource } from "../collectors/rss-collector.js";
import type { TelegramSource } from "../collectors/telegram-collector.js";

export const RSS_SOURCES: RssSource[] = [
  { name: "TRT Haber Son Dakika", url: "https://www.trthaber.com/sondakika_articles.rss", category: "Son Dakika" },
  { name: "TRT Haber Gundem", url: "https://www.trthaber.com/gundem_articles.rss", category: "Turkiye" },
  { name: "Anadolu Ajansi Guncel", url: "https://www.aa.com.tr/tr/rss/default?cat=guncel", category: "Turkiye" },
  { name: "NTV Gundem", url: "https://www.ntv.com.tr/gundem.rss", category: "Turkiye" },
  { name: "BBC Turkce", url: "https://feeds.bbci.co.uk/turkce/rss.xml", category: "Dunya" },
  { name: "ShiftDelete.Net", url: "https://shiftdelete.net/feed", category: "Teknoloji" },
  { name: "Webtekno", url: "https://www.webtekno.com/rss.xml", category: "Teknoloji" },
  { name: "DonanimHaber", url: "https://www.donanimhaber.com/rss/tum/", category: "Teknoloji" },
  { name: "TechCrunch AI", url: "https://techcrunch.com/category/artificial-intelligence/feed/", category: "Yapay Zeka" },
  { name: "OpenAI News", url: "https://openai.com/news/rss.xml", category: "Yapay Zeka" },
  { name: "MIT Technology Review AI", url: "https://www.technologyreview.com/topic/artificial-intelligence/feed", category: "Yapay Zeka" },
  { name: "The Verge AI", url: "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml", category: "Yapay Zeka" },
  { name: "Google AI", url: "https://blog.google/technology/ai/rss/", category: "Yapay Zeka" },
  { name: "Bloomberg HT", url: "https://www.bloomberght.com/rss", category: "Finans" },
  { name: "NTV Ekonomi", url: "https://www.ntv.com.tr/ekonomi.rss", category: "Ekonomi" },
  { name: "TRT Haber Ekonomi", url: "https://www.trthaber.com/ekonomi_articles.rss", category: "Ekonomi" },
  { name: "Koin Bulteni", url: "https://koinbulteni.com/feed", category: "Kripto" },
  { name: "TRT Haber Spor", url: "https://www.trthaber.com/spor_articles.rss", category: "Spor" },
  { name: "Anadolu Ajansi Spor", url: "https://www.aa.com.tr/tr/rss/default?cat=spor", category: "Spor" },
  { name: "BBC Sport", url: "https://feeds.bbci.co.uk/sport/rss.xml", category: "Spor" },
  { name: "Evrim Agaci", url: "https://evrimagaci.org/rss.xml", category: "Bilim" },
  { name: "Webrazzi", url: "https://webrazzi.com/feed/", category: "Girisimcilik" },
  { name: "Anadolu Ajansi Kultur", url: "https://www.aa.com.tr/tr/rss/default?cat=kultur", category: "Kultur ve Sanat" },
  { name: "NTV Saglik", url: "https://www.ntv.com.tr/saglik.rss", category: "Saglik" },
  { name: "Anadolu Ajansi Saglik", url: "https://www.aa.com.tr/tr/rss/default?cat=saglik", category: "Saglik" },
  { name: "BBC World", url: "https://feeds.bbci.co.uk/news/world/rss.xml", category: "Dunya" },
  { name: "DW Turkce", url: "https://rss.dw.com/xml/rss-tur-all", category: "Dunya" }
];

export const TELEGRAM_SOURCES: TelegramSource[] = [
  { handle: "pusholder", name: "Pusholder", category: "Son Dakika" },
  { handle: "bpthaber", name: "BPT", category: "Son Dakika" },
  { handle: "solcugazete", name: "Solcu Gazete", category: "Son Dakika" },
  { handle: "darkwebhaber", name: "DarkWeb Haber", category: "Son Dakika" },
  { handle: "trthaber", name: "TRT Haber", category: "Turkiye" },
  { handle: "anadoluajansi", name: "Anadolu Ajansi", category: "Turkiye" },
  { handle: "ntvhaber", name: "NTV Haber", category: "Turkiye" },
  { handle: "webtekno", name: "Webtekno", category: "Teknoloji" },
  { handle: "shiftdeletenet", name: "ShiftDelete", category: "Teknoloji" },
  { handle: "donanimhaber", name: "DonanimHaber", category: "Teknoloji" }
];
