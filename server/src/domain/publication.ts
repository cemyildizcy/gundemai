import { validateAnalysis } from "./analysis-quality.js";
import type { AiAnalysis, ArticleCluster, ReadyArticle } from "./types.js";

export const ANALYSIS_VERSION = 1;

export function createReadyArticle(
  cluster: ArticleCluster,
  analysis: AiAnalysis,
  readyAt: number = Date.now()
): ReadyArticle {
  const quality = validateAnalysis(cluster, analysis);
  if (!quality.ok) {
    throw new Error(`Analysis quality gate rejected article: ${quality.reasons.join("; ")}`);
  }

  const primarySource = cluster.sources[0];
  if (!primarySource) throw new Error("Analysis quality gate rejected article: source is required");

  return {
    ...analysis,
    id: cluster.id,
    status: "READY",
    readyAt,
    publishedAt: cluster.publishedAt,
    imageUrl: cluster.imageUrl,
    sourceName: primarySource.name,
    sourceUrl: primarySource.url,
    sourceCount: cluster.sources.length,
    sources: cluster.sources,
    analysisVersion: ANALYSIS_VERSION
  };
}
