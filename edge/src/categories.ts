import { CATEGORY_NAMES, type CategoryName } from "./types";
import { fold } from "./text";

const RULES: ReadonlyArray<{ category: CategoryName; patterns: RegExp[] }> = [
  { category: "Yapay Zeka", patterns: [/\byapay zeka\b/, /\bartificial intelligence\b/, /\bopenai\b/, /\bchatgpt\b/, /\bgemini\b/, /\banthropic\b/, /\bllm\b/] },
  { category: "Kripto", patterns: [/\bkripto\b/, /\bcrypto\b/, /\bbitcoin\b/, /\bethereum\b/, /\bblockchain\b/, /\bbinance\b/] },
  { category: "Finans", patterns: [/\bfaiz\b/, /\bmerkez bankasi\b/, /\btcmb\b/, /\bborsa\b/, /\bbist\b/, /\bhisse\b/, /\bdolar\b/, /\beuro\b/, /\bkredi\b/] },
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

export function categoryFor(title: string, description: string, hint: string): CategoryName {
  const text = fold(`${title} ${description}`);
  let best: { category: CategoryName; score: number } | undefined;
  for (const rule of RULES) {
    const score = rule.patterns.reduce((total, pattern) => total + Number(pattern.test(text)), 0);
    if (score > 0 && (!best || score > best.score)) best = { category: rule.category, score };
  }
  if (best) return best.category;
  return CATEGORY_NAMES.includes(hint as CategoryName) ? hint as CategoryName : "Son Dakika";
}
