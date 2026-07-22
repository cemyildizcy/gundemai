import { GoogleAuth } from "google-auth-library";

import type { ReadyArticle } from "../domain/types.js";

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

export interface FcmTopicMessage {
  topic: string;
  data: Record<string, string>;
  android: {
    priority: "HIGH";
    ttl: string;
  };
}

export interface NotificationPublishResult {
  attempted: number;
  sent: number;
  failed: number;
}

export function topicForCategory(category: string): string | undefined {
  return TOPICS_BY_CATEGORY[category];
}

export function buildNewsTopicMessage(article: ReadyArticle): FcmTopicMessage | undefined {
  const topic = topicForCategory(article.category);
  if (!topic) return undefined;
  return {
    topic,
    data: {
      article_id: article.id,
      title: article.title.slice(0, 240),
      category: article.category,
      source_name: article.sourceName.slice(0, 120),
      is_breaking: String(article.category === "Son Dakika")
    },
    android: {
      priority: "HIGH",
      ttl: "300s"
    }
  };
}

export async function sendPublishedNewsNotifications(
  articles: ReadyArticle[],
  projectId: string,
  request: typeof fetch = fetch
): Promise<NotificationPublishResult> {
  const messages = articles
    .map(buildNewsTopicMessage)
    .filter((message): message is FcmTopicMessage => Boolean(message));
  if (messages.length === 0) return { attempted: 0, sent: 0, failed: 0 };

  const client = await new GoogleAuth({ scopes: [FCM_SCOPE] }).getClient();
  const accessTokenResult = await client.getAccessToken();
  const accessToken = typeof accessTokenResult === "string" ? accessTokenResult : accessTokenResult.token;
  if (!accessToken) throw new Error("Google service account did not provide an FCM access token");

  let sent = 0;
  for (const message of messages) {
    const response = await request(
      `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ message })
      }
    );
    if (!response.ok) {
      const detail = (await response.text()).slice(0, 500);
      throw new Error(`FCM HTTP ${response.status}: ${detail}`);
    }
    sent += 1;
  }
  return { attempted: messages.length, sent, failed: messages.length - sent };
}
