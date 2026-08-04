import { truncate } from "./text";
import type { Env } from "./types";

export const AI_MODEL = "@cf/google/gemma-4-26b-a4b-it";

const DEFAULT_OPENROUTER_MODELS = [
  "openrouter/free",
  "nvidia/nemotron-3-super-120b-a12b:free"
];

export async function runAiPrompt<T>(
  env: Env,
  prompt: ChatCompletionsMessagesInput,
  parse: (content: string) => T
): Promise<T> {
  const errors: string[] = [];
  try {
    const output = await env.AI.run(AI_MODEL, prompt);
    const content = output.choices[0]?.message.content;
    if (!content) throw new Error("Workers AI bos yanit verdi");
    return parse(content);
  } catch (error) {
    errors.push(`Cloudflare: ${error instanceof Error ? error.message : String(error)}`);
  }

  if (env.OPENROUTER_API_KEY) {
    const configuredModels = env.OPENROUTER_MODELS
      ?.split(",")
      .map((model) => model.trim())
      .filter(Boolean);
    for (const model of configuredModels?.length ? configuredModels : DEFAULT_OPENROUTER_MODELS) {
      try {
        const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
          method: "POST",
          headers: {
            Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
            "Content-Type": "application/json",
            "HTTP-Referer": "https://gundemai.web.app",
            "X-Title": "GundemAI"
          },
          body: JSON.stringify({
            model,
            ...prompt,
            stream: false
          }),
          signal: AbortSignal.timeout(25_000)
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${truncate(await response.text(), 200)}`);
        }
        const output = await response.json<{
          choices?: Array<{ message?: { content?: string | null } }>;
        }>();
        const content = output.choices?.[0]?.message?.content;
        if (!content) throw new Error("bos yanit");
        return parse(content);
      } catch (error) {
        errors.push(`${model}: ${error instanceof Error ? error.message : String(error)}`);
      }
    }
  }
  throw new Error(truncate(errors.join(" | "), 600));
}
