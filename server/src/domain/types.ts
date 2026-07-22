export const CATEGORY_NAMES = [
  "Son Dakika",
  "Yapay Zeka",
  "Teknoloji",
  "Turkiye",
  "Dunya",
  "Ekonomi",
  "Finans",
  "Kripto",
  "Spor",
  "Transfer",
  "Bilim",
  "Oyun",
  "Girisimcilik",
  "Kultur ve Sanat",
  "Saglik"
] as const;

export type CategoryName = (typeof CATEGORY_NAMES)[number];

export interface SourceReference {
  name: string;
  url: string;
  publishedAt: number;
  headline?: string;
}

export interface ArticleCluster {
  id: string;
  title: string;
  description: string;
  content: string;
  categoryHint: string;
  publishedAt: number;
  imageUrl: string | null;
  sources: SourceReference[];
}

export type VerificationStatus =
  | "OFFICIAL_CONFIRMED"
  | "MULTI_SOURCE_CONFIRMED"
  | "SINGLE_SOURCE_REPORT"
  | "UNVERIFIED_CLAIM"
  | "DEVELOPING_STORY"
  | "SOURCES_CONFLICT"
  | "INSUFFICIENT_INFORMATION";

export interface AiAnalysis {
  title: string;
  summary: string;
  category: CategoryName;
  whatHappened: string;
  whyImportant: string;
  missingInformation: string;
  verificationStatus: VerificationStatus;
  confidenceScore: number;
  possibleImpacts: string[];
  unverifiedClaims: string[];
  contradictions: string[];
  verifiedFacts: string[];
}

export interface ReadyArticle extends AiAnalysis {
  id: string;
  status: "READY";
  readyAt: number;
  publishedAt: number;
  imageUrl: string | null;
  sourceName: string;
  sourceUrl: string;
  sourceCount: number;
  sources: SourceReference[];
  analysisVersion: number;
}
