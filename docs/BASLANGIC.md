# Sıfırdan HızlıTV geliştirmek

Bu rehber TV uygulaması geliştirmemiş biri içindir. İlk hedef, kaynak koddan bir APK üretip kendi TV'nde açmaktır. Root veya TV yazılımını değiştirme işlemi gerekmez.

## 1. Kelimeler ne demek?

| Terim | Bu projedeki anlamı |
| --- | --- |
| Kaynak kod | Ekranın ve davranışların yazıldığı okunabilir Java dosyaları |
| APK | Android cihazına kurulan uygulama paketi |
| JDK | Java kodunu derlemek için gereken araçlar; burada sürüm 17 |
| Android SDK | Android sınıfları, derleyiciler ve ADB gibi geliştirme araçları |
| Gradle | Kodları, kaynakları ve bağımlılıkları bir araya getirip APK üreten araç |
| Gradle Wrapper | Projenin seçtiği Gradle sürümünü indiren `gradlew` dosyaları |
| Activity | Android'de ekrana sahip bir uygulama bileşeni |
| View | Buton, yazı, liste veya resim gibi ekrana yerleşen bir bileşen |
| DPAD / odak | Kumandanın yön tuşlarıyla hangi öğenin seçili olduğunu yönetme |
| Manifest | Uygulamanın ekranlarını, izinlerini ve TV desteğini ilan eden XML dosyası |
| İmza | APK'nın sahibini belirleyen kimlik; aynı paketi veri silmeden güncellemek için aynı anahtar gerekir |

`minSdk 23`, Android 6'dan aşağıda kurulamayacağını belirtir. `compileSdk 35`, derleme sırasında kullanılan Android API kümesidir. `targetSdk 35`, uygulamanın hangi Android davranışlarına göre hazırlandığını belirtir; minimum cihaz sürümüyle aynı şey değildir.

## 2. Araçları hazırla

1. [Android Studio](https://developer.android.com/studio) kur. Ücretsizdir. Bu projeyi açarken `settings.gradle` dosyasının bulunduğu **hizlitv klasörünü** seç.
2. SDK Manager üzerinden **Android SDK Platform 35**, **Android SDK Build-Tools 35.0.0** ve **Platform-Tools** kur.
3. Gradle JDK olarak **JDK 17** seç. Terminalde `java -version` ile kullandığın Java sürümünü görebilirsin. IDE ile terminal farklı Java kullanıyor olabilir.
4. Projeyi açıp Gradle eşitlemesinin tamamlanmasını bekle. İlk açılış, bağımlılıkları internetten indirir.

Terminal kullanıyorsan SDK yolunu yerel `local.properties` dosyasına yaz:

```properties
sdk.dir=ANDROID_SDK_KLASORUNUN_TAM_YOLU
```

Bu satırdaki değeri kendi bilgisayarındaki gerçek SDK klasörüyle değiştir. Dosya Git'e eklenmez; her geliştiricinin yolu farklı olabilir. Yönetici izni olmadan kullanıcı klasörüne kurulmuş JDK/SDK da kullanılabilir; gerekli erişim işletim sistemine ve kurulum konumuna bağlıdır.

## 3. Test et ve APK üret

Terminali `hizlitv` içinde aç:

```sh
./gradlew :media-core:test :player:testDebugUnitTest
./gradlew :launcher:assembleDebug
```

Windows PowerShell'de komutların başını `.\gradlew.bat` yap. macOS/Linux'ta izin hatası alırsan bir kez `chmod +x gradlew` çalıştır.

Başarılı derlemede tek paket oluşur:

```text
launcher/build/outputs/apk/debug/launcher-debug.apk
```

Debug APK geliştirme anahtarıyla imzalanır. Başka bir anahtarla daha önce kurulmuş HızlıTV'nin üzerine kurulamayabilir. Bu durumda hemen uygulamayı kaldırma: kaldırmak kütüphaneyi ve eşleştirmeyi de siler. [İmza ve güncelleme rehberini](YAYINLAMA.md) oku.

## 4. TV'ye bağlan ve kur

TV ile bilgisayar aynı yerel ağda olsun. Android TV'nin Hakkında bölümünde Derleme satırına yedi kez basarak geliştirici seçeneklerini aç. Cihazın sunduğu USB/ağ hata ayıklama seçeneğini etkinleştir. Ağ üzerinden ADB yöntemi üretici ve Android sürümüne göre değişir; tüm TV'lerde doğrudan 5555 portu açık olmaz.

Klasik ağ ADB'si sunan ve kendi onayladığın cihazda:

```sh
adb connect TV_IP:5555
adb devices
```

`TV_IP` yerine TV'nin Ağ ayarlarında gördüğün adresi yaz. TV'deki yetkilendirme penceresini kendin onayla. `unauthorized` görünüyorsa cihaz henüz izin vermemiştir. Android'in eşleştirme koduyla kablosuz hata ayıklama sunduğu sürümlerde önce cihazın gösterdiği adres/portla `adb pair` kullanılır; Android 9/11 TV'lerinde bu menü her zaman bulunmaz.

Ardından:

```sh
adb install -r launcher/build/outputs/apk/debug/launcher-debug.apk
adb shell am start -n tr.hizlitv.home/.MainActivity
```

Birden fazla cihaz bağlıysa her komutta `adb -s CIHAZ_KIMLIGI ...` kullan. `-r` mevcut uygulamayı aynı imzayla günceller; uygulama verilerini silmez. Komut hata verirse başarı varsayma; `adb devices` ve hata çıktısını kontrol et.

HızlıTV, HOME tuşuna bağlanmaz. Sistem başlatıcısından normal bir uygulama gibi açılır. Cihaz menüsündeki **Orijinal ana ekran** sistemin seçtiği başlatıcıya döner.

## 5. İlk küçük değişikliğini yap

1. `tv-ui/.../TvUi.java` içinde `TEXT` rengini hafifçe değiştir; başlık ve düğme yazıları etkilenir.
2. `./gradlew :launcher:assembleDebug` çalıştır.
3. Aynı anahtarla derlenen APK'yı `adb install -r ...` ile güncelle.
4. TV'de arayüzü aç; okunabilirliği ve kumanda odağını kontrol et.
5. Değişikliği geri alıp tekrar derle. Kaynak kod değişikliği tek başına kurulu TV uygulamasını değiştirmez.

## Sonraki okuma

[Mimari rehberi](MIMARI.md) bir bağlantının telefondan oynatıcıya nasıl ulaştığını gösterir. [Android TV gezinme kılavuzu](https://developer.android.com/training/tv/get-started/navigation), kumanda odağının ve Geri tuşunun neden önemli olduğunu açıklar. Oynatma motoru için [Media3 ExoPlayer belgelerini](https://developer.android.com/media/media3/exoplayer) inceleyebilirsin.
