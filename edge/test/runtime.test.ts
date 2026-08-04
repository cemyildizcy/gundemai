import assert from "node:assert/strict";
import test from "node:test";

import { resolveFeedLimit, evaluatePipelineHealth } from "../src/feed";
import { NEWS_SOURCES, scheduledSourceShard } from "../src/sources";

test("feed accepts the 250 article limit requested by Android", () => {
  assert.equal(resolveFeedLimit("250"), 250);
  assert.equal(resolveFeedLimit("999"), 250);
  assert.equal(resolveFeedLimit("invalid"), 100);
});

test("pipeline health is healthy only when the latest run is fresh and error-free", () => {
  const checkedAt = Date.UTC(2026, 6, 28, 12, 0, 0);
  assert.deepEqual(
    evaluatePipelineHealth({ last_run_at: checkedAt - 3 * 60_000, last_error: null }, checkedAt),
    { ok: true, status: "healthy", reasons: [] }
  );
});

test("pipeline health exposes source or AI errors as degraded", () => {
  const checkedAt = Date.UTC(2026, 6, 28, 12, 0, 0);
  const result = evaluatePipelineHealth(
    { last_run_at: checkedAt - 2 * 60_000, last_error: "BBC: HTTP 503" },
    checkedAt
  );
  assert.equal(result.ok, false);
  assert.equal(result.status, "degraded");
  assert.deepEqual(result.reasons, ["pipeline_error"]);
});

test("pipeline health reports a run older than six minutes as stale", () => {
  const checkedAt = Date.UTC(2026, 6, 28, 12, 0, 0);
  const result = evaluatePipelineHealth(
    { last_run_at: checkedAt - 7 * 60_000, last_error: null },
    checkedAt
  );
  assert.equal(result.ok, false);
  assert.equal(result.status, "stale");
  assert.deepEqual(result.reasons, ["last_run_stale"]);
});

test("three-minute workflow runs rotate through one source shard at a time", () => {
  const start = Date.UTC(2026, 6, 28, 12, 0, 0);
  const shards = Array.from({ length: 6 }, (_, index) =>
    scheduledSourceShard(start + index * 3 * 60_000)
  );
  assert.deepEqual(shards, [0, 1, 2, 3, 4, 0]);
});

test("the source list excludes feeds that consistently block Cloudflare Workers", () => {
  assert.equal(NEWS_SOURCES.some((source) => source.name === "DonanimHaber"), false);
});
