# M3U listesi nasıl hazırlanır?

[English](en/IPTV.md) · **Türkçe**

M3U, kanal adlarını ve yayın adreslerini yazdığın bir metin biçimidir. HızlıTV bir kanal aboneliği veya hazır yayın listesi sağlamaz; kendi yayın bağlantılarını eklersin.

En küçük örnek:

```m3u
#EXTM3U
#EXTINF:-1 group-title="Haber",Örnek Haber
https://media.example.org/haber/master.m3u8
#EXTINF:-1 group-title="Belgesel",Örnek Belgesel
https://media.example.org/belgesel/master.m3u8
```

Bu örnekteki URL'ler **yer tutucudur, çalışan kanal değildir**. Kendi gerçek yayın adreslerinle değiştir. Hazır başlangıç dosyası: [examples/kanallar.m3u](../examples/kanallar.m3u).

## Satırların anlamı

- `#EXTM3U`: bunun bir liste olduğunu belirtir. En üstte bulunsun.
- `#EXTINF:-1`: ardından bir kanal açıklaması gelir. `-1`, burada süresi belli olmayan kaydı ifade eder.
- `group-title="Haber"`: kütüphanedeki grup adı.
- Virgülden sonraki yazı: TV'de görünecek kanal adı.
- Bir sonraki satır: **doğrudan yayın URL'si**. Normal kanal web sayfası bu satırda yeterli değildir.

Metni UTF-8 olarak `kanallar.m3u` adıyla saklayabilir veya dosya oluşturmadan QR sayfasına doğrudan yapıştırabilirsin. Uygulamadaki telefon sayfasında şu anda dosya yükleme düğmesi yoktur; dosyanın **metnini** ya da internette sunulan liste URL'sini yapıştır.

## Listeyi TV'ye verme

1. TV'de **Player → Telefondan ekle** aç.
2. Aynı yerel ağdaki telefondan QR'ı okut.
3. Kutucuğa M3U metnini veya liste URL'sini yapıştır; **TV'ye gönder** de.
4. TV'de bulunan kanal sayısını kontrol et, **Listeyi ekle** seç.
5. Kütüphanede kanal adını seçip **Oynat** de. Grup filtresiyle kanalları ayırabilirsin.

Kanal URL'si aynıysa yeniden içe aktarma kaydı günceller ve favori bilgisini korur. URL değişmişse yeni kayıt oluşabilir; eski kaydı uzun OK ile kaldırabilirsin. Kütüphanedeki **Geri al**, son içe aktarmayı geri döndürür.

## Desteklenen ek bilgiler

`#EXTGRP:Grup Adı`, grup belirtmenin başka bir yoludur. Bazı yayınlar belirli Referer/User-Agent başlığı ister; kaynağın sağladığı bilgiler varsa aşağıdaki VLC seçenekleri okunur:

```m3u
#EXTM3U
#EXTINF:-1 group-title="Özel",Örnek yayın
#EXTVLCOPT:http-referrer=https://video.example.org/watch
#EXTVLCOPT:http-user-agent=ExamplePlayer/1
https://media.example.org/live/master.m3u8
```

Bu özellik bir kaynağa giriş yapmaz veya erişim izni vermez. Kod CR/LF içeren başlık değerlerini reddeder. Listede `tvg-logo`, `tvg-id` gibi alanlar bulunabilir; mevcut arayüz bunlarla logo veya EPG rehberi oluşturmaz.

## HLS ve M3U farkı

Bir `.m3u8` bağlantısı bazen **tek kanalın HLS akışını**, bazen bir kanal listesini içerir. Kod yalnızca uzantıya güvenmez. `#EXT-X-...` etiketleri varsa akış tek video/kanal olarak ele alınır; kanal listesi gibi içe aktarılmaz.

En fazla 2 MB liste ve 5000 kanal desteklenir. Desteklenen URL şemaları HTTP/HTTPS'tir. `file://`, `content://` veya RTSP giriş yolu eklenmiş değildir. IPTV listesi içinde normal film web sayfası vermek, tekil web sayfası çözümleme akışını otomatik çalıştırmaz; film sayfasını ayrıca telefondan gönder.
