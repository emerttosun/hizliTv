# Kumandayla günlük kullanım

[English](en/USAGE.md) · **Türkçe**

## Ana ekran

HızlıTV'yi TV'nin uygulama listesinden aç. Favoriler üstte, kurulu tüm uygulamalar hemen altında aynı ekranda durur. Sağ/sol/yukarı/aşağı ile kart seç; beyaz çerçeve hangi kartın seçili olduğunu gösterir. **OK** uygulamayı açar. **Uzun OK** seçili uygulamayı favoriye ekler veya favoriden çıkarır.

Saatin yanındaki dört yuvarlak simge **AirPlay** (alıcı kurulu değilse gizlenir), **Bellek**, **Ayarlar** ve **Eski ana ekran**ı açar; odaklanılan simgenin adı selamlama panelinde görünür. **Player** ilk favori karttır. Kumandada **Menü** tuşu varsa favoriler ile tüm uygulamalar arasında geçer. **Geri**, tüm uygulamalardan favorilere, favorilerden önceki ekrana döner. **HOME** sistemin orijinal başlatıcısını açar.

**Player** kartı kişisel kütüphaneyi açar. Diğer kartlar cihazda zaten kurulu uygulamalardır; HızlıTV onların aboneliklerini veya içeriklerini sağlamaz.

## Kütüphane

![Player kütüphanesi](images/library.png)

- **Telefondan gönder paneli:** QR kod kütüphanenin sağında hep görünür; ilk açılışta eşleştirme kendiliğinden oluşur. Panelde OK büyük QR'ı ve **Yeni QR kod** seçeneğini açar. Daha önce kurulmuş Kestirme için tekrar QR okutmak gerekmez; kütüphane görünür olsun.
- **Göster düğmesi:** Tümü, Favoriler veya bir M3U grubu arasında seçim yapar.
- **Arama:** kanal veya film adını arar. Türkçe harflerle arama desteklenir.
- Kayıtta **OK:** doğrudan oynatır; kütüphane senin listen olduğu için ek onay sorulmaz. Sayfadan eklenen kayıtların geçici video bağlantısı önce yeniden bulunur. Telefondan gelen bağlantılar yine TV’de onay ister.
- Kayıtta **uzun OK:** favori veya kayıt kaldırma seçeneklerini açar.
- **Bağlantı / liste:** elle bağlantı veya M3U girme. **Geri al:** yalnızca geri alınabilecek bir değişiklik varken görünür; son kütüphane değişikliğini geri alır.
- **← HızlıTV:** aynı uygulamanın ana ekranına dönme. Player için ikinci APK gerekmez.

Kanalları bir kez içe aktarınca listede adlarıyla seçebilirsin. Bir yayın adresi değişirse güncel listeyi yeniden gönder. Şu anda zamanlanmış otomatik liste yenileme veya EPG rehberi yoktur.

Bağlantı gönderince HızlıTV tasarımlı onay penceresi açılır. **Oynat**, **Kütüphaneye ekle** veya **Vazgeç** seçilebilir. Birden fazla kaynak bulunduysa önce kaynak seçilir. Gönderim kendiliğinden oynatmayı veya kaydetmeyi başlatmaz.

## Film oynarken

Oynatma ekranının üstünde başlık, grup/kaynak, listedeki sıra, canlı yayınlarda **CANLI** rozeti ve saat; altında kalan süreli ilerleme çubuğu ve tuş ipucu görünür. Oynarken beş saniye sonra gizlenir, duraklatınca açık kalır.

| Tuş | İşlem |
| --- | --- |
| OK / Oynat-Duraklat | Oynat veya duraklat (OK bırakılınca çalışır) |
| ◀ ▶ / Geri sar, İleri sar | 10 sn sar; basılı tutunca 30 ve 60 sn adımlara hızlanır (yalnız filmler) |
| ▲ ▼ / CH−, CH+ | Açıldığı listedeki (geçerli filtre veya arama) önceki/sonraki kayıt; art arda basınca hedef değişir, bırakınca geçer |
| Uzun OK / Menü / Info | Seçenekler: kalite, ses, altyazı, görüntü (sığdır, yakınlaştır, uzat) ve hız (yalnız filmler). Uzun OK, Menü tuşu olmayan kumandalarda da çalışır |
| Geri | Önce bilgi şeridini gizler, sonra oynatmadan çıkar |

Kayıtlı konumu en az bir dakika olan film **Kaldığın yerden** / **Baştan başla** sorar; bu soruda Geri kaldığı yerden devam eder. Oynamaya başlamış bir yayının bağlantısı koparsa oynatıcı hata göstermeden önce üç kez sessizce yeniden bağlanmayı dener. Hata penceresi olası nedeni (reddedilen/süresi dolmuş bağlantı, bağlantı yok, desteklenmeyen biçim, kopya koruması) söyler; Yeniden dene, listede Sonraki ve Geri seçenekleri sunar. Kaynağın sunmadığı seçenek üretilmez. Video çevresindeki boşluklar siyahtır; Sığdır en-boy oranını korur ve varsayılandır, görüntü seçimi hatırlanır.

Ses tuşları sistemin medya akışını kontrol eder. TV'nin kendi ses ayarı ile Android TV cihazının medya sesi iki ayrı seviye olabilir. Ses düşükse önce cihazın medya sesini kontrol et. Bu uygulama her açılışta sesi yükseltmez.

## Görünür kısayollar ve bellek

RAM kullanım oranı ana ekrandaki selamlama panelinde görünür. Yüksek doluluk tek başına sorun değildir; Android boş RAM'i önbellek için de kullanır.

**Bellekteki uygulamalar**, izinlerin sağladığı ölçümleri gösterir. Süreç varlığı ile o an ekranda açık uygulama aynı şey değildir; RAM ölçümleri Android'in son örneği olabilir. **Bellek** ekranındaki **RAM’i rahatlat** yalnızca kullanıcının isteğiyle uygun arka plan uygulamaları için Android'in izin verdiği isteği gönderir. Android onları tekrar başlatabilir; belirli miktarda bellek kazanımı garanti edilmez.

Seçilen uygulamaları doğrudan durdurma için cihaz içi ADB eşleştirmesi ayrı ve isteğe bağlıdır. Yeni kurulumda otomatik yetkilendirilmez. İzinler ve sınırlamalar [SECURITY.md](../SECURITY.md) içinde açıklanır.

## AirPlay

Ana ekrandaki AirPlay kısayolundan kurulu alıcıyı açabilirsin. iPhone ekran yansıtması o uygulamanın sağladığı özelliklerle çalışır. QR/Kestirme gönderimi, AirPlay ekran yansıtmasından farklıdır: film URL'si gönderilince videoyu Mi Stick kendi oynatıcısında açar; telefon ekranını sürekli aktarmak gerekmez. AirPlay alıcısının kaynağı ayrı lisanslı Git alt modülüne sabitlenmiştir; özgün APK ayrıca indirilir.

## Dil seçimi

Ana ekranın üstündeki **EN/TR** düğmesi, Sistem dili / English / Türkçe seçeneklerini açar. İngilizce kaynak dili ve desteklenmeyen diller için varsayılandır. Tercih yalnız HızlıTV’yi etkiler; Android dili değişmez. Player ve hata mesajları aynı tercihi izler. Telefon sayfası tarayıcı dilini kullanır; sayfanın dil seçicisinden de değiştirilebilir.

AirPlay ayrı isteğe bağlı uygulamadır. [Türkçe AirPlay kurulumu ve geri alınabilir profiller](../airplay/README.tr.md).
