# GitHub ve sürüm hazırlama

[English](en/RELEASING.md) · **Türkçe**

Bu klasör tek başına paylaşılacak Android TV projesidir. Üstteki eski Samsung/widget çalışmasını, cihaz günlüklerini, yedekleri veya yerel araç önbelleklerini aynı Git deposuna ekleme. Buradaki komutlar bir rehberdir; GitHub deposu kendiliğinden açılmaz ve kaynaklar otomatik yayımlanmaz.

## Yüklenecek klasör

Doğrudan bu `hizlitv` klasöründen Git ile gönderim yap. ZIP oluşturmak veya GitHub arayüzünden dosyaları tek tek yüklemek gerekmez. Önce paylaşılacak kaynakları kontrol et:

```sh
python3 scripts/check_public.py
git status --short
```

`.gitignore`, yerel SDK/imzalama ayarlarını, anahtarları ve derleme çıktılarını dışarıda tutar. Kontrol betiği ayrıca paylaşılabilir kaynaklarda belirgin kişisel veri kalıplarını tarar.

## Git deposunu başlatma

GitHub hesabında boş bir depo oluşturduktan sonra aşağıdaki yer tutucuları kendi hesabın ve depo adınla değiştir:

```sh
git init -b main
git add .
git diff --cached --stat
git status --short
git commit -m "Initial Android TV launcher and player"
git remote add origin https://github.com/HESABIN/DEPO_ADI.git
git push -u origin main
```

`git add .` yalnız **bu hizlitv klasörünün içinde** çalıştırılmalı. İmzalama/SDK dosyalarını `git status` içinde görürsen commit etmeden düzelt. ZIP hazırlamak push yapmak değildir; GitHub'a gönderim ayrı bir adımdır.

## CI ne yapıyor?

`.github/workflows/android.yml`, push ve pull request'te JDK 17/SDK 35 ortamında testleri, Android lint'i ve tek debug APK derlemesini çalıştırır. İsteğe bağlı eski Player geçiş aracının lint/derlemesini de kontrol eder; normal uygulama dağıtımı tek APK kalır. Sonuçları artifact olarak saklar. Herhangi bir release anahtarı veya hesap sırrı istemez. Bu workflow gerçek TV'ye veya iPhone'a bağlanmaz; gerçek cihaz kontrolleri ayrıca yapılır.

## Debug ile release farkı

Debug APK geliştirme için otomatik anahtarla imzalanır. Yeni bir geliştiricinin debug anahtarı, daha önce kurulu uygulamanın imzasıyla aynı olmayabilir. Release sürümünün kimliğini depo sahibi yönetir. **Aynı paket adını veri silmeden güncellemek için aynı imza gerekir.** Anahtarı kaybetmek, var olan kurulumlara aynı kimlikle güncelleme yapmayı engeller.

Varsayılan olarak release derlemesi, sahibi bir anahtar sağlamadıysa **imzasız APK** üretir:

```sh
./gradlew :launcher:assembleRelease
```

İmzalı release için özel `local-signing.properties` oluştur:

```properties
storeFile=IMZALAMA_DOSYASININ_YOLU
storePassword=DEPO_SAHIBININ_PAROLASI
keyAlias=ANAHTAR_TAKMA_ADI
keyPassword=ANAHTAR_PAROLASI
```

Bu değerler örnektir; kaynak depoya gerçek değerleri ekleme. `storeFile` proje köküne göre göreli veya tam yol olabilir. Aynı bilgiler CI'da özel ortam değişkenlerinden de sağlanabilir: `HIZLITV_KEYSTORE`, `HIZLITV_STORE_PASSWORD`, `HIZLITV_KEY_ALIAS`, `HIZLITV_KEY_PASSWORD`. Mevcut workflow release imzalama veya yayınlama yapmaz.

Yeni bir ürün için anahtar üretmek gerekiyorsa JDK'nın `keytool -genkeypair` aracını kullanabilirsin; anahtarı proje dışında ve yedekli sakla. Mevcut HızlıTV kurulumuna güncelleme hazırlıyorsan yeni anahtar oluşturma; o kuruluma ait mevcut anahtar gerekir.

## Sürüm numarası ve geri dönüş

Sürümün `versionCode` ve `versionName` değerleri `launcher/build.gradle` içindedir. `player` bir kütüphanedir; ayrı sürümlü APK üretmez. Güncellemede `versionCode` artırılır. Yayınlanmış bir APK'nın dosyasını aynı sürüm kimliği altında farklı içerikle değiştirme; yeni sürüm çıkar ve SHA256 özetini kaydet.

Eski APK'ya düşük sürüm numarasıyla doğrudan dönmek Android tarafından engellenebilir. `-d` her release kurulumu için çözüm değildir. Veri silmeden güvenilir bir geri dönüş yöntemi: eski kaynak davranışını aynı imzayla, **daha yüksek versionCode** kullanarak yeniden derleyip güncellemek. Kaldırma veya `pm clear`, kayıtları ve telefon eşleştirmesini siler.

## Yayına hazırlanırken

Kontrollerin sonucunu [TEST.md](TEST.md) içinde sürüm/cihaz kapsamıyla belirt. Orijinal lisansı ve üçüncü taraf bildirimlerini koru. Çalışan bir yayın listesi veya AirPlay alıcısının APK'sını kaynak ZIP'ine ekleme. GitHub Actions başarısını, gerçek iPhone ve TV deneyiminin tamamının test edildiği şeklinde sunma.

## İsteğe bağlı kaynak arşivi

Proje kökünde:

```sh
python3 scripts/check_public.py
python3 scripts/export_source.py ../HizliTV-source.zip
```

İhracat, açık bir dosya/klasör izin listesi kullanır. Yerel SDK yolu, imzalama ayarları, anahtarlar, derleme çıktıları ve günlükler dışarıda kalır. `gradle-wrapper.jar` kaynağı derlemek için bilinçli olarak dâhil edilir. ZIP'i başka klasöre açıp JDK/SDK dışında kendi makinenin özel dosyaları olmadan derlemeyi dene.


## İki dil ve AirPlay kaynakları

Yayın öncesi `python3 scripts/check_translations.py` ve `python3 -m unittest discover -s airplay/tests` de çalıştır. Actions, `hizlitv-debug-apk` ile `verification-reports` çıktılarını ayrı üretir. CI debug APK’sı kalıcı imzalı Release değildir. AirPlay kaynağı `airplay/receiver` Git alt modülüne sabitlenmiştir; ana Gradle derlemesi alıcıyı derlemez. Profil dosyaları genel, `.state` geri alma kayıtları özeldir. İngilizce README ana giriş, README.tr.md Türkçe giriş olarak korunur.
