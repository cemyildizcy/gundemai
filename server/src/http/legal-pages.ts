export function privacyPage(supportEmail: string): string {
  const email = escapeHtml(supportEmail);
  return legalPage(
    "GündemAI Gizlilik Politikası",
    `<h1>GündemAI Gizlilik Politikası</h1>
    <p>Son güncelleme: 22 Temmuz 2026</p>
    <h2>İşlenen bilgiler</h2>
    <p>Hesap açmayı seçerseniz Firebase Authentication e-posta adresinizi, görünen adınızı ve kimlik doğrulama bilgilerinizi işler. Misafir kullanımında GündemAI hesabı oluşturulmaz.</p>
    <p>Uygulama; tercihleri, yer imlerini, arama geçmişini ve bildirimleri cihazınızda saklar. Haber akışı isteği merkezi GündemAI servisine gönderilir; kişisel haber profili veya kişiye özel yapay zeka analizi oluşturulmaz.</p>
    <h2>Üçüncü taraf hizmetler</h2>
    <p>Firebase Authentication hesap girişini, Google Play Billing abonelik satın alımlarını işler. Reklamlar etkinleştirildiğinde Google AdMob ve User Messaging Platform reklam gösterimi, izin ve ölçüm için cihaz ya da reklam tanımlayıcılarını işleyebilir. Bu hizmetlerin işlediği veriler kendi politikalarına tabidir.</p>
    <h2>Amaç ve saklama</h2>
    <p>Bilgiler giriş sağlamak, tercihleri hatırlamak, abonelik durumunu göstermek, güvenliği korumak ve uygulamayı çalıştırmak için kullanılır. Hesabınızı sildiğinizde Firebase hesabınız ve cihazdaki kişisel uygulama verileriniz silinir. Yasal olarak saklanması gereken Google Play ödeme kayıtları Google tarafından yönetilir.</p>
    <h2>Hesap silme</h2>
    <p>Uygulamada Profil &gt; Hesabı ve verilerimi sil yolunu kullanabilirsiniz. Uygulamaya erişemiyorsanız <a href="/account-deletion">hesap silme talebi sayfasını</a> kullanın.</p>
    <h2>İletişim</h2>
    <p>Gizlilik soruları için <a href="mailto:${email}">${email}</a> adresine yazabilirsiniz.</p>`
  );
}

export function accountDeletionPage(supportEmail: string): string {
  const email = escapeHtml(supportEmail);
  const subject = encodeURIComponent("GündemAI hesap silme talebi");
  return legalPage(
    "GündemAI Hesap Silme",
    `<h1>GündemAI hesap silme talebi</h1>
    <p>Uygulamaya erişebiliyorsanız en hızlı yol Profil &gt; Hesabı ve verilerimi sil seçeneğidir.</p>
    <p>Uygulamaya erişemiyorsanız, hesabınızda kullandığınız e-posta adresinden aşağıdaki adrese "GündemAI hesap silme talebi" konusuyla yazın. Kimliğiniz doğrulandıktan sonra Firebase hesabınız ve GündemAI ile ilişkili kişisel verileriniz silinir.</p>
    <p><a class="button" href="mailto:${email}?subject=${subject}">Silme talebi gönder</a></p>
    <p>İletişim: <a href="mailto:${email}">${email}</a></p>
    <h2>Abonelik hakkında</h2>
    <p>GündemAI hesabını silmek Google Play aboneliğini otomatik olarak iptal etmez. Aktif aboneliğinizi Google Play &gt; Ödemeler ve abonelikler bölümünden ayrıca iptal edin.</p>
    <p><a href="/privacy">Gizlilik politikasını görüntüle</a></p>`
  );
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#39;"
  })[character] ?? character);
}

function legalPage(title: string, body: string): string {
  return `<!doctype html><html lang="tr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHtml(title)}</title><style>body{margin:0;background:#f8fafc;color:#172033;font:16px/1.65 system-ui,sans-serif}main{max-width:760px;margin:auto;padding:40px 20px 64px}h1{font-size:32px;line-height:1.2}h2{margin-top:32px;font-size:20px}a{color:#155eef}.button{display:inline-block;background:#155eef;color:#fff;padding:12px 16px;border-radius:6px;text-decoration:none;font-weight:700}</style></head><body><main>${body}</main></body></html>`;
}
