# GündemAI

GündemAI; haberleri merkezi olarak toplayan, ortak bir yapay zekâ analizinden
geçiren ve bütün kullanıcılara aynı sırada sunan Android haber uygulamasıdır.
Telefonda AI veya haber servisi anahtarı bulunmaz.

## Yapı

- `app/`: Kotlin, Jetpack Compose, Room, WorkManager, Firebase Auth ve FCM.
- `edge/`: Cloudflare Workflow, D1 kalıcı haber kuyruğu, Workers AI ve yayın API'si.
- `server/`: Eski Firebase tabanlı yayın hattı ve sunucu testleri; geçiş yedeği.
- `hosting/`: Gizlilik, hesap silme sayfaları ve geçiş sırasındaki eski ortak akış.
- `.github/workflows/deploy-edge.yml`: Kalıcı sunucuyu otomatik kurar ve günceller.

Kalıcı akış:

`3 dakikada bir tara -> D1 kuyruğuna yaz -> olayı/kategoriyi belirle -> AI ile analiz et -> kalite kontrolü -> yayınla -> kategori bildirimi gönder`

Haber, AI çağrısından önce D1'e yazılır. AI veya kaynak geçici olarak hata
verirse haber kaybolmaz; yeniden deneme kuyruğunda kalır. Mobil uygulama yalnız
`READY` durumundaki ortak analizleri gösterir.

## Ücretsiz çalışma düzeni

Her üç dakikalık çalışma beş toplama ve bir analiz adımından oluşur. Böylece
günde `480 x 6 = 2.880` Workflow adımı kullanılır ve Cloudflare Free plandaki
3.000 adımlık günlük sınırın altında kalır.

Workers AI ücretsiz günlük kotası sınırsız değildir. Sistem bu nedenle tur
başına en fazla iki haber analiz eder ve kota dolduğunda ham haberleri silmez.
Kota yenilendiğinde kuyruk otomatik devam eder. Ücretsiz bir üçüncü taraf
serviste sınırsız AI garantisi verilemez; bu mimari ücretsiz sınır dolduğunda
veri kaybetmemeyi garanti eder.

Kurulum ve izinler için [UCRETSIZ_KURULUM.md](UCRETSIZ_KURULUM.md), sunucunun
teknik ayrıntıları için [edge/README.md](edge/README.md) dosyasına bakın.

## Android geliştirme

Firebase Android yapılandırmasını `app/google-services.json` olarak ekleyin.
Bu dosya ve yerel `.env` güvenlik nedeniyle GitHub'a yüklenmez.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## Release

Google Play AAB dosyası için upload keystore ile `KEYSTORE_PATH`,
`STORE_PASSWORD`, `KEY_ALIAS` ve `KEY_PASSWORD` tanımlanmalıdır. Reklam
gösterilecekse Google test kimlikleri yerine onaylı AdMob uygulama ve banner
kimlikleri kullanılmalıdır.

Gizlilik politikası: https://gundemai.web.app/privacy

Hesap silme: https://gundemai.web.app/account-deletion
