# İki APK’dan tek APK’ya geçiş

[English](en/MIGRATION.md) · **Türkçe**

Yeni kurulumda yalnız **HızlıTV** APK’sını kur. Player bir Android kütüphane modülüdür ve aynı uygulamanın içinde açılır. Aşağıdaki araç yalnız eski `tr.hizlitv.home` + `tr.hizlitv.player` kurulumu olan cihazlar içindir.

## Neden özel aktarım gerekiyor?

Android her uygulamanın kayıtlarını ayrı bir özel alanda tutar. Paket kimliği farklı bir uygulama, aynı geliştiricinin olsa bile bu dosyaları doğrudan okuyamaz. Eski Player’ı kaldırıp baştan başlamak kütüphaneyi ve telefon eşleştirmesini kaybettirir.

`legacy-bridge`, eski Player paketinin **aynı imzalı bir güncellemesidir**. Yeni başlatıcı simgesi veya ağ servisi sunmaz. Yalnız aynı imza sertifikasına sahip yeni HızlıTV’ye sınırlı bir içerik sağlayıcısıyla kayıt akışı verir. Farklı imza ve ADB shell çağrıları reddedilir. Özel imza anahtarı kaynak paketine eklenmez.

## Geçiş sırası

1. Önce mevcut APK’ları ve kaynakları kendi özel yedek alanında sakla. İmzayı değiştirme ve uygulama verilerini temizleme.
2. Her iki paketi de eski kurulumun anahtarıyla derle:

   ```sh
   ./gradlew -PincludeLegacyBridge=true :legacy-bridge:assembleRelease :launcher:assembleRelease
   ```

3. Önce `legacy-bridge/build/outputs/apk/release/legacy-bridge-release.apk`, ardından tek HızlıTV APK’sını `adb install -r` ile güncelle. Bu işlem eski Player arayüzünü aktarım köprüsüyle değiştirir; kayıt alanını silmez.
4. HızlıTV içindeki Player kartını aç. Aktarım ilk kütüphane yüklemesinde arka planda yapılır. Başarılı bitene kadar telefon alıcısı başlatılmaz ve yeni eşleştirme üretilmez.
5. Kütüphane, favoriler, geri alma kaydı, izleme konumları ve eski QR/Kestirme adresinin çalıştığını doğrula. Logcat’te `HizliTV.Migration` yalnız kayıt sayıları ve başarı bilgisini bildirir; tam URL veya eşleştirme anahtarı yazmaz.

Eski Player kurulu ama uygun köprü yoksa uygulama kayıtları sessizce atlayıp boş kütüphane oluşturmaz; geçiş gerektiğini bildirir. İki uygulamada da ayrı yeni kütüphaneler varsa otomatik üzerine yazmaz. Yeni bir imzayla eski kayıtlara erişmeye çalışmak desteklenmez.

## Yedek ve geri dönüş

Yeni uygulamanın özel `files/legacy-player-backup.zip` dosyası, aktarım öncesindeki değiştirilmemiş arşivi tutar. Sabit dosya adları, boyut sınırları, JSON şeması ve son tamamlanma kaydı doğrulanmadan içe aktarma yapılmaz. `player-migration-result.json`, özel alanda aktarım özetini saklar. Geçici dosyalar başarılı aktarım sonunda temizlenir.

Köprüyü hemen kaldırmak gerekmez: kullanıcı yeni sürümü doğrulayana kadar eski özel veri alanını korumak, geri dönüşü kolaylaştırır. Görünen eski Player simgesi yoktur; günlük kullanım tek HızlıTV APK’sındadır. Aktarım bitince eski köprünün süreci durdurulabilir; özel veriler korunur. Yeni kurulumlar köprüyü hiç kurmaz. Köprü kaldırılacaksa yeni özel arşivin ve aktarım sonucunun doğrulandığından emin ol; eski paketi kaldırmak onun özel veri alanını siler.

Köprü hâlâ kuruluysa eski Player kaynaklarını aynı imzayla, **100’den yüksek versionCode** ile derleyip `install -r` yapmak eski özel verileri tekrar kullanır. Yeni uygulamadaki değişiklikler eski pakete kendiliğinden geri aktarılmaz. Köprü kaldırıldıysa eski APK’yı kurmak arşivi otomatik geri yüklemez; aynı imzalı bakım sürümüyle yeni uygulamanın özel yedeğinden kontrollü geri yükleme gerekir. Yeni HızlıTV’yi kaldırma veya `pm clear` yapma: özel yedek de silinir.

Ürün dağıtımında tek APK paylaşılır. Köprü APK’sı ve cihazdan alınan özel arşiv, normal release paketinin veya kaynak deposunun parçası değildir.
