CREATE TABLE IF NOT EXISTS daily_briefs (
  date_key TEXT PRIMARY KEY,
  status TEXT NOT NULL DEFAULT 'RETRY'
    CHECK (status IN ('GENERATING', 'READY', 'RETRY')),
  title TEXT,
  summary TEXT,
  items_json TEXT NOT NULL DEFAULT '[]',
  model TEXT,
  generated_at INTEGER,
  generation_started_at INTEGER,
  next_attempt_at INTEGER NOT NULL DEFAULT 0,
  last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_daily_briefs_status
  ON daily_briefs(status, next_attempt_at);
