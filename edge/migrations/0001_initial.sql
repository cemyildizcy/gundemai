PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS news_items (
  id TEXT PRIMARY KEY,
  event_key TEXT NOT NULL UNIQUE,
  raw_title TEXT NOT NULL,
  raw_description TEXT NOT NULL,
  category_hint TEXT NOT NULL,
  image_url TEXT,
  published_at INTEGER NOT NULL,
  discovered_at INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'RETRY')),
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at INTEGER NOT NULL DEFAULT 0,
  processing_started_at INTEGER,
  last_error TEXT,
  title TEXT,
  summary TEXT,
  category TEXT,
  what_happened TEXT,
  why_important TEXT,
  missing_information TEXT,
  verification_status TEXT,
  confidence_score INTEGER,
  possible_impacts TEXT NOT NULL DEFAULT '[]',
  unverified_claims TEXT NOT NULL DEFAULT '[]',
  contradictions TEXT NOT NULL DEFAULT '[]',
  verified_facts TEXT NOT NULL DEFAULT '[]',
  ready_at INTEGER,
  notification_sent_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_news_items_queue
  ON news_items(status, next_attempt_at, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_news_items_ready
  ON news_items(status, ready_at DESC);

CREATE TABLE IF NOT EXISTS news_sources (
  article_id TEXT NOT NULL,
  url TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  headline TEXT NOT NULL,
  published_at INTEGER NOT NULL,
  PRIMARY KEY (article_id, url),
  FOREIGN KEY (article_id) REFERENCES news_items(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_news_sources_article
  ON news_sources(article_id, published_at);

CREATE TABLE IF NOT EXISTS pipeline_state (
  singleton INTEGER PRIMARY KEY CHECK (singleton = 1),
  last_run_at INTEGER,
  last_collected_count INTEGER NOT NULL DEFAULT 0,
  last_published_count INTEGER NOT NULL DEFAULT 0,
  last_notification_count INTEGER NOT NULL DEFAULT 0,
  last_error TEXT
);

INSERT OR IGNORE INTO pipeline_state(singleton) VALUES (1);
