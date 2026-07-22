import type { AiTextProvider } from "./server-news-analyzer.js";

async function fetchJson(
  url: string,
  init: RequestInit,
  timeoutMs: number,
  request: typeof fetch = fetch
): Promise<unknown> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await request(url, { ...init, signal: controller.signal });
    const body = await response.text();
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${body.slice(0, 300)}`);
    return JSON.parse(body) as unknown;
  } finally {
    clearTimeout(timeout);
  }
}

export class CloudflareWorkersAiProvider implements AiTextProvider {
  readonly name: string;

  constructor(
    private readonly accountId: string,
    private readonly apiToken: string,
    private readonly model: string,
    private readonly timeoutMs = 70_000,
    private readonly request: typeof fetch = fetch
  ) {
    this.name = `cloudflare:${model}`;
  }

  async generate(input: { system: string; user: string }): Promise<string> {
    const modelPath = this.model.split("/").map(encodeURIComponent).join("/");
    const url = `https://api.cloudflare.com/client/v4/accounts/${encodeURIComponent(this.accountId)}/ai/run/${modelPath}`;
    const body = await fetchJson(url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.apiToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        messages: [
          { role: "system", content: input.system },
          { role: "user", content: input.user }
        ],
        temperature: 0.1,
        max_completion_tokens: 1600,
        response_format: { type: "json_object" }
      })
    }, this.timeoutMs, this.request) as {
      result?: {
        response?: string;
        choices?: Array<{ message?: { content?: string } }>;
      };
    };
    const content = body.result?.choices?.[0]?.message?.content ?? body.result?.response;
    if (!content) throw new Error("Cloudflare Workers AI returned no content");
    return content;
  }
}

export class OpenRouterProvider implements AiTextProvider {
  readonly name: string;

  constructor(
    private readonly apiKey: string,
    private readonly model: string,
    private readonly timeoutMs = 70_000
  ) {
    this.name = `openrouter:${model}`;
  }

  async generate(input: { system: string; user: string }): Promise<string> {
    const body = await fetchJson("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://gundemai.app",
        "X-Title": "GundemAI Server"
      },
      body: JSON.stringify({
        model: this.model,
        messages: [
          { role: "system", content: input.system },
          { role: "user", content: input.user }
        ],
        temperature: 0.1,
        max_tokens: 1600
      })
    }, this.timeoutMs) as { choices?: Array<{ message?: { content?: string } }> };
    const content = body.choices?.[0]?.message?.content;
    if (!content) throw new Error("OpenRouter returned no content");
    return content;
  }
}

export class GeminiProvider implements AiTextProvider {
  readonly name = "gemini";

  constructor(
    private readonly apiKey: string,
    private readonly model: string,
    private readonly timeoutMs = 30_000
  ) {}

  async generate(input: { system: string; user: string }): Promise<string> {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(this.model)}:generateContent?key=${encodeURIComponent(this.apiKey)}`;
    const body = await fetchJson(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: input.system }] },
        contents: [{ role: "user", parts: [{ text: input.user }] }],
        generationConfig: { responseMimeType: "application/json" }
      })
    }, this.timeoutMs) as { candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }> };
    const content = body.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!content) throw new Error("Gemini returned no content");
    return content;
  }
}
