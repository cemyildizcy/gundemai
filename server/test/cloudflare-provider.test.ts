import assert from "node:assert/strict";
import test from "node:test";

import { CloudflareWorkersAiProvider } from "../src/ai/http-providers.js";

test("reads a Cloudflare Workers AI chat completion", async () => {
  let requestedUrl = "";
  let authorization = "";
  let requestBody: Record<string, unknown> = {};
  const request = async (input: string | URL | Request, init?: RequestInit): Promise<Response> => {
    requestedUrl = String(input);
    authorization = new Headers(init?.headers).get("Authorization") ?? "";
    requestBody = JSON.parse(String(init?.body)) as Record<string, unknown>;
    return new Response(JSON.stringify({
      result: {
        choices: [{ message: { content: "{\"category\":\"Teknoloji\"}" } }]
      }
    }), { status: 200 });
  };
  const provider = new CloudflareWorkersAiProvider(
    "account-id",
    "api-token",
    "@cf/google/gemma-4-26b-a4b-it",
    1_000,
    request as typeof fetch
  );

  const result = await provider.generate({ system: "system", user: "user" });

  assert.equal(result, "{\"category\":\"Teknoloji\"}");
  assert.match(requestedUrl, /accounts\/account-id\/ai\/v1\/chat\/completions$/);
  assert.equal(authorization, "Bearer api-token");
  assert.equal(requestBody.model, "@cf/google/gemma-4-26b-a4b-it");
  assert.equal(requestBody.max_tokens, 900);
});

test("reads an unwrapped OpenAI-compatible Cloudflare response", async () => {
  const request = async (): Promise<Response> => new Response(JSON.stringify({
    choices: [{ message: { content: [{ type: "text", text: "{\"category\":\"Dünya\"}" }] } }]
  }), { status: 200 });
  const provider = new CloudflareWorkersAiProvider(
    "account-id",
    "api-token",
    "@cf/model",
    1_000,
    request as typeof fetch
  );

  assert.equal(await provider.generate({ system: "system", user: "user" }), "{\"category\":\"Dünya\"}");
});

test("serializes structured Cloudflare responses", async () => {
  const request = async (): Promise<Response> => new Response(JSON.stringify({
    result: { response: { category: "Ekonomi", summary: "Özet" } }
  }), { status: 200 });
  const provider = new CloudflareWorkersAiProvider(
    "account-id",
    "api-token",
    "@cf/model",
    1_000,
    request as typeof fetch
  );

  assert.equal(
    await provider.generate({ system: "system", user: "user" }),
    "{\"category\":\"Ekonomi\",\"summary\":\"Özet\"}"
  );
});
