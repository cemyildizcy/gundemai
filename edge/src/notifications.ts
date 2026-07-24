import { truncate } from "./text";
import type { Env } from "./types";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const TOPICS_BY_CATEGORY: Record<string, string> = {
  "Son Dakika": "news_son_dakika",
  "Yapay Zeka": "news_yapay_zeka",
  Teknoloji: "news_teknoloji",
  Turkiye: "news_turkiye",
  Dunya: "news_dunya",
  Ekonomi: "news_ekonomi",
  Finans: "news_finans",
  Kripto: "news_kripto",
  Spor: "news_spor",
  Transfer: "news_transfer",
  Bilim: "news_bilim",
  Oyun: "news_oyun",
  Girisimcilik: "news_girisimcilik",
  "Kultur ve Sanat": "news_kultur_sanat",
  Saglik: "news_saglik"
};

interface ServiceAccount {
  client_email: string;
  private_key: string;
  token_uri?: string;
}

interface NotificationRow {
  id: string;
  title: string;
  category: string;
  source_name: string;
}

const NOTIFICATION_MAX_AGE_MS = 2 * 60 * 60 * 1000;

export interface NotificationResult {
  attempted: number;
  sent: number;
  failed: number;
  skipped: boolean;
  errors: string[];
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function textBase64Url(value: string): string {
  return base64Url(new TextEncoder().encode(value));
}

function pemBytes(pem: string): ArrayBuffer {
  const binary = atob(pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, ""));
  return Uint8Array.from(binary, (character) => character.charCodeAt(0)).buffer as ArrayBuffer;
}

async function accessToken(serviceAccount: ServiceAccount): Promise<string> {
  const tokenUri = serviceAccount.token_uri ?? "https://oauth2.googleapis.com/token";
  const now = Math.floor(Date.now() / 1000);
  const header = textBase64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = textBase64Url(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: FCM_SCOPE,
    aud: tokenUri,
    iat: now,
    exp: now + 3600
  }));
  const unsigned = `${header}.${payload}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemBytes(serviceAccount.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  );
  const assertion = `${unsigned}.${base64Url(new Uint8Array(signature))}`;
  const response = await fetch(tokenUri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });
  if (!response.ok) throw new Error(`Google OAuth HTTP ${response.status}: ${truncate(await response.text(), 300)}`);
  const result = await response.json<{ access_token?: string }>();
  if (!result.access_token) throw new Error("Google OAuth access token dondurmedi");
  return result.access_token;
}

async function rowFor(env: Env, now: number): Promise<NotificationRow | null> {
  await env.DB.prepare(`
    UPDATE news_items
    SET notification_sent_at = -1
    WHERE status = 'READY'
      AND notification_sent_at IS NULL
      AND ready_at < ?
  `).bind(now - NOTIFICATION_MAX_AGE_MS).run();

  const result = await env.DB.prepare(`
    SELECT
      n.id,
      n.title,
      n.category,
      COALESCE((SELECT s.name FROM news_sources s
        WHERE s.article_id = n.id ORDER BY s.published_at ASC LIMIT 1), 'GundemAI') AS source_name
    FROM news_items n
    WHERE n.status = 'READY'
      AND n.notification_sent_at IS NULL
      AND n.ready_at >= ?
    ORDER BY
      CASE WHEN n.category = 'Son Dakika' THEN 0 ELSE 1 END,
      n.ready_at DESC
    LIMIT 1
  `).bind(now - NOTIFICATION_MAX_AGE_MS).first<NotificationRow>();
  return result;
}

export async function sendNotifications(env: Env): Promise<NotificationResult> {
  if (!env.FIREBASE_SERVICE_ACCOUNT) {
    return { attempted: 0, sent: 0, failed: 0, skipped: true, errors: [] };
  }

  const row = await rowFor(env, Date.now());
  if (!row) {
    return { attempted: 0, sent: 0, failed: 0, skipped: false, errors: [] };
  }

  const serviceAccount = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT) as ServiceAccount;
  const token = await accessToken(serviceAccount);
  let sent = 0;
  const errors: string[] = [];

  const topic = TOPICS_BY_CATEGORY[row.category];
  if (!topic) {
    errors.push(`${row.id}: bildirim konusu bulunamadi`);
    await env.DB.prepare(
      "UPDATE news_items SET notification_sent_at = -1 WHERE id = ?"
    ).bind(row.id).run();
  } else {
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(env.FIREBASE_PROJECT_ID)}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          message: {
            topic,
            data: {
              article_id: row.id,
              title: truncate(row.title, 240),
              category: row.category,
              source_name: truncate(row.source_name, 120),
              is_breaking: String(row.category === "Son Dakika")
            },
            android: { priority: "HIGH", ttl: "300s" }
          }
        })
      }
    );
    if (!response.ok) {
      errors.push(`${row.id}: FCM HTTP ${response.status} ${truncate(await response.text(), 240)}`);
    } else {
      sent = 1;
      await env.DB.prepare(
        "UPDATE news_items SET notification_sent_at = ? WHERE id = ?"
      ).bind(Date.now(), row.id).run();
    }
  }

  return {
    attempted: 1,
    sent,
    failed: 1 - sent,
    skipped: false,
    errors
  };
}
