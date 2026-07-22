import { createHash } from "node:crypto";
import he from "he";

export function cleanSourceText(value: string | null | undefined): string {
  if (!value) return "";
  return he.decode(value)
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/p>/gi, "\n")
    .replace(/<[^>]+>/g, " ")
    .replace(/\[\+\d+\s+chars\]/gi, " ")
    .replace(/[ \t]+/g, " ")
    .replace(/\s*\n\s*/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

export function stableRawId(url: string): string {
  return createHash("sha256").update(url).digest("hex").slice(0, 24);
}

export function parsePublishedAt(value: string | number | null | undefined): number {
  if (typeof value === "number" && Number.isFinite(value)) return value < 1e12 ? value * 1000 : value;
  if (typeof value === "string") {
    const numeric = Number(value);
    if (Number.isFinite(numeric) && numeric > 1e9) return numeric < 1e12 ? numeric * 1000 : numeric;
    const parsed = Date.parse(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return Date.now();
}

export async function fetchText(url: string, timeoutMs = 10_000): Promise<string> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        "User-Agent": "GundemAI-NewsBot/1.0 (+https://gundemai.app)",
        Accept: "application/rss+xml, application/xml, text/xml, text/html, application/json"
      }
    });
    if (!response.ok) throw new Error(`HTTP ${response.status} for ${url}`);
    return await response.text();
  } finally {
    clearTimeout(timeout);
  }
}
