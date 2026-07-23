# GundemAI Edge

GundemAI Edge is the permanent, queue-first news backend. Cloudflare starts one
durable Workflow every three minutes. Five collection steps scan the configured
RSS and Telegram sources; the sixth step analyzes queued articles, publishes
them through the shared API, and sends Firebase topic notifications.

## Reliability model

- Every newly discovered source URL is written to D1 before AI analysis starts.
- Source and AI failures affect only the relevant item, not the whole run.
- Failed AI items remain in `RETRY`; daily quota failures resume after the
  Workers AI free allocation resets.
- A timed-out `PROCESSING` item is automatically returned to the queue.
- Event fingerprints merge matching headlines from different sources.
- The API merges the old Firebase feed during migration, so the app does not
  start with an empty timeline.
- Ready items are retained for 90 days. Permanently failing items are retained
  for at least 30 days and 20 attempts before cleanup.

## Free-tier budget

The Workflow uses exactly six durable steps per scheduled run:

`480 runs/day x 6 steps = 2,880 steps/day`

This remains below the Cloudflare Workers Free allowance of 3,000 Workflow
steps per day. AI throughput is capped at two articles per run (up to 960 per
day) so the compact multilingual model normally stays within the daily free AI
allocation. If a provider quota is exhausted, D1 keeps the backlog and retries
it automatically; no free service can guarantee unlimited AI usage.

## Deployment

`.github/workflows/deploy-edge.yml` creates or reuses `gundemai-news`, applies
D1 migrations, deploys the Worker and scheduled Workflow, installs the Firebase
service-account secret, and starts the first run.

The `CLOUDFLARE_API_TOKEN` repository secret needs these account permissions:

- Workers Scripts: Edit
- Workflows: Edit
- D1: Edit
- Workers AI: Read

`CLOUDFLARE_ACCOUNT_ID` and `FIREBASE_SERVICE_ACCOUNT_GUNDEMAI` are also
required. `GUNDEMAI_ADMIN_TOKEN` is optional and enables `POST /admin/run`.

## Endpoints

- `GET /v1/news`
- `GET /v1/news.json`
- `GET /health.json`
- `POST /admin/run` with an optional admin bearer token
