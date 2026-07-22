export interface RawArticle {
  id: string;
  title: string;
  description: string;
  content: string;
  categoryHint: string;
  imageUrl: string | null;
  url: string;
  sourceName: string;
  publishedAt: number;
}
