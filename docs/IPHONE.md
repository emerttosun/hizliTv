# iPhone'dan TV'ye bağlantı gönderme

[English](en/IPHONE.md) · **Türkçe**

En kolay ilk deneme: TV'de **Player → Telefondan ekle**, iPhone kamerasıyla QR'ı okut, açılan sayfaya bağlantını yapıştır ve **TV'ye gönder** düğmesine bas. İki cihaz aynı yerel ağda olsun. TV'de bulunan kaynağı **Oynat** ile onayla. M3U gönderdiysen **Listeyi ekle** seç.

QR sayfası internetteki bir site değil, TV'de çalışan küçük bir HTTP alıcısıdır. URL'nin sonundaki rastgele kod, o kuruluma ait eşleştirme kimliğidir. Güncellemede korunur. TV'nin IP adresi değişirse adresin başlangıcı değişebilir; modemden DHCP rezervasyonu yaparak TV'ye aynı yerel adresin verilmesini sağlayabilirsin. Menü adı modem modeline göre değişir.

## Safari Paylaş menüsüne eklemek

1. iPhone **Kestirmeler** uygulamasında yeni kestirme oluştur. Adını **TV'de izle** yap.
2. Kestirmenin ayrıntılarında **Paylaşım Sayfasında Göster** seçeneğini aç. Girdi türlerinde yalnızca **URL'ler** seç. Kullanmayacağın Uygulamalar gibi türleri kapat.
3. **URL İçeriğini Al** işlemini ekle. İşlem adı iOS sürümünde küçük farklılık gösterebilir.
4. URL alanına QR ile Safari'de açılan sayfanın **tam adresini** koy. `http://` öneki, port ve `/` sonrasındaki uzun kod dâhil olmalı. Yalnızca IP/port yazma. Alıcı HTTPS sunmadığı için bu adrese `https://` yazma.
5. Yöntem **POST** olsun. **Başlıklar** bölümünde tek bir satır ekle: anahtar `X-HizliTV-Token`, değer QR sayfasındaki uzun eşleştirme kodu. Anahtar adına `:` yazma. Başta/sonda boşluk, satır sonu veya boş bir ikinci başlık satırı bırakma.
6. **İstek Gövdesi → JSON** seç. **Metin** türünde alan ekle: anahtar `url`, değer **Kestirme Girişi** değişkeni. Değişkeni elle yazılmış sabit metin olarak ekleme.
7. Safari'de göndermek istediğin film/yayın/liste sayfasını aç. **Paylaş → TV'de izle** seç. iPhone yerel ağa erişim sorarsa bu kurduğun akış için izin ver.

Kestirme URL'sinin yapısı:

```text
http://TV_IP:8198/ESLESTIRME_KODU
```

Gönderilen gövdenin mantığı:

```json
{"url":"https://video.example.org/watch"}
```

`TV_IP`, `ESLESTIRME_KODU` ve örnek video adresi yer tutucudur. TV'nin gerçek adresini yalnızca kendi yerel Kestirmene gir. Eşleştirme kodunu herkese açık bir ekran görüntüsüne veya GitHub issue'suna koyma.

## Sonucu görmek

Geçici tanılama için **URL İçeriğini Al** işleminin altına **Göz At** ekle. Girdisi önceki işlemin **URL'nin İçeriği** çıktısı olsun; Kestirme Girişi değil. Apple bu yaklaşımı [işlemleri test etme kılavuzunda](https://support.apple.com/tr-tr/guide/shortcuts/apda75604f37/ios) anlatır. Çalışınca tanılama işlemini kaldırabilirsin.

| Sonuç | Kontrol |
| --- | --- |
| TLS / güvenli bağlantı hatası | Alıcı URL'si açıkça `http://` ile başlasın. Sertifika denetimini kapatma; bu sunucuda TLS yok. |
| Ağ bağlantısı kesildi | Önce QR sayfasını Safari'de yenile. Açılıyorsa Kestirme'de boş/bozuk HTTP başlığı olup olmadığını kontrol et. |
| Bulunamadı | Adreste `/` sonrasındaki eşleştirme yolu eksik veya yanlış olabilir. |
| Eşleştirme gerekli | `X-HizliTV-Token` başlığı veya değeri yanlış olabilir. |
| JSON içinde url metin alanı gerekli | `url` değeri bir metin olmalı; sayı, liste veya boş girdi gönderme. |
| TV'ye gönderildi | İstek alındı; kaynak aramasını ve oynatma onayını TV'de tamamla. |
| TV başka bir bağlantıyı inceliyor | Önceki isteğin tamamlanmasını bekle, sonra tekrar dene. |

Geliştirme ekranındaki ▶ düğmesi, paylaşılacak bir Safari URL'si sağlamadan çalıştırılabilir. Gerçek testi film sayfasının **Paylaş** menüsünden yap. TV'de video oynarken veya başka uygulamadayken alıcı kapalıdır; önce kütüphaneye dön.

## Panodaki bağlantıyı göndermek

Aynı isteği kullanan ikinci bir kestirme oluşturabilirsin: başına **Panoyu Al** işlemi ekle ve JSON `url` alanına onun sonucunu bağla. Bu yalnızca kestirmeyi çalıştırdığında panoyu okur. M3U metni için QR sayfasındaki kutu daha kolaydır; URL türüyle sınırlanmış Paylaş kestirmesi ham liste metni için hazırlanmadı.

Android TV ile Apple Universal Clipboard / Handoff entegrasyonu yoktur; kopyalanan her içerik kendiliğinden TV'ye aktarılmaz. AirPlay yansıtması da bu HTTP gönderim yolundan ayrıdır.
