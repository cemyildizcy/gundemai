# GündemAI Ücretsiz Kurulum

Bu kurulumda Firebase Spark planı değiştirilmez ve kredi kartı eklenmez.

## Hazır Olanlar

- Firebase proje kimliği: gundemai
- Android paket adı: com.gundemai.app
- Authentication: E-posta/şifre ve Google
- Firestore: Standard edition, production mode
- Uygulama haber adresi: https://gundemai.web.app/v1/news
- Gizlilik politikası: https://gundemai.web.app/privacy
- Hesap silme: https://gundemai.web.app/account-deletion

## Senin Yapacağın Adımlar

1. GitHub'da ücretsiz ve public bir depo oluştur. GitHub'ın standart bulut çalıştırıcıları public depolarda ücretsizdir; beş dakikalık sıklık private deponun aylık ücretsiz dakikasını aşar.
2. Bu proje klasörünü depoya yükle. .env dosyasını veya herhangi bir API anahtarını yükleme; .gitignore bunu zaten engelliyor.
3. Firebase Console'da Project settings > Service accounts > Generate new private key yolundan yeni bir JSON anahtar indir.
4. GitHub deposunda Settings > Secrets and variables > Actions > New repository secret bölümünü aç.
5. FIREBASE_SERVICE_ACCOUNT_GUNDEMAI adlı secret oluştur ve indirdiğin JSON dosyasının bütün içeriğini değer olarak ekle.
6. OpenRouter Keys ekranında eski uygulamaya yazılmış anahtarı sil ve yeni bir anahtar oluştur.
7. GitHub'a OPENROUTER_API_KEY adlı ikinci secret ekle.
8. GitHub'da Actions > Publish shared news feed > Run workflow düğmesine bas.
9. İşlem yeşil tamamlandığında https://gundemai.web.app/v1/news adresini aç. articles listesi görünüyorsa merkezi sistem çalışıyor.

## Ücretsiz Kota Koruması

Görev beş dakikada bir kaynakları kontrol eder. Her turda en fazla bir yeni haber AI'a gider ve bütün görevler için günlük başlangıç sınırı 20 haberdir. Daha fazla haber varsa silinmez; sonraki uygun tura bırakılır. Aynı haber yeniden analiz edilmez. AI ücretsiz kotası biterse mevcut yayın çalışmaya devam eder, yeni analizler sonraki uygun turda eklenir.

GitHub ve OpenRouter kotaları zaman içinde değişebildiği için kullanım ekranlarını ara sıra kontrol et. Firebase Console'da Upgrade düğmesine basma; bu mimari Spark planda çalışmak üzere hazırlanmıştır.
