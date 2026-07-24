import type { ReadyArticle } from "../domain/types.js";
import type { NewsStore, RejectedArticle } from "../pipeline/contracts.js";

export class InMemoryNewsStore implements NewsStore {
  private readonly ready = new Map<string, ReadyArticle>();
  private readonly analysisUsage = new Map<string, number>();
  readonly rejections: RejectedArticle[] = [];

  async hasReady(id: string, analysisVersion: number): Promise<boolean> {
    return this.ready.get(id)?.analysisVersion === analysisVersion;
  }

  async hasRecentRejection(id: string, analysisVersion: number, rejectedAfter: number): Promise<boolean> {
    return this.rejections.some((rejection) =>
      rejection.cluster.id === id &&
      rejection.analysisVersion === analysisVersion &&
      rejection.rejectedAt >= rejectedAfter
    );
  }

  async reserveAnalysisSlot(dayKey: string, dailyLimit: number): Promise<boolean> {
    const used = this.analysisUsage.get(dayKey) ?? 0;
    if (used >= dailyLimit) return false;
    this.analysisUsage.set(dayKey, used + 1);
    return true;
  }

  async releaseAnalysisSlot(dayKey: string): Promise<void> {
    const used = this.analysisUsage.get(dayKey) ?? 0;
    this.analysisUsage.set(dayKey, Math.max(0, used - 1));
  }

  async saveReady(article: ReadyArticle): Promise<void> {
    this.ready.set(article.id, article);
  }

  async saveRejection(rejection: RejectedArticle): Promise<void> {
    this.rejections.push(rejection);
  }

  async listReady(options: { category?: string; limit?: number } = {}): Promise<ReadyArticle[]> {
    const category = options.category;
    return [...this.ready.values()]
      .filter((article) => !category || article.category === category)
      .sort((a, b) => b.publishedAt - a.publishedAt || a.id.localeCompare(b.id))
      .slice(0, options.limit ?? 100);
  }
}
