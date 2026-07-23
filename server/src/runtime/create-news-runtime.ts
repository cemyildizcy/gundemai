import { Firestore } from "@google-cloud/firestore";

import {
  CloudflareWorkersAiProvider,
  GeminiProvider,
  OpenRouterProvider
} from "../ai/http-providers.js";
import { ServerNewsAnalyzer, type AiTextProvider } from "../ai/server-news-analyzer.js";
import { GNewsCollector, NewsApiCollector } from "../collectors/api-collectors.js";
import { RssCollector } from "../collectors/rss-collector.js";
import { TelegramCollector } from "../collectors/telegram-collector.js";
import { RSS_SOURCES, TELEGRAM_SOURCES } from "../config/sources.js";
import type { NewsCollector } from "../pipeline/contracts.js";
import { NewsPipeline } from "../pipeline/news-pipeline.js";
import { FirestoreNewsStore } from "../storage/firestore-news-store.js";

export const DEFAULT_OPENROUTER_MODELS = [
  "nvidia/nemotron-3-ultra-550b-a55b:free",
  "openrouter/free",
  "nvidia/nemotron-3-super-120b-a12b:free",
] as const;

export const DEFAULT_CLOUDFLARE_MODELS = [
  "@cf/google/gemma-4-26b-a4b-it",
  "@cf/zai-org/glm-4.7-flash"
] as const;

export function createNewsRuntime(environment: NodeJS.ProcessEnv = process.env): {
  store: FirestoreNewsStore;
  pipeline: NewsPipeline;
} {
  const providers: AiTextProvider[] = [];
  if (environment.CLOUDFLARE_ACCOUNT_ID && environment.CLOUDFLARE_API_TOKEN) {
    for (const model of cloudflareModels(environment)) {
      providers.push(new CloudflareWorkersAiProvider(
        environment.CLOUDFLARE_ACCOUNT_ID,
        environment.CLOUDFLARE_API_TOKEN,
        model
      ));
    }
  }
  if (environment.OPENROUTER_API_KEY) {
    for (const model of openRouterModels(environment)) {
      providers.push(new OpenRouterProvider(environment.OPENROUTER_API_KEY, model));
    }
  }
  if (environment.GEMINI_API_KEY) {
    providers.push(new GeminiProvider(
      environment.GEMINI_API_KEY,
      environment.GEMINI_MODEL || "gemini-3.6-flash"
    ));
  }
  if (providers.length === 0) {
    throw new Error("Configure Cloudflare Workers AI, OpenRouter, or Gemini");
  }

  const collectors: NewsCollector[] = [
    new RssCollector(RSS_SOURCES),
    new TelegramCollector(TELEGRAM_SOURCES)
  ];
  if (environment.GNEWS_API_KEY) collectors.push(new GNewsCollector(environment.GNEWS_API_KEY));
  if (environment.NEWS_API_KEY) collectors.push(new NewsApiCollector(environment.NEWS_API_KEY));

  const projectId = environment.GOOGLE_CLOUD_PROJECT || environment.GCLOUD_PROJECT;
  const store = new FirestoreNewsStore(new Firestore(projectId ? { projectId } : {}));
  const maxNewArticles = positiveInteger(environment.MAX_AI_ARTICLES_PER_RUN, 1);
  const maxAnalysisAttempts = positiveInteger(
    environment.MAX_AI_ATTEMPTS_PER_RUN,
    Math.max(maxNewArticles, maxNewArticles * 3)
  );
  const maxNewArticlesPerDay = positiveInteger(environment.MAX_AI_ARTICLES_PER_DAY, 50);
  const maxCandidatesPerRun = positiveInteger(environment.MAX_CANDIDATES_PER_RUN, 25);
  const pipeline = new NewsPipeline({
    collectors,
    analyzer: new ServerNewsAnalyzer(providers),
    store,
    maxNewArticles,
    maxAnalysisAttempts,
    maxNewArticlesPerDay,
    maxCandidatesPerRun
  });
  return { store, pipeline };
}

function positiveInteger(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function openRouterModels(environment: NodeJS.ProcessEnv): string[] {
  const configured = environment.OPENROUTER_MODELS?.trim();
  const legacySingleModel = environment.OPENROUTER_MODEL?.trim();
  const values = configured
    ? configured.split(",")
    : legacySingleModel
      ? [legacySingleModel]
      : [...DEFAULT_OPENROUTER_MODELS];
  return [...new Set(values.map((model) => model.trim()).filter(Boolean))].slice(0, 10);
}

export function cloudflareModels(environment: NodeJS.ProcessEnv): string[] {
  const configured = environment.CLOUDFLARE_MODELS?.trim();
  const legacySingleModel = environment.CLOUDFLARE_MODEL?.trim();
  const values = configured
    ? configured.split(",")
    : legacySingleModel
      ? [legacySingleModel]
      : [...DEFAULT_CLOUDFLARE_MODELS];
  return [...new Set(values.map((model) => model.trim()).filter(Boolean))].slice(0, 5);
}
