# İzinler, yerel veri ve güvenlik sınırları

## Temel kullanım

Tek APK’nın manifesti, Player’ın `INTERNET` ve `ACCESS_NETWORK_STATE` normal izinlerini ve aşağıdaki isteğe bağlı cihaz araçlarının izinlerini içerir. Player temel kullanımında tanılama/ADB yetkisi gerekmez. Kamera, mikrofon, kişiler veya ortak depolama izni istenmez. QR kodunu TV üretir; telefonun kamerası okur.

Kütüphane, oynatma konumu, telefon eşleştirmesi ve Player işlem günlüğü Android uygulama alanında saklanır. URL'ler sağlayıcı bilgileri içerebilir; genel Logcat kayıtlarına tam URL, gövde veya eşleştirme anahtarı yazılmaz. Uygulamayı kaldırmak ya da verilerini temizlemek bu kayıtları siler. Kaynak deposunda gerçek kanal listesi, hesap, cihaz yedeği veya özel anahtar bulunmaz.

Telefon alıcısı aynı yerel ağ için tasarlanmıştır. Eşleştirme URL'si gizli bir yetkilendirme bilgisi taşır; POST isteğinde `X-HizliTV-Token` da gerekir. Sunucu yol, belirteç, köken, veri türü ve boyutu kontrol eder. CORS, komut çalıştırma, dosya servisi veya açık proxy sunmaz. Alıcı HTTP kullanır; yerel ağ trafiğini şifrelemez. Modemden bu porta internet erişimi açma. **Eşleştirmeyi unut** mevcut URL'yi geçersiz kılar.

Güvenlik kontrolleri kaynak çözümleme başarısı garantisi değildir. DRM, oturum açma veya CAPTCHA aşma özelliği yoktur. Site bağdaştırıcıları yalnız tanımlı açık veri biçimlerini okur; JavaScript çalıştırmaz.

## Launcher'ın isteğe bağlı cihaz araçları

| İzin / özellik | Ne için var? | Sınır |
| --- | --- | --- |
| `KILL_BACKGROUND_PROCESSES` | Kullanıcının başlattığı arka plan bellek isteği | Sistem izin verdiği ölçüde çalışır; Android uygulamayı tekrar açabilir. Android 14+ için diğer uygulamalara temizlik isteği gönderilmez. |
| `DUMP` | Süreç tanılama raporunu okumak | Normal kurulumda otomatik verilmez; cihaz sahibi geliştirici araçlarıyla açıkça verir. Sistem servisleri hakkında daha geniş tanılama bilgisi sağlayabilir. |
| `PACKAGE_USAGE_STATS` | Bazı cihazların süreç/uygulama raporu için istediği ek okuma yetkisi | Normal kurulumda otomatik verilmez; Android/üretici uygulaması değişebilir. |
| `INTERNET` | İsteğe bağlı cihaz içi ADB soketi | ADB istemcisi yalnız loopback adresine bağlanır; UI'dan seçilen uygulamaları yönetmek içindir. |

DUMP ve kullanım bilgisi olmadan temel ana ekran ve Player çalışır. RAM'in toplam/kullanılabilir miktarı için bu geniş izinler gerekli değildir; `ActivityManager.getMemoryInfo` kullanılır. Ayrıntılı süreç listesi erişilemezse uygulama bunu bildirir.

Bu tanılama izinlerini vermeyi **kendin seçtiğin** cihazda örnek komutlar:

```sh
adb shell pm grant tr.hizlitv.home android.permission.DUMP
adb shell pm grant tr.hizlitv.home android.permission.PACKAGE_USAGE_STATS
```

Bir üretici bu komutları reddedebilir; izin verilmiş varsayılmaz. Geri almak için aynı komutlarda `grant` yerine `revoke` kullan. Kullanım erişimi için Android'in Özel Uygulama Erişimi ekranı da üreticiye göre sunulabilir. Kullanıcı izni dışında bu yetkiler uygulama tarafından yükseltilmez.

**Yerel ADB**, bu okuma izinlerinden daha geniş yetkilidir. Uygulama ekranı bunu açıklar; kullanıcı bağlantıyı başlatır ve TV'nin hata ayıklama penceresini onaylar. Anahtar cihazda uygulamanın özel alanında tutulur; dışarı aktarılmaz. Kullanılan yol root gerektirmez. Bazı cihazlar her bağlantıda tekrar onay ister; kalıcı yetki garanti edilmez. Bağlantıyı unutma seçeneği ve Android'deki hata ayıklama yetkilerini iptal etme seçeneği ayrı ayrı kullanılabilir.

Zorla durdurma yalnız kullanıcının seçtiği, koruma listesinde olmayan paketlere uygulanır. Veriler/önbellek silinmez; fakat uygulamanın devam eden işi durabilir. Yeni kullanım için uygulamayı tekrar aç. Bu özelliği temel gezinmeyle karıştırma; Bellektekiler kısayolunda ayrı tutulur.

## Eski Player’dan geçiş

İsteğe bağlı `legacy-bridge` eski paketi aynı imzayla günceller. İçerik sağlayıcısı, veri okumadan önce çağıranın UID sertifikasını kendi sertifikasıyla karşılaştırır; farklı imza veya ADB shell erişimi reddedilir. Ağ/başlatıcı/komut arayüzü sunmaz. Yalnız sabit adlar ve boyut sınırlarıyla bir özel yedek akışı üretir. Yeni Player, arşivi doğrulayıp özel alana kopyalar; eşleştirme anahtarı loglanmaz. [Geçiş ve geri dönüş](docs/GECIS.md).

## Kaynak paylaşma ve hata bildirme

`python3 scripts/check_public.py`, kaynak ihracatının dosya listesini ve belirgin kişisel veri kalıplarını kontrol eder. Bu bir tam güvenlik denetimi değildir; gönderilecek dosyaları ayrıca gözden geçir. `local.properties`, `local-signing.properties`, anahtarlar, APK'lar ve derleme klasörleri Git'e eklenmez.

Hata bildirirken eşleştirme kodunu, sağlayıcı hesabını, yayın URL'sindeki kimlik bilgilerini ve özel anahtarları kaldır. Güvenlik açığı için hesap/anahtar paylaşmadan yeniden üretim adımlarını ve etkilenen sürümü bildir. Bir özel iletişim kanalı tanımlanmamışsa hassas ayrıntıları açık issue'ya koyma; depo sahibinden özel bildirim kanalı iste.
