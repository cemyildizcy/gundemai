import { cleanText, fold, truncate } from "./text";

const ENGLISH_MARKERS = new Set([
  "a", "after", "an", "and", "are", "as", "at", "before", "by", "for", "from",
  "how", "in", "is", "launches", "new", "of", "on", "or", "releases", "says",
  "the", "to", "what", "who", "why", "with"
]);

const TURKISH_MARKERS = new Set([
  "acikladi", "ardindan", "bir", "dayali", "etmeye", "icin", "icerik", "ile",
  "ise", "modelini", "olarak", "sonra", "tahmin", "tanitti", "tarafindan", "ve",
  "veya", "yeni", "yildizini", "yayimlandi", "yayinlandi"
]);

function languageScores(value: string): { english: number; turkish: number } {
  const tokens = fold(value).split(/\s+/).filter(Boolean);
  return tokens.reduce(
    (scores, token) => ({
      english: scores.english + Number(ENGLISH_MARKERS.has(token)),
      turkish: scores.turkish + Number(TURKISH_MARKERS.has(token))
    }),
    { english: 0, turkish: 0 }
  );
}

function isLikelyEnglish(value: string): boolean {
  const scores = languageScores(value);
  return scores.english > 0 && scores.english > scores.turkish;
}

function firstSentence(value: string): string {
  const cleaned = cleanText(value);
  const boundary = cleaned.search(/[.!?](?:\s|$)/);
  const sentence = boundary >= 0 ? cleaned.slice(0, boundary) : cleaned;
  return truncate(sentence.trim(), 180);
}

/**
 * Keeps trustworthy Turkish source headlines intact. For foreign headlines,
 * prefers the model's source-grounded Turkish headline and uses the already
 * validated Turkish analysis as a legacy fallback.
 */
export function choosePublishedTitle(
  rawTitle: string,
  translatedTitle: string | null | undefined,
  whatHappened: string
): string {
  const raw = truncate(cleanText(rawTitle), 300);
  if (!isLikelyEnglish(raw)) return raw;

  const translated = truncate(cleanText(translatedTitle ?? ""), 180);
  if (translated && !isLikelyEnglish(translated)) return translated;

  const analysisHeadline = firstSentence(whatHappened);
  if (analysisHeadline.length >= 12 && !isLikelyEnglish(analysisHeadline)) return analysisHeadline;
  return raw;
}
