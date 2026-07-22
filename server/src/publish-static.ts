import { fileURLToPath } from "node:url";

import { createNewsRuntime } from "./runtime/create-news-runtime.js";
import { writeStaticSite } from "./static/write-static-site.js";

async function main(): Promise<void> {
  const { store, pipeline } = createNewsRuntime();
  const pipelineResult = await pipeline.run();
  const outputDirectory = process.env.HOSTING_PUBLIC_DIR ||
    fileURLToPath(new URL("../../hosting/public/", import.meta.url));
  const siteResult = await writeStaticSite({
    store,
    outputDirectory,
    supportEmail: required("SUPPORT_EMAIL")
  });
  console.log(JSON.stringify({ pipeline: pipelineResult, hosting: siteResult }));
}

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
}

main().catch((error: unknown) => {
  console.error(error);
  process.exitCode = 1;
});
