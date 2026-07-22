import assert from "node:assert/strict";
import test from "node:test";

import {
  DEFAULT_OPENROUTER_MODELS,
  openRouterModels
} from "../src/runtime/create-news-runtime.js";

test("uses the requested five free OpenRouter models in priority order", () => {
  assert.deepEqual(openRouterModels({}), [
    "nvidia/nemotron-3-ultra-550b-a55b:free",
    "google/gemma-4-31b-it:free",
    "nvidia/nemotron-3-super-120b-a12b:free",
    "google/gemma-4-26b-a4b-it:free",
    "z-ai/glm-4.5-air:free"
  ]);
  assert.equal(DEFAULT_OPENROUTER_MODELS.length, 5);
});

test("accepts a custom comma-separated OpenRouter fallback list", () => {
  assert.deepEqual(openRouterModels({
    OPENROUTER_MODELS: " model/a:free,model/b:free,model/a:free "
  }), ["model/a:free", "model/b:free"]);
});
