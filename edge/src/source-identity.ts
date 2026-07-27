import { fold } from "./text";

interface SourceIdentityInput {
  name: string;
  url: string;
}

function normalizedHost(url: URL): string {
  return url.hostname
    .toLowerCase()
    .replace(/\.$/, "")
    .replace(/^www\./, "");
}

function telegramChannel(url: URL): string | null {
  const segments = url.pathname.split("/").filter(Boolean);
  const channel = segments[0] === "s" ? segments[1] : segments[0];
  return channel?.toLowerCase() ?? null;
}

export function sourceIdentity(source: SourceIdentityInput): string {
  try {
    const url = new URL(source.url);
    const host = normalizedHost(url);
    if (host === "t.me" || host === "telegram.me") {
      const channel = telegramChannel(url);
      return channel ? `telegram:${channel}` : `name:${fold(source.name)}`;
    }
    return host ? `host:${host}` : `name:${fold(source.name)}`;
  } catch {
    return `name:${fold(source.name)}`;
  }
}

export function distinctSourceCount(sources: SourceIdentityInput[]): number {
  return new Set(sources.map(sourceIdentity)).size;
}
