import { CATEGORY_NAMES, type CategoryName } from "./types.js";

function fold(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/ı/g, "i")
    .replace(/İ/g, "I")
    .replace(/â/g, "a")
    .replace(/[^a-zA-Z0-9]+/g, " ")
    .trim()
    .toLowerCase();
}

const ALIASES: Record<string, CategoryName> = {
  "son dakika": "Son Dakika",
  gundem: "Son Dakika",
  general: "Son Dakika",
  "yapay zeka": "Yapay Zeka",
  ai: "Yapay Zeka",
  teknoloji: "Teknoloji",
  technology: "Teknoloji",
  turkiye: "Turkiye",
  turkey: "Turkiye",
  dunya: "Dunya",
  world: "Dunya",
  ekonomi: "Ekonomi",
  economy: "Ekonomi",
  business: "Ekonomi",
  finans: "Finans",
  finance: "Finans",
  kripto: "Kripto",
  crypto: "Kripto",
  spor: "Spor",
  sports: "Spor",
  transfer: "Transfer",
  bilim: "Bilim",
  science: "Bilim",
  oyun: "Oyun",
  gaming: "Oyun",
  girisimcilik: "Girisimcilik",
  startup: "Girisimcilik",
  "kultur ve sanat": "Kultur ve Sanat",
  sanat: "Kultur ve Sanat",
  saglik: "Saglik",
  health: "Saglik"
};

export function normalizeCategory(value: string | null | undefined): CategoryName {
  if (!value) return "Son Dakika";
  const normalized = fold(value.replace(/_/g, " "));
  return ALIASES[normalized] ?? "Son Dakika";
}

const CATEGORY_RULES: ReadonlyArray<{ category: CategoryName; patterns: RegExp[] }> = [
  { category: "Yapay Zeka", patterns: [/\byapay zeka\b/, /\bartificial intelligence\b/, /\bopenai\b/, /\bchatgpt\b/, /\bgemini\b/, /\banthropic\b/, /\bclaude\b/, /\bllm\b/, /\bai model/] },
  { category: "Kripto", patterns: [/\bkripto\b/, /\bcrypto\b/, /\bbitcoin\b/, /\bethereum\b/, /\bblockchain\b/, /\bbinance\b/] },
  { category: "Finans", patterns: [/\bfaiz\b/, /\bmerkez bankasi\b/, /\btcmb\b/, /\bborsa\b/, /\bbist\b/, /\bhisse\b/, /\bdolar\b/, /\beuro\b/, /\bmevduat\b/, /\bkredi\b/] },
  { category: "Ekonomi", patterns: [/\bekonomi\b/, /\benflasyon\b/, /\bihracat\b/, /\bithalat\b/, /\bzam\b/, /\bissizlik\b/] },
  { category: "Transfer", patterns: [/\btransfer\b/, /\bbonservis\b/, /\bkiralik\b/, /\bsozlesme imzaladi\b/] },
  { category: "Spor", patterns: [/\bfutbol\b/, /\bbasketbol\b/, /\bmac\b/, /\blig\b/, /\bfenerbahce\b/, /\bgalatasaray\b/, /\bbesiktas\b/] },
  { category: "Saglik", patterns: [/\bsaglik\b/, /\bhastalik\b/, /\basi\b/, /\bvirus\b/, /\bdoktor\b/, /\bhastane\b/] },
  { category: "Bilim", patterns: [/\bbilim\b/, /\bnasa\b/, /\buzay\b/, /\barastirma\b/, /\bfizik\b/] },
  { category: "Oyun", patterns: [/\boyun\b/, /\bgaming\b/, /\bplaystation\b/, /\bxbox\b/, /\bsteam\b/, /\bnintendo\b/] },
  { category: "Girisimcilik", patterns: [/\bgirisim\b/, /\bstartup\b/, /\byatirim turu\b/, /\bventure\b/] },
  { category: "Kultur ve Sanat", patterns: [/\bsanat\b/, /\bsinema\b/, /\btiyatro\b/, /\bmuzik\b/, /\bsergi\b/] },
  { category: "Teknoloji", patterns: [/\bteknoloji\b/, /\byazilim\b/, /\bandroid\b/, /\biphone\b/, /\bapple\b/, /\bsamsung\b/, /\bsiber\b/] },
  { category: "Turkiye", patterns: [/\bturkiye\b/, /\bankara\b/, /\bistanbul\b/, /\btbmm\b/, /\bbakanlik\b/] },
  { category: "Dunya", patterns: [/\bdunya\b/, /\babd\b/, /\bavrupa\b/, /\brusya\b/, /\bukrayna\b/, /\bisrail\b/, /\bfilistin\b/] }
];

export function categorizeArticle(input: {
  title: string;
  description: string;
  requestedCategory?: string | null;
}): CategoryName {
  const text = fold(`${input.title} ${input.description}`);
  let best: { category: CategoryName; score: number } | null = null;

  for (const rule of CATEGORY_RULES) {
    const score = rule.patterns.reduce((sum, pattern) => sum + (pattern.test(text) ? 1 : 0), 0);
    if (score > 0 && (!best || score > best.score)) best = { category: rule.category, score };
  }

  return best?.category ?? normalizeCategory(input.requestedCategory);
}

export function isCategoryName(value: string): value is CategoryName {
  return CATEGORY_NAMES.includes(value as CategoryName);
}
