import type { Firestore, Query } from "@google-cloud/firestore";

import type { ReadyArticle } from "../domain/types.js";
import type { NewsStore, RejectedArticle } from "../pipeline/contracts.js";

export class FirestoreNewsStore implements NewsStore {
  constructor(private readonly firestore: Firestore) {}

  async hasReady(id: string, analysisVersion: number): Promise<boolean> {
    const snapshot = await this.firestore.collection("news_ready").doc(id).get();
    return snapshot.exists && snapshot.get("analysisVersion") === analysisVersion;
  }

  async hasRecentRejection(id: string, analysisVersion: number, rejectedAfter: number): Promise<boolean> {
    const snapshot = await this.firestore.collection("news_rejected").doc(id).get();
    return snapshot.exists &&
      snapshot.get("analysisVersion") === analysisVersion &&
      Number(snapshot.get("rejectedAt")) >= rejectedAfter;
  }

  async reserveAnalysisSlot(dayKey: string, dailyLimit: number): Promise<boolean> {
    const reference = this.firestore.collection("pipeline_state").doc(`ai_quota_${dayKey}`);
    return this.firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(reference);
      const used = snapshot.exists ? Number(snapshot.get("used") ?? 0) : 0;
      if (used >= dailyLimit) return false;
      transaction.set(reference, {
        dayKey,
        used: used + 1,
        updatedAt: Date.now()
      }, { merge: true });
      return true;
    });
  }

  async releaseAnalysisSlot(dayKey: string): Promise<void> {
    const reference = this.firestore.collection("pipeline_state").doc(`ai_quota_${dayKey}`);
    await this.firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(reference);
      if (!snapshot.exists) return;
      const used = Number(snapshot.get("used") ?? 0);
      transaction.set(reference, {
        used: Math.max(0, used - 1),
        updatedAt: Date.now()
      }, { merge: true });
    });
  }

  async saveReady(article: ReadyArticle): Promise<void> {
    await this.firestore.collection("news_ready").doc(article.id).set(article, { merge: false });
  }

  async saveRejection(rejection: RejectedArticle): Promise<void> {
    await this.firestore.collection("news_rejected").doc(rejection.cluster.id).set(rejection, { merge: false });
  }

  async listReady(options: { category?: string; limit?: number } = {}): Promise<ReadyArticle[]> {
    let query: Query = this.firestore.collection("news_ready");
    if (options.category) query = query.where("category", "==", options.category);
    query = query.orderBy("publishedAt", "desc").limit(options.limit ?? 100);
    const snapshot = await query.get();
    return snapshot.docs
      .map((document) => document.data() as ReadyArticle)
      .sort((a, b) => b.publishedAt - a.publishedAt || a.id.localeCompare(b.id));
  }
}
