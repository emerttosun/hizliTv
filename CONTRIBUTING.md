# Katkı rehberi

Amaç, düşük güçlü Android TV cihazlarında kumandayla rahat kullanılabilen küçük bir uygulama tutmak. Yeni özellikler kadar kolay kurulum, anlaşılır hata mesajı ve veri korunması önemlidir.

## Başlangıç

1. [Başlangıç rehberi](docs/BASLANGIC.md) ile debug APK üret.
2. [Mimariyi](docs/MIMARI.md) okuyup değişikliğin doğru modülünü seç.
3. Küçük bir dal aç ve tek bir kullanıcı sorununu çöz. Uygulama verilerini etkileyen değişikliğe geçiş planı ekle.
4. İlgili testleri, ardından CI komutlarını çalıştır. Gerçek TV kontrolünü birim testinden ayrı belirt.

```sh
./gradlew :media-core:test :player:testDebugUnitTest
./gradlew :launcher:lintDebug :player:lintDebug
./gradlew :launcher:assembleDebug
python3 scripts/check_public.py
```

## Kod ilkeleri

- UI veri depolamasın veya ağ isteği yapmasın. `LibraryScreen` görünüm, `LibraryRepository` arka plan kayıt akışı, `Store` kalıcılık, `Resolver` kaynak bulma işini yapar.
- Liste ve URL kurallarını mümkünse Android'den bağımsız `media-core` içinde tut.
- Cihaz IP'si, kullanıcı hesabı veya yerel bilgisayar yolu sabitleme. İsteğe bağlı uygulama entegrasyonlarını `AppTargets` içine koy.
- Yeni kaynak biçimini `SourceAdapter` ile ekle; genel çözümleyiciye her site için dallanma ekleme. Test fixture'ını kendin üret; özel site yanıtlarını, hesapları veya erişim kodlarını depoya koyma.
- Ağ/iş parçacığı işlerini Activity yaşam döngüsüne bağla. Gereksiz arka plan servisi, sürekli çizim ve uzak arayüz bağımlılığı ekleme.
- DPAD odağı, OK ve Geri davranışı öngörülebilir olsun. TV HOME ataması ve ses düzeyi otomatik değiştirilmesin.
- API/sürüm uyumluluğu hatasını lint'i genel kapatarak gizleme. Özel bir durum varsa en dar kapsamda açıklayıp belgele.
- Değişen davranışı test et; yalnız uygulamanın kendi kodunu tekrar eden test ekleme.

## İnceleme için

PR açıklamasında kullanıcı açısından önce/sonra davranışını, ilgili kontrolleri ve kalan sınırları yaz. Ekran değişikliğinde eşleştirme kodu içermeyen gerçek cihaz görüntüsü ekle. Test edilmemiş bir Android sürümü veya iPhone akışını çalışıyor diye sunma.

Yazılar Türkçe hazırlanmıştır. Kodun işlevsel ayrımı başka ürünlerde kullanımı kolaylaştırır; tüm kullanıcı metinlerinin çok dilli kaynaklara taşınması henüz tamamlanmış değildir.
