import express, { type Express } from "express";

import type { NewsStore } from "../pipeline/contracts.js";
import { accountDeletionPage, privacyPage } from "./legal-pages.js";

export function createHttpApp(options: {
  store: NewsStore;
  cronSecret: string;
  supportEmail: string;
  runPipeline: () => Promise<unknown>;
}): Express {
  const app = express();
  let ingestRunning = false;
  app.disable("x-powered-by");
  app.use(express.json({ limit: "32kb" }));

  app.get("/health", (_request, response) => {
    response.json({ ok: true, service: "gundemai-server" });
  });

  app.get("/privacy", (_request, response) => {
    response.type("html").send(privacyPage(options.supportEmail));
  });

  app.get("/account-deletion", (_request, response) => {
    response.type("html").send(accountDeletionPage(options.supportEmail));
  });

  app.get("/v1/news", async (request, response, next) => {
    try {
      const requestedLimit = Number(request.query.limit ?? 50);
      const limit = Number.isFinite(requestedLimit)
        ? Math.max(1, Math.min(100, Math.floor(requestedLimit)))
        : 50;
      const category = typeof request.query.category === "string" && request.query.category.trim()
        ? request.query.category.trim()
        : undefined;
      const articles = await options.store.listReady(category ? { category, limit } : { limit });
      response.set("Cache-Control", "public, max-age=60, stale-while-revalidate=120");
      response.json({ articles, sharedAnalysis: true, generatedAt: Date.now() });
    } catch (error) {
      next(error);
    }
  });

  app.post("/internal/ingest", async (request, response, next) => {
    const authorization = request.header("Authorization");
    if (!options.cronSecret || authorization !== `Bearer ${options.cronSecret}`) {
      response.status(401).json({ error: "unauthorized" });
      return;
    }
    if (ingestRunning) {
      response.status(409).json({ error: "ingestion_already_running" });
      return;
    }

    ingestRunning = true;
    try {
      response.json(await options.runPipeline());
    } catch (error) {
      next(error);
    } finally {
      ingestRunning = false;
    }
  });

  app.use((error: unknown, _request: express.Request, response: express.Response, _next: express.NextFunction) => {
    console.error(error);
    response.status(500).json({ error: "internal_server_error" });
  });

  return app;
}
