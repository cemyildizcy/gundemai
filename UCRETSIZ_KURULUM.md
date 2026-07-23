# GündemAI ücretsiz ve kalıcı sunucu kurulumu

Kurulum GitHub Actions tarafından otomatik yapılır. D1 veritabanını, tabloları,
Worker'ı, üç dakikalık Workflow'u ve ilk haber çalışmasını elle oluşturmanız
gerekmez.

## GitHub secrets

Depoda `Settings > Secrets and variables > Actions` bölümünde şunlar bulunmalı:

- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_API_TOKEN`
- `FIREBASE_SERVICE_ACCOUNT_GUNDEMAI`
- `GUNDEMAI_ADMIN_TOKEN` (yalnız elle çalıştırma API'si istenirse, isteğe bağlı)

Cloudflare API token hesabınızda şu izinlere sahip olmalı:

- Workers Scripts: Edit
- Workflows: Edit
- D1: Edit
- Workers AI: Read

Kaynak kapsamı yalnız kendi Cloudflare hesabınız olarak seçilebilir. Zone izni
gerekmez.

## İlk kurulum

1. GitHub'da `Actions` bölümünü açın.
2. `Deploy permanent news pipeline` görevini seçin.
3. `Run workflow` düğmesine basın.
4. Görev tamamlanınca özet bölümündeki `gundemai-edge...workers.dev` adresini
   alın.
5. Adresin sonuna `/health.json` ekleyin. `ok: true` ve
   `schedule: "*/3 * * * *"` görünmelidir.

Sonraki kod değişikliklerinde dağıtım otomatik yapılır. D1 geçişleri de aynı
görev tarafından uygulanır.

## Ücretsiz plan gerçeği

- Workflow her üç dakikada bir çalışır.
- D1 kalıcı kuyruktur; AI hatasında haber silinmez.
- Hazır haberler 90 gün tutulur.
- Tur başına en fazla iki AI analizi yapılır.
- Workers AI günlük ücretsiz kotası dolarsa analizler bir sonraki kota
  yenilenmesine kadar bekler.

Bu düzen sabit bir aylık ücret istemez. Cloudflare veya Firebase'in gelecekte
ücretsiz plan koşullarını değiştirmesi üçüncü taraf riski olarak kalır.
