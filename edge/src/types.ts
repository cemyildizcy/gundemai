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

export interface Env {
  DB: D1Database;
  AI: Ai;
  NEWS_WORKFLOW: Workflow;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_SERVICE_ACCOUNT?: string;
  ADMIN_TOKEN?: string;
  LEGACY_FEED_URL?: string;
  ANALYSES_PER_RUN?: string;
  OPENROUTER_API_KEY?: string;
  OPENROUTER_MODELS?: string;
}

export interface NewsSource {
  kind: "rss" | "telegram";
  name: string;
  url: string;
  category: CategoryName;
}

export interface RawNewsItem {
  title: string;
  description: string;
  categoryHint: CategoryName;
  imageUrl: string | null;
  url: string;
  sourceName: string;
  publishedAt: number;
}

export interface QueueRow {
  id: string;
  raw_title: string;
  raw_description: string;
  category_hint: string;
  image_url: string | null;
  published_at: number;
  attempts: number;
}

export interface ReadyRow {
  id: string;
  title: string;
  summary: string;
  category: CategoryName;
  image_url: string | null;
  published_at: number;
  ready_at: number;
  what_happened: string;
  why_important: string;
  missing_information: string;
  verification_status: string;
  confidence_score: number;
  possible_impacts: string;
  unverified_claims: string;
  contradictions: string;
  verified_facts: string;
}

export interface SourceRow {
  article_id: string;
  name: string;
  url: string;
  headline: string;
  published_at: number;
}
