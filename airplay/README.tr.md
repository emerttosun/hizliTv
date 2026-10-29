# AirPlay yardımcı uygulaması

[English](README.md) · [Türkçe](README.tr.md)

HızlıTV, **jqssun ve diğer katkıcıların AirPlay Server for Android** alıcısını açar. Kaynak kod, [receiver](receiver) altında belirli bir sürüme sabitlenmiş Git alt deposu olarak eklenmiştir. **HızlıTV’nin MIT lisanslı ana ekran/Player APK’sına bağlanmaz.** Alıcı GPL-3.0 lisansını korur: [lisans](RECEIVER-LICENSE), [kaynak kaydı](upstream.json).

Sürüm **v0.0.36**, commit `a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea`. Android11 Mi TV Stick’te iPhone/iPad ile daha önce denenmiş upstream sürümüdür. Alıcının protokolü veya APK’sı özgün HızlıTV kodu olarak sunulmaz.

## Kurulum

1. Kendi TV’ne ADB ile bağlanıp hata ayıklama isteğini onayla. [Başlangıç rehberi](../docs/BASLANGIC.md).
2. Sabitlenmiş orijinal APK’yı indir; araç SHA256 özetini doğrular:

   ```sh
   python3 airplay/manage.py download
   ```

3. Kur ve aç:

   ```sh
   adb -s TV_SERIAL install -r airplay/.cache/AirPlay-0.0.36.apk
   adb -s TV_SERIAL shell am start -n io.github.jqssun.airplay/.MainActivity
   ```

4. iPhone/iPad/Mac ile TV aynı yerel ağda olsun. Ekran Yansıtma’dan alıcının görünen adını seç; istenirse TV’deki PIN’i gir.

`TV_SERIAL`, `adb devices` çıktısındaki cihaz kimliğidir. APK orijinal imzayı korur. Kurulu alıcının imzası farklıysa verileri silmek yerine imza uyuşmazlığını çöz. Kaynak deposunda alıcı APK’sı gömülü değildir.

## Profiller ve geri alma

Profil, kullanıcının açıkça başlattığı bir kurulum adımıdır; HızlıTV açılırken kendiliğinden uygulanmaz.

- `balanced`: 1080p / 30 FPS sınırı, otomatik ses tamponu, PIN zorunlu.
- `low-latency`: aynı görüntü sınırı, düşük gecikme seçeneği, sabit 20 ms ses tamponu, PIN zorunlu.

İkisi de telefonun sistem sesini eşitlemesini kapatır. TV’nin sistem sesini doğrudan değiştirmez. Küçük tampon kararsız ağda takılma yapabilir; sıfır gecikme garantisi değildir.

Cihaza bağlanmadan önizleme:

```sh
python3 airplay/manage.py apply --profile balanced --dry-run
```

Onayladığın cihazda uygula:

```sh
python3 airplay/manage.py --serial TV_SERIAL status
python3 airplay/manage.py --serial TV_SERIAL apply --profile balanced --name Salon
```

Araç mevcut değerleri okur; **değişiklikten önce** özel yedek dosyasını kaydeder, her tamamlanan ayarı işler ve sonucu doğrular. Çıktıdaki yedek dosyasıyla geri dön:

```sh
python3 airplay/manage.py --serial TV_SERIAL restore airplay/.state/KAYIT_DOSYASI.json
```

Geri alma da kendi yedeğini oluşturur. `.state/` ve `.cache/` Git dışında tutulur. Cihaz bağlantısı kesilirse kısmi değişiklik olabilir; eski değerler yedekte kalır. Yedek sonrasında yapılan ilgisiz ayar değişiklikleri otomatik birleştirilmez. Farklı ADB için `--adb YOL`, sunucu portu için `--port PORT` kullan.

## Kaynaktan derlemek

```sh
git submodule update --init --recursive airplay/receiver
cd airplay/receiver
./gradlew assembleDebug
```

Bu ayrı bir derlemedir. Sabitlenmiş upstream CI JDK21, derleme dosyaları SDK37 ve NDK27.0.12077973 ister; CMake/native bağımlılıkları da gerekir. HızlıTV’nin SDK35 derlemesi AirPlay alıcısını derlemez. [Upstream derleme dosyalarını](receiver/app/build.gradle.kts) ve [CI’ını](receiver/.github/workflows/apk.yml) izle. Bizim CI, HızlıTV ve profil aracını sınar; bütün native alıcıyı derlediği iddia edilmez. Kendi imzanla üretilen APK orijinal imzalı alıcının üzerine kurulamayabilir.

Upstream Android7+ desteği, görüntü/ses yansıtma ve korumasız medya oynatma bildirir. DRM korumalı içerik desteklenmez. Apple cihaz sürümleri, codec ve gecikme kendi donanımında denenmelidir. Apple ile resmi bir bağlantı yoktur.
