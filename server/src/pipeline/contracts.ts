import type { RawArticle } from "../domain/raw-article.js";
import type { AiAnalysis, ArticleCluster, ReadyArticle } from "../domain/types.js";

export interface NewsCollector {
  collect(): Promise<RawArticle[]>;
}

export interface NewsAnalyzer {
  analyze(cluster: ArticleCluster): Promise<AiAnalysis>;
}

export interface RejectedArticle {
  cluster: ArticleCluster;
  reason: string;
  rejectedAt: number;
  analysisVersion: number;
}

export interface NewsStore {
  hasReady(id: string, analysisVersion: number): Promise<boolean>;
  hasRecentRejection(id: string, analysisVersion: number, rejectedAfter: number): Promise<boolean>;
  reserveAnalysisSlot(dayKey: string, dailyLimit: number): Promise<boolean>;
  releaseAnalysisSlot(dayKey: string): Promise<void>;
  saveReady(article: ReadyArticle): Promise<void>;
  saveRejection(rejection: RejectedArticle): Promise<void>;
  listReady(options?: { category?: string; limit?: number }): Promise<ReadyArticle[]>;
}
