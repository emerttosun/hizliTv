# Değişiklikler

[English](en/CHANGELOG.md) · **Türkçe**

## 0.10.1 — 19 Kasım 2026

- Telefon alıcısı kütüphane kapanınca portunu tamamen bırakıyor; kütüphaneyi hemen yeniden açınca QR adresi aynı kalıyor. Önceden kısa bir yarış durumu alıcıyı başka bir porta taşıyıp mevcut iPhone kestirmesini bozabiliyordu.
- CI: Gradle önbelleği, bağımlılık indirme hatasında bir kez yeniden deneme ve aynı portu 20 kez yeniden açan bir regresyon testi.

## 0.10.0 — 19 Kasım 2026

0.9.0'ın üzerine aynı imza anahtarıyla kurulur; kütüphane, oynatma konumları ve telefon eşleştirmesi korunur.

**Telefondan gönderme**
- Eşleştirme QR'ı kütüphanenin yanında hep durur; eşleştirme ilk açılışta kendiliğinden oluşur, önce açılacak bir pencere yoktur. Panelde OK büyük QR'ı ve yeni QR kod seçeneğini açar.
- Telefon sayfasında metin kutusu en üstte, durum renkli; iPhone kestirmesi için Adresi kopyala / Anahtarı kopyala düğmeleri var. İstek biçimi değişmedi; mevcut kestirmeler çalışmaya devam eder.
- Alıcının portu TV'de doluysa alıcı başlamadan kalmaz, boş bir port seçilir.

**Ana ekran**
- Yedi yazılı düğmenin yerine dört yuvarlak araç simgesi (AirPlay, Bellek, Ayarlar, Eski ana ekran); odaktaki simgenin adı, RAM'i de gösteren selamlama panelinde görünür.
- Favoriler ve tüm uygulamalar tek ekranda. Kumandada Menü tuşu varsa ikisi arasında geçer; Geri favorilere döner.
- RAM'i rahatlat Bellek ekranına taşındı. Alıcı kurulu değilse AirPlay simgesi gizlenir.

**Kütüphane**
- Üç düğmenin yerine tek Göster düğmesi (Tümü, Favoriler veya bir grup); Geri al yalnızca geri alınacak bir şey varken görünür.
- Kayıtta OK doğrudan oynatır. Telefondan gelen bağlantılar yine TV'de onay ister.
- Kütüphanedeki tüm pencereler TV stilinde; liste yalnızca odaktayken vurgulanır.

**Oynatıcı**
- Kumanda odaklı yeni bilgi şeridi: başlık, kaynak, listedeki sıra, CANLI rozeti, saat, ilerleme ve kalan süre.
- OK oynat/duraklat, ◀ ▶ sarma (basılı tutunca hızlanır), ▲ ▼ / CH± önceki/sonraki kayıt, seçenekler için uzun OK (veya Menü/Info).
- Seçenekler: kalite, ses, altyazı, altyazı boyutu, görüntü (sığdır/yakınlaştır/uzat), hız ve uyku zamanlayıcısı. Her biri küçük bir `PlayerOption` modülüdür.
- Kaldığın yerden devam veya baştan başla, kopan yayında sessiz yeniden bağlanma, Yeniden dene / Sonraki / Geri seçenekli anlaşılır hata mesajları.
- Dosya adından okunabilir başlıklar; yeterli belleği olan 4K panellerde 4K'ya kadar oynatma.

**Başka Android TV'ler**
- `DeviceProfile` paneli ve RAM'i çalışırken okur; kontrol listesi için [başka bir Android TV'de çalıştırma](YENI_TV.md).

**Proje**
- Ekran görüntüleri yenilendi; CI eylemleri güncellendi, hatalar annotation olarak raporlanıyor.
