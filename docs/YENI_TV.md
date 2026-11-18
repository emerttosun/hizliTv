# HızlıTV'yi başka bir Android TV'de çalıştırmak

[English](en/PORTING.md) · **Türkçe**

HızlıTV, yerel (native) kütüphanesi olmayan, düz Java ile yazılmış tek bir APK'dır. Bu yüzden aynı derleme Android 6 (API 23) ve sonrasındaki ARM ve x86 Android TV / Google TV cihazlarda çalışır. Modele göre değişen değerler koda yazılmaz, çalışırken cihazdan okunur. Bu sayfa neyin kendiliğinden uyum sağladığını, yeni bir TV'de neyin kontrol edileceğini ve bir model farklı bir şey isterse hangi birkaç yerin değişeceğini anlatır.

Gerçek cihazda yalnızca Mi TV Stick (Android 11, 1080p) denendi. Diğer modellerle ilgili aşağıdakiler bir uyumluluk iddiası değil, kontrol listesidir.

## Kendiliğinden uyum sağlayanlar

| Alan | Nasıl uyum sağlar | Kod |
| --- | --- | --- |
| Video çözünürlüğü | En az yaklaşık 1,8 GB RAM'i olan 4K panelde 2160p, diğerlerinde 1080p. Panel boyutu ekranın desteklediği modlardan okunur; menülerini 1080p çalıştıran bir 4K TV de 4K video alır | `DeviceProfile.maxVideoSize` |
| Oynatma tamponu | Video 1080p ile sınırlıyken 24 MB (test edilen değer); 4K oynatmada 48 MB, 3 GB RAM'den itibaren 96 MB | `DeviceProfile.bufferBytes` |
| Yerleşim | Ölçüler dp cinsindendir; TV hem 1080p hem 4K'da yaklaşık 960×540 dp bildirir, kartlar ve yazılar oranını korur | `HomeScreen`, `LibraryScreen` |
| Menü tuşu olmayan kumanda | Google TV kumandalarında çoğu zaman Menü tuşu yoktur. Oynatıcıda seçenekler için **OK'e basılı tut** (varsa Menü/Info da çalışır). Ana ekranda tüm uygulamalar için aşağı in | `PlayerActivity.dispatchKeyEvent` |
| Orijinal başlatıcı | HOME ve Eski ana ekran simgesi cihazın kendi başlatıcısını bulur (Android TV veya Google TV) | `AppTargets.stockHome` |
| AirPlay simgesi | Desteklenen bir alıcı kurulu değilse gizlenir | `AppTargets.AIRPLAY` |
| Telefon alıcı portu | Kayıtlı port korunur (varsayılan 8198). TV'de başka bir uygulama o portu kullanıyorsa boş bir port seçilir ve QR kod yenisini gösterir | `PhoneCompanion.start` |
| RAM'i rahatlat | Android 14 ve sonrasında Android izin vermediği için açıklamayla kapatılır | `MemoryController.relax` |
| Dil | Cihaz dili Türkçe/İngilizce ise onu izler, diğerlerinde İngilizce | `tv-ui` kaynakları |

Her açılışta cihazı anlatan tek satır yazılır; TV'leri karşılaştırırken işe yarar:

```sh
adb logcat -s HizliTV.Device
```

Model, Android sürümü, panel boyutu, RAM ve seçilen video sınırı ile tampon görünür. Kullanıcı verisi içermez.

## Yeni bir TV'ye kurulum

1. **Ayarlar → Sistem → Hakkında** (bazı TV'lerde **Cihaz Tercihleri → Hakkında**) bölümünde **Android TV OS derlemesi** satırına yedi kez bas; geliştirici seçenekleri açılır.
2. **Geliştirici seçenekleri**nde **USB hata ayıklama**yı aç. TV'nin IP adresini ağ ayarlarından not al.
3. Aynı ağdaki bilgisayardan:

   ```sh
   adb connect TV_IP:5555
   ```

   TV'de çıkan izni onayla. Bağlantı reddedilirse TV bunun yerine **Kablosuz hata ayıklama** kullanıyor olabilir: Geliştirici seçeneklerinden aç, kodla eşleştirmeyi seç, TV'nin gösterdiği değerlerle `adb pair TV_IP:EŞLEŞTİRME_PORTU` ve ardından `adb connect TV_IP:PORT` çalıştır. Menü adları üreticiye göre değişir.
4. Kur ve aç:

   ```sh
   adb -s TV_IP:5555 install -r launcher/build/outputs/apk/release/launcher-release.apk
   adb -s TV_IP:5555 shell am start -n tr.hizlitv.home/.MainActivity
   ```

Yeni TV'de önceden HızlıTV olmadığı için her imza kurulur. Sonradan kütüphaneyi kaybetmeden güncellemek için hep aynı imza anahtarını kullan. Sürümleri kendi `local-signing.properties` dosyanla derle (bkz. [yayınlama](YAYINLAMA.md)). Farklı makinelerde veya CI'da derlenen debug APK'lar birbirini güncelleyemeyebilir.

## Yeni model için kontrol listesi

| Kontrol | Nasıl | Olmazsa |
| --- | --- | --- |
| Cihaz özeti | `adb logcat -s HizliTV.Device` | Panel veya RAM yanlış okunuyordur: `DeviceProfile` kurallarını düzelt |
| Ana ekran sığıyor, odak görünüyor | Simgeler, favoriler ve tüm uygulamalar arasında gez | Ölçüler `HomeScreen` içinde |
| Kumanda tuşları | Oynatıcıda OK, uzun OK, ◀ ▶, ▲ ▼, CH±, Geri, Oynat/Duraklat | Kumandanın tuş kodunu `PlayerActivity.dispatchKeyEvent` içine ekle |
| 4K / HDR oynatma | 4K bir örnek oynat, sonra uzun OK → Kalite | 4K takılıyorsa `DeviceProfile.maxVideoSize` sınırını düşür |
| Çok kanallı ses | AC-3/E-AC-3 içeriği TV hoparlöründen ve HDMI ARC/eARC üzerinden dinle | Media3, TV'nin bildirdiğine göre sesi doğrudan aktarır veya çözer; TV'nin ses çıkışı ayarına bak |
| Telefondan gönderme | Aynı Wi-Fi'daki telefonla kütüphane QR'ını okut | Bazı modemler cihazları birbirinden yalıtır ("AP isolation"); yerel trafiğe izin ver |
| Bellek ekranı | Bellek'i aç | Süreç listesi isteğe bağlı izinler ister; RAM'i rahatlat Android 14+'ta yoktur; seçili uygulamaları kapatmak 5555 portunda ağ ADB'si ister, birçok Google TV bunu sunmaz |
| AirPlay alıcısı | Yardımcı uygulamayı kur, simgeye bak | `adb shell getprop ro.product.cpu.abilist` çıktısına uyan sürümü kur (bazı TV'ler 64 bit çipte 32 bit Android çalıştırır) |

## Bir model için nereler değişir

Çoğu model kod değişikliği istemez. İsterse değişiklik birkaç yerde kalır:

- **Video sınırı ve tampon:** `tv-ui/src/main/java/tr/hizlitv/ui/DeviceProfile.java`; unit testleri `tv-ui/src/test` içinde.
- **Oynatıcı seçenekleri** (kare hızı veya ses gibi modele özel bir seçenek eklemek): `player/src/main/java/tr/hizlitv/player/options` içinde yeni bir `PlayerOption`, `PlayerOptions.defaults()` içine kaydedilir.
- **Kumanda tuş eşlemeleri:** `player/src/main/java/tr/hizlitv/player/PlayerActivity.java` (`dispatchKeyEvent`).
- **Hazır favoriler, AirPlay paketleri, orijinal başlatıcı:** `launcher/src/main/java/tr/hizlitv/home/AppTargets.java`.
- **Bellek ekranının asla kapatmayacağı uygulamalar** (başlatıcılar, ayarlar): `LocalAdb.PROTECTED` ve `AppManagerActivity.protectedPackage`. TV üreticisinin başlatıcısını veya sistem uygulamalarını buraya ekle.
- **Paket görünürlüğü:** yeni başlatıcı veya alıcı paket adlarını `launcher/src/main/AndroidManifest.xml` içindeki `<queries>` bölümüne ekle.

İngilizce ve Türkçe metinleri eşit tut (`python3 scripts/check_translations.py`) ve kurmadan önce [test](TEST.md) sayfasındaki kontrolleri çalıştır.
