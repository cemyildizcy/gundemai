import { createHttpApp } from "./http/create-http-app.js";
import { createNewsRuntime } from "./runtime/create-news-runtime.js";

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
}

const { store, pipeline } = createNewsRuntime();
const app = createHttpApp({
  store,
  cronSecret: required("CRON_SECRET"),
  supportEmail: required("SUPPORT_EMAIL"),
  runPipeline: () => pipeline.run()
});

const port = Number(process.env.PORT || 8080);
app.listen(port, () => console.log(`GundemAI server listening on ${port}`));
