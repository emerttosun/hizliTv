# Doğrulama ve sınırlar

[English](en/TESTING.md) · **Türkçe**

Sürüm: **HızlıTV 0.9.0**, gömülü Player, tek APK. Tarih: 29 Ekim 2026.

## Otomatik kontroller

```sh
./gradlew :media-core:test :player:testDebugUnitTest
./gradlew :launcher:lintDebug :player:lintDebug
./gradlew :launcher:assembleDebug
./gradlew :launcher:assembleRelease
python3 scripts/check_public.py
```

0.8.1’de mevcut 15 işlev testi, sıfır hata ve sıfır atlanan testle geçti. 0.8.3’te oynatma zemini ve bağlantı sonrası pencereler değişti; launcher/player/tv-ui Android lint ve release derlemesi tekrar geçti. Tam Android lint kontrolü geçti; kaynaklarda çeviri/araç önizlemesi gibi ölümcül olmayan uyarılar kalabilir. Android 6/API 23 için `Map.getOrDefault` ve `java.util.function.Consumer` bağımlılığı kaldırıldı. Media3'ün kararsız API yüzeyine yalnız PlayerActivity içinde açık opt-in verildi; lint genel olarak kapatılmadı.

| Alan | Kanıt |
| --- | --- |
| M3U | Grup/başlık, tırnak içindeki virgül, göreli URL, başlık seçenekleri, geçersiz kayıt ve 5000 kayıt sınırı |
| URL | İzinli şemalar ve CR/LF başlık enjeksiyonunun reddi |
| Filtreleme | Türkçe harfler, grup/favori birleşimi, asıl listenin değişmemesi |
| HTML çözümleme | Yerel socket fixture'ında iframe, video ve altyazı; reklam betiğine istek gitmemesi |
| Kaynak türü | `.m3u8` uzantılı kanal listesi ile `.txt` HLS yayınının ayrılması |
| HTTP hatası | Gerçek 403 yanıtının hata olarak iletilmesi |
| Site bağdaştırıcısı | Kendi ürettiğimiz sentetik veri, değişen isimler ve farklı ana makinede reddetme |
| Modülerlik | Enjekte edilen örnek bağdaştırıcıyla genel Resolver değiştirilmeden kaynak bulunması |
| Telefon alıcısı | Token/köken doğrulama, JSON, kararlı yeniden başlama ve özel veriyi loglamayan tanılama |

Testler özel hesap, çalışan kanal listesi veya kişisel HTML yedeği gerektirmez. Socket testleri loopback üzerinde geçici port açar; harici film sitelerine bağlanmaz. `org.json` JVM test bağımlılığı Android'in gerçek çalışma zamanı uygulaması yerine yalnız test ortamında kullanılır.

## 0.9.0 iki dil doğrulaması

18 JVM testi (8 media-core, 10 player) ve AirPlay araçlarının 3 Python testi geçti. İngilizce/Türkçe kaynaklarda çoğullar ve yer tutucular dahil 226 girdi eşleşiyor. Launcher, Player ve tv-ui lint ile debug/release derlemeleri geçti. Yeni testler iki dilde arama ve telefon sayfası dil seçimini kapsıyor. 0.9.0 aynı imzayla Mi Stick’e kuruldu. Ana ekran, gömülü kütüphane ve kaynak onayı İngilizce/Türkçe kontrol edildi. İngilizce liste onayı ve geçersiz bağlantı hatası, ilk Oynat odağı, iki dilde telefon sayfası yanıtları ve eski eşleştirme adresi doğrulandı. Dil tekrar Sistem dili’ne döndürüldü; kütüphane boş kaldı ve stok HOME korundu. TV düğmelerinde ilk dokunuş yalnız odak aldığı için iptal işlemleri gerçek kumanda OK yolu ile tamamlandı. Yeni belgelerdeki görüntüler gerçek 0.9.0 ekranlarıdır. Önceki performans ölçümleri yeni sürümün ölçümü gibi sunulmaz. Yeni telefon sayfası kontrolü HTTP istemcisiyle yapıldı; fiziksel iPhone testi olarak sunulmaz.

## Gerçek cihaz

### 0.8.3 arayüz güncellemesi

VersionCode22 aynı imzayla kuruldu. Siyah oynatma teması yalnız video ekranına uygulandı. Kaynak onayı, kanal listesi onayı, çoklu kaynak seçimi ve hata mesajı ortak `TvDialog` ile TV’de doğrulandı. İlk Oynat odağı, sağ tuşuyla diğer düğmeye geçiş ve çoklu kaynakta tek OK ile onay açılması sınandı. Yerel, sentetik HTML/HTTP404 fixture’ı kullanıldı; gerçek kullanıcı bağlantısı veya kanal listesi gerekmedi. Açık örnek video yeni pencereden OK ile başlatıldı ve ilk video karesi callback’i alındı. Test kaynağı kaydedilmedi; boş kütüphane korundu.

`launcher`, `player` ve `tv-ui` lint kontrolleri ve release derlemesi geçti. Kaynaklar yerel Git’te tutulur; özel imza ayarları, cihaz günlükleri ve APK’lar Git’e alınmaz.

### Önceki 0.8.1 temel doğrulaması

HızlıTV 0.8.1 aynı imzayla Android 11 Mi TV Stick’e kuruldu. Ana ekran ve kütüphanenin gerçek ekran görüntüleri incelendi. HOME ataması ve uygulama verilerini temizleme işlemi yapılmadı.

Doğrulananlar:

- Player kartında kumandanın **OK** tuşu, aynı `tr.hizlitv.home` paketi içindeki Player Activity’sini açtı. Bu testte Player’a doğrudan `am start` gönderilmedi.
- Eski kütüphane boştu; beş izleme konumu, telefon anahtarı ve port aktarıldı. Özel geçiş arşivi kaydedildi. Eski QR/Kestirme adresi HTTP200 verdi. Köprü, farklı imzalı shell okuma isteğini reddetti.
- Yerel telefon protokolü üzerinden gönderim HTTP202 verdi ve TV’de kaynak onayı açıldı. 202 tek başına oynatma başarısı sayılmadı.
- İki sentetik kanallı M3U listesi onayla eklendi; iki kayıt göründü. **Geri al** ile tekrar boş kütüphaneye dönüldü. Gerçek kullanıcı kanal listesi bu teste gerekmedi.
- Açık ExoPlayer örnek videosunda aynı APK içindeki Media3 oynatma ekranı hazır durumuna ulaştı; ilk video karesi callback’i geldi. Yaklaşık beş saniyelik 320×180 örnekte çözücü 141 kare çizdi, sıfır kare düşürdü. Bu kısa test uzun süreli veya 1080p yayın garantisi değildir. Donanım video katmanı ekran görüntüsünde siyah görünebildiği için siyah PNG, oynatma başarısı veya hatası olarak yorumlanmadı.
- Oynatmadan Geri ile kütüphaneye, **← HızlıTV** ile ana ekrana dönüldü. Kütüphaneden çıkınca yerel telefon alıcısının kapandığı doğrulandı.

Yeni sürümde telefon alıcısının istemcisi aynı HTTP protokolüdür. Gerçek iPhone Safari Paylaş/Kestirme akışı ve video/ses daha önce kullanıcı tarafından doğrulanmıştır. Bu sürümde bilgisayardan aynı protokolle uçtan uca gönderim sınandı; her iOS sürümü veya her site için garanti değildir. Kullanılan kaynağın dil/kalite/altyazı seçenekleri ayrıca denenmelidir.

## Hız ve bellek ölçümü

Ana ekran görünürken RAM örneklenir; duraklatıldığında örnekleme ve saat callback’leri kaldırılır. Player görünmezken telefon portu kapatılır. ExoPlayer yalnız video Activity’si açıldığında oluşturulur ve çıkınca serbest bırakılır.

Kütüphane disk işlemleri ve indeksleme sıralı arka plan işçisine taşındı. 5000 kayıtlı saf Java testinde Türkçe arama, grup ve favori birleşimi doğrulandı. `ListView` yalnız gerekli satır View’larını oluşturur. Ana ekrandaki uygulama keşfi kısa süre önbellekte tutulur; dönüşte değişmeyen kartlar yeniden kurulmaz. Odak çerçeveleri önceden oluşturulur, dinamik gölge yoktur.

Ölçüm yöntemi: yalnız kendi paketimiz `am force-stop` ile durdurulup üç kez `am start -W` ile açılır. Player’a kumanda OK ile geçilir; ActivityTaskManager’ın Displayed kaydı okunur. Süreç belleği `dumpsys meminfo` PSS değeridir, RAM panelindeki sistem doluluk oranı değildir. Kumanda kareleri `dumpsys gfxinfo` ile ayrıca örneklenir. Üç örnek istatistiksel benchmark değildir; sıcaklık, sistem yükü ve ART önbelleği sonucu değiştirir.

Yerelleştirme öncesi kısa örneklem:

| Ölçüm | Sonuç |
| --- | --- |
| Ana ekran soğuk açılış, 3 örnek | 621 / 621 / 633 ms; medyan 621 ms |
| Gömülü Player Displayed kaydı, 2 örnek | 133 / 146 ms |
| Ana ekran süreç PSS | yaklaşık 63 MiB |
| Kütüphane süreç PSS | yaklaşık 49 MiB |
| Aralıklı 20 yön tuşu, öncesi | 44 çizim karesinden 16 geç kare; p90 38 ms |
| Aynı yöntem, gölge/çerçeve düzeltmesi sonrası | 33 çizim karesinden 4 geç kare; p90 31 ms |

Bu sayılar cihazın o anki ölçümleridir. Kare sayısı, tuşların kaç kez örneklendiğine ve sistem çizimine göre değişir; sonuçlardan kesin bir hızlanma yüzdesi çıkarılmaz. Tek komutta 60 tuş gönderilen ayrı stres denemesinde 46 çizim karesinin 38’i geç kaldı. Gerçek kullanım testiyle bu tuş yağmuru aynı ölçüm değildir; 60 fps veya hiç takılmama garantisi verilmez.

## Dağıtım kontrolü

CI workflow projede bulunur; son sonucu deponun Actions ekranından kontrol et. Workflow dosyasının bulunması tek başına başarı kanıtı değildir. Yerelde aynı görevler kullanılır. Kaynak ihracatı kişisel veriler için taranır. Temiz ZIP, özel `local.properties` ve imzalama dosyası olmadan başka klasöre açılıp Gradle Wrapper ile test/lint/debug/release derlemesinden geçirildi; başarılı oldu. Debug APK ve beklenen imzasız release APK oluştu. JDK 17/SDK 35 ve bağımlılık önbelleği dışarıdan sağlandı.

Android 6'dan itibaren derleme/lint uyumluluğu hedeflenir; gerçek cihazda Android 11 denenmiştir. Tüm üreticiler, Android sürümleri ve codec'ler doğrulanmış değildir. Arayüz açılışında tek bir süre ölçümü, genel performans karşılaştırması değildir. Kaynak sunucu/ağ yavaşlığı uygulamanın tek başına ortadan kaldırabileceği bir şey değildir.

0.9.0 oynatma kontrolünde aynı APK içindeki Player ilk video karesini ve hazır olayını bildirdi. Açık 320×180 örnekte 3463 kare çizildi, sıfır kare düştü. İlk kare, ağ/hazırlık dahil oynatma isteğinden yaklaşık 8,6 saniye sonra geldi; ilk 7 saniyelik gözlem kısa kaldı. Bu tek örnek açılış hızı/codec garantisi değildir; test kaynağı kütüphaneye eklenmedi.
