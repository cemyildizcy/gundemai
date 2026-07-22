# GündemAI Ücretsiz Kurulum

Bu kurulumda Firebase Spark planı değiştirilmez ve kredi kartı eklenmez.

## Hazır Olanlar

- Firebase proje kimliği: gundemai
- Android paket adı: com.gundemai.app
- GitHub deposu: https://github.com/cemyildizcy/gundemai
- GitHub Actions: Publish shared news feed (aktif, beş dakikada bir)
- Authentication: E-posta/şifre ve Google
- Firestore: Standard edition, production mode
- Uygulama haber adresi: https://gundemai.web.app/v1/news
- Gizlilik politikası: https://gundemai.web.app/privacy
- Hesap silme: https://gundemai.web.app/account-deletion

## Senin Yapacağın Adımlar

1. Firebase Console'da Project settings > Service accounts > Generate new private key yolundan yeni bir JSON anahtar indir.
2. GitHub deposunda Settings > Secrets and variables > Actions > New repository secret bölümünü aç.
3. FIREBASE_SERVICE_ACCOUNT_GUNDEMAI adlı secret oluştur ve indirdiğin JSON dosyasının bütün içeriğini değer olarak ekle.
4. Cloudflare hesabında Workers AI > Use REST API bölümünü aç; Create a Workers AI API Token ile token oluştur ve Account ID değerini kopyala.
5. GitHub'a CLOUDFLARE_ACCOUNT_ID ve CLOUDFLARE_API_TOKEN adlarında iki secret ekle.
6. OpenRouter anahtarını yalnızca yedek sağlayıcı olarak tut; zorunlu değildir.
7. GitHub'da Actions > Publish shared news feed > Run workflow düğmesine bas.
8. İşlem yeşil tamamlandığında https://gundemai.web.app/v1/news adresini aç. articles listesi görünüyorsa merkezi sistem çalışıyor.

## Ücretsiz Kota Koruması

Görev beş dakikada bir kaynakları kontrol eder. Her turda en fazla bir yeni haber AI'a gider ve bütün görevler için günlük başlangıç sınırı 50 haberdir. Cloudflare Workers AI ana sağlayıcıdır; OpenRouter yalnızca yedektir. Daha fazla haber varsa silinmez, sonraki uygun tura bırakılır. Aynı haber yeniden analiz edilmez.

GitHub ve Cloudflare kotaları zaman içinde değişebildiği için kullanım ekranlarını ara sıra kontrol et. Firebase Console'da Upgrade düğmesine basma; bu mimari Spark planda çalışmak üzere hazırlanmıştır.
