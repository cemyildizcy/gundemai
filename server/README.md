# GündemAI Haber İşleme Servisi

Bu paket RSS, Telegram ve isteğe bağlı haber API'lerinden içerik toplar. Aynı olayı kümeler, kategoriyi haber metninden belirler, AI analizi üretir ve yalnızca kalite denetimini geçen haberleri news_ready koleksiyonuna yazar.

## Ücretsiz Çalışma

npm run publish:static komutu bir toplama turu çalıştırır ve hazır ortak akışı hosting/public/v1/news.json dosyasına yazar. GitHub Actions bu komutu beş dakikada bir çalıştırıp dosyaları Firebase Hosting'e gönderir. Sürekli çalışan sunucu gerekmez.

Gerekli gizli değerler:

- CLOUDFLARE_ACCOUNT_ID ve CLOUDFLARE_API_TOKEN: Ana ücretsiz AI sağlayıcısı olan Workers AI erişimi.
- OPENROUTER_API_KEY: İsteğe bağlı yedek sağlayıcı; eski uygulamaya gömülmüş anahtar kullanılmaz.
- FIREBASE_SERVICE_ACCOUNT_GUNDEMAI: GitHub secret olarak saklanan Firebase hizmet hesabı JSON'u.

Yapılandırma:

- GOOGLE_CLOUD_PROJECT: Firebase proje kimliği; bu proje için gundemai.
- CLOUDFLARE_MODELS: Önce Gemma 4 26B, ardından GLM 4.7 Flash çalışır.
- OPENROUTER_MODELS: Cloudflare cevap veremezse sırayla denenecek yedek ücretsiz modeller.
- MAX_AI_ARTICLES_PER_RUN: Bir turda AI'a gönderilecek en fazla yeni haber; ücretsiz kurulumda 1.
- MAX_AI_ARTICLES_PER_DAY: Firestore üzerinden bütün görevler için uygulanan günlük AI sınırı; Cloudflare ücretsiz kotasına göre başlangıç değeri 50.
- MAX_CANDIDATES_PER_RUN: Firestore ücretsiz okuma kotasını korumak için her turda kontrol edilen en yeni olay sayısı; başlangıç değeri 25.
- SUPPORT_EMAIL: Yasal sayfalarda gösterilen iletişim adresi.
- GNEWS_API_KEY, NEWS_API_KEY: İsteğe bağlıdır; RSS ve Telegram için gerekmez.

## Komutlar

```powershell
npm.cmd ci
npm.cmd test
npm.cmd run build
npm.cmd run publish:static
```

Eski Cloud Run HTTP seçeneği npm start ile hâlâ kullanılabilir. Ücretsiz kurulum bu seçeneği kullanmaz.

Mobil uygulama Firestore'a doğrudan bağlanmaz. Firestore kuralları istemci erişimini kapalı tutar; GitHub görevi hizmet hesabıyla erişir.
