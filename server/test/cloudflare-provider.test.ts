import assert from "node:assert/strict";
import test from "node:test";

import { CloudflareWorkersAiProvider } from "../src/ai/http-providers.js";

test("reads a Cloudflare Workers AI chat completion", async () => {
  let requestedUrl = "";
  let authorization = "";
  const request = async (input: string | URL | Request, init?: RequestInit): Promise<Response> => {
    requestedUrl = String(input);
    authorization = new Headers(init?.headers).get("Authorization") ?? "";
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
  assert.match(requestedUrl, /accounts\/account-id\/ai\/run\/%40cf\/google\/gemma-4-26b-a4b-it$/);
  assert.equal(authorization, "Bearer api-token");
});
