import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

import type { ReadyArticle } from "./domain/types.js";
import { sendPublishedNewsNotifications } from "./notifications/firebase-news-notifier.js";

async function main(): Promise<void> {
  const outboxPath = process.env.NOTIFICATION_OUTBOX_PATH ||
    fileURLToPath(new URL("../notification-outbox.json", import.meta.url));
  const articles = JSON.parse(await readFile(outboxPath, "utf8")) as ReadyArticle[];
  const result = await sendPublishedNewsNotifications(
    articles,
    process.env.GOOGLE_CLOUD_PROJECT || "gundemai"
  );
  console.log(JSON.stringify({ notifications: result }));
  if (result.failed > 0) throw new Error(`${result.failed} Firebase notification(s) failed`);
}

main().catch((error: unknown) => {
  console.error(error);
  process.exitCode = 1;
});
