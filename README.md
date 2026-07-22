# GündemAI

GündemAI, haberleri merkezi olarak toplayan ve yapay zeka analizinden geçirdikten sonra bütün kullanıcılara aynı sırada sunan Android haber uygulamasıdır. Telefon üzerinde AI anahtarı veya haber API anahtarı bulunmaz.

## Yapı

- app/: Kotlin, Jetpack Compose, Room, WorkManager ve Firebase Authentication.
- server/: Haber toplama, kümeleme, kategori belirleme, AI analizi ve kalite denetimi.
- hosting/: Mobil uygulamanın okuduğu ortak haber JSON'u ile gizlilik ve hesap silme sayfaları.
- .github/workflows/publish-news.yml: Beş dakikada bir çalışan ücretsiz yayın görevi.

Akış: kaynakları topla -> aynı olayı kümele -> kategoriyi belirle -> AI analizi üret -> kalite denetiminden geçir -> Firestore'a kaydet -> Firebase Hosting'e ortak akış olarak yayınla.

## Ücretsiz Kurulum

Firebase projesi Spark planda kalabilir. Cloud Run, Cloud Functions veya ücretli zamanlayıcı kullanılmaz. Otomatik görev public GitHub deposunda beş dakikada bir çalışır; analizde ücretsiz Cloudflare Workers AI'ı, gerektiğinde OpenRouter yedeğini kullanır ve sonucu https://gundemai.web.app/v1/news adresinde yayınlar.

Adım adım kullanıcı rehberi için UCRETSIZ_KURULUM.md dosyasını açın.

## Android Geliştirme

Firebase Android yapılandırmasını Firebase Console'dan indirip `app/google-services.json` olarak ekleyin. Bu dosya ve yerel `.env` değerleri güvenlik amacıyla GitHub'a yüklenmez.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## Release

Google Play AAB dosyası için bir upload keystore oluşturulmalı ve KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD değerleri tanımlanmalıdır. Reklam gösterilecekse ayrıca gerçek AdMob uygulama ve banner kimlikleri gerekir; test reklam kimlikleriyle mağaza yayını yapılmamalıdır.

Gizlilik politikası https://gundemai.web.app/privacy, hesap silme sayfası https://gundemai.web.app/account-deletion adresindedir.
