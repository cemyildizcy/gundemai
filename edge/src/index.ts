import { WorkflowEntrypoint, type WorkflowEvent, type WorkflowStep } from "cloudflare:workers";

import { analyzePending } from "./analyzer";
import { collectShard, type CollectionResult } from "./collector";
import { buildFeed, buildHealth } from "./feed";
import { sendNotifications } from "./notifications";
import { SOURCE_SHARD_COUNT } from "./sources";
import type { Env } from "./types";

interface WorkflowParams {
  reason?: "manual" | "schedule";
}

const SCHEDULE_INTERVAL_MS = 3 * 60 * 1000;

function scheduledInstanceId(scheduledTime: number): string {
  return `schedule-${Math.floor(scheduledTime / SCHEDULE_INTERVAL_MS)}`;
}

async function finishRun(env: Env, collections: CollectionResult[]) {
  const now = Date.now();
  const analysis = await analyzePending(env, now);
  let notifications = {
    attempted: 0,
    sent: 0,
    failed: 0,
    skipped: true,
    errors: [] as string[]
  };
  try {
    notifications = await sendNotifications(env);
  } catch (error) {
    notifications.errors.push(error instanceof Error ? error.message : String(error));
  }

  await env.DB.batch([
    env.DB.prepare(`
      DELETE FROM news_items
      WHERE status = 'READY' AND published_at < ?
    `).bind(now - 90 * 24 * 60 * 60 * 1000),
    env.DB.prepare(`
      DELETE FROM news_items
      WHERE status = 'RETRY' AND attempts >= 20 AND discovered_at < ?
    `).bind(now - 30 * 24 * 60 * 60 * 1000),
    env.DB.prepare(`
      UPDATE pipeline_state SET
        last_run_at = ?,
        last_collected_count = ?,
        last_published_count = ?,
        last_notification_count = ?,
        last_error = ?
      WHERE singleton = 1
    `).bind(
      now,
      collections.reduce((total, result) => total + result.insertedItems, 0),
      analysis.published,
      notifications.sent,
      [...collections.flatMap((result) => result.errors), ...analysis.errors, ...notifications.errors]
        .slice(0, 8)
        .join(" | ") || null
    )
  ]);

  return { collections, analysis, notifications, completedAt: now };
}

export class NewsWorkflow extends WorkflowEntrypoint<Env, WorkflowParams> {
  async run(_event: WorkflowEvent<WorkflowParams>, step: WorkflowStep) {
    const collections: CollectionResult[] = [];
    for (let shard = 0; shard < SOURCE_SHARD_COUNT; shard += 1) {
      const result = await step.do(`collect-source-shard-${shard + 1}`, async () =>
        collectShard(this.env, shard)
      );
      collections.push(result);
    }
    return step.do("analyze-publish-notify", async () => finishRun(this.env, collections));
  }
}

function unauthorized(): Response {
  return Response.json({ error: "Unauthorized" }, { status: 401 });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "GET" && (url.pathname === "/v1/news" || url.pathname === "/v1/news.json")) {
      return buildFeed(env, url);
    }
    if (request.method === "GET" && url.pathname === "/health.json") return buildHealth(env);
    if (request.method === "POST" && url.pathname === "/admin/run") {
      if (!env.ADMIN_TOKEN) {
        return Response.json({ error: "ADMIN_TOKEN is not configured" }, { status: 503 });
      }
      if (request.headers.get("Authorization") !== `Bearer ${env.ADMIN_TOKEN}`) return unauthorized();
      const instance = await env.NEWS_WORKFLOW.create({
        id: crypto.randomUUID(),
        params: { reason: "manual" }
      });
      return Response.json({ accepted: true, instanceId: instance.id }, { status: 202 });
    }
    if (request.method === "GET" && url.pathname === "/") {
      return Response.json({
        service: "GundemAI Edge",
        status: "ok",
        news: "/v1/news",
        health: "/health.json"
      });
    }
    return Response.json({ error: "Not found" }, { status: 404 });
  },
  async scheduled(controller: ScheduledController, env: Env, ctx: ExecutionContext): Promise<void> {
    ctx.waitUntil(
      env.NEWS_WORKFLOW.create({
        id: scheduledInstanceId(controller.scheduledTime),
        params: { reason: "schedule" }
      })
    );
  }
} satisfies ExportedHandler<Env>;
