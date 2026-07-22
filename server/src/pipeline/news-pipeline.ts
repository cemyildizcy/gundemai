import { clusterRawArticles } from "../domain/clustering.js";
import { ANALYSIS_VERSION, createReadyArticle } from "../domain/publication.js";
import type { ReadyArticle } from "../domain/types.js";
import type { NewsAnalyzer, NewsCollector, NewsStore } from "./contracts.js";

export interface PipelineResult {
  collected: number;
  clustered: number;
  published: number;
  skipped: number;
  deferred: number;
  rejected: number;
  rejectionReasons: string[];
  collectorErrors: number;
  publishedArticles: ReadyArticle[];
}

const REJECTION_RETRY_DELAY_MS = 6 * 60 * 60 * 1000;
const QUOTA_DAY_FORMATTER = new Intl.DateTimeFormat("en-CA", {
  timeZone: "UTC",
  year: "numeric",
  month: "2-digit",
  day: "2-digit"
});

export class NewsPipeline {
  private readonly collectors: NewsCollector[];
  private readonly analyzer: NewsAnalyzer;
  private readonly store: NewsStore;
  private readonly now: () => number;
  private readonly maxNewArticles: number;
  private readonly maxNewArticlesPerDay: number;
  private readonly maxCandidatesPerRun: number;

  constructor(options: {
    collectors: NewsCollector[];
    analyzer: NewsAnalyzer;
    store: NewsStore;
    now?: () => number;
    maxNewArticles?: number;
    maxNewArticlesPerDay?: number;
    maxCandidatesPerRun?: number;
  }) {
    this.collectors = options.collectors;
    this.analyzer = options.analyzer;
    this.store = options.store;
    this.now = options.now ?? Date.now;
    this.maxNewArticles = options.maxNewArticles ?? Number.POSITIVE_INFINITY;
    this.maxNewArticlesPerDay = options.maxNewArticlesPerDay ?? Number.POSITIVE_INFINITY;
    this.maxCandidatesPerRun = options.maxCandidatesPerRun ?? Number.POSITIVE_INFINITY;
  }

  async run(): Promise<PipelineResult> {
    const collections = await Promise.allSettled(this.collectors.map((collector) => collector.collect()));
    const rawArticles = collections.flatMap((result) => result.status === "fulfilled" ? result.value : []);
    const allClusters = clusterRawArticles(rawArticles);
    const clusters = allClusters.slice(0, this.maxCandidatesPerRun);
    const counters = {
      published: 0,
      skipped: 0,
      deferred: allClusters.length - clusters.length,
      rejected: 0,
      rejectionReasons: [] as string[]
    };
    const publishedArticles: ReadyArticle[] = [];
    let analysisAttempts = 0;
    let dailyQuotaExhausted = false;

    for (const cluster of clusters) {
      const now = this.now();
      if (await this.store.hasReady(cluster.id, ANALYSIS_VERSION) ||
          await this.store.hasRecentRejection(cluster.id, ANALYSIS_VERSION, now - REJECTION_RETRY_DELAY_MS)) {
        counters.skipped += 1;
        continue;
      }

      if (analysisAttempts >= this.maxNewArticles || dailyQuotaExhausted) {
        counters.deferred += 1;
        continue;
      }

      if (Number.isFinite(this.maxNewArticlesPerDay)) {
        const dayKey = QUOTA_DAY_FORMATTER.format(new Date(now));
        const reserved = await this.store.reserveAnalysisSlot(dayKey, this.maxNewArticlesPerDay);
        if (!reserved) {
          dailyQuotaExhausted = true;
          counters.deferred += 1;
          continue;
        }
      }

      try {
        analysisAttempts += 1;
        const analysis = await this.analyzer.analyze(cluster);
        const article = createReadyArticle(cluster, analysis, now);
        await this.store.saveReady(article);
        publishedArticles.push(article);
        counters.published += 1;
      } catch (error) {
        const reason = sanitizeRejectionReason(error);
        counters.rejected += 1;
        counters.rejectionReasons.push(reason);
        await this.store.saveRejection({
          cluster,
          reason,
          rejectedAt: now,
          analysisVersion: ANALYSIS_VERSION
        });
      }
    }

    return {
      collected: rawArticles.length,
      clustered: allClusters.length,
      ...counters,
      collectorErrors: collections.filter((result) => result.status === "rejected").length,
      publishedArticles
    };
  }
}

function sanitizeRejectionReason(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message
    .replace(/sk-or-v1-[A-Za-z0-9_-]+/g, "[redacted-openrouter-key]")
    .replace(/[\r\n\t]+/g, " ")
    .slice(0, 4_000);
}
