package tr.hizlitv.player;

import java.util.Locale;
import org.json.JSONObject;

/** Independent bilingual phone page; the TV pairing protocol is unchanged. */
final class PhonePage {
    private static final String[][] TEXT = {
        {"title", "Send to HızlıTV", "HızlıTV’ye gönder"},
        {"intro", "Paste a video page, direct video link or M3U channel list. Confirm playback on the TV.", "Video sayfası, doğrudan video bağlantısı veya M3U kanal listesi yapıştır. Oynatmayı TV’de onayla."},
        {"input", "Link or channel list", "Bağlantı veya kanal listesi"},
        {"example", "https://… or #EXTM3U…", "https://… veya #EXTM3U…"},
        {"send", "Send to TV", "TV’ye gönder"},
        {"local", "This page works on your local network while the HızlıTV Player library is open. Nothing is sent to a cloud service.", "Bu sayfa yerel ağında, HızlıTV Player kütüphanesi açıkken çalışır. Buluta gönderim yapılmaz."},
        {"shortcut", "Send from the iPhone Share menu", "iPhone Paylaş menüsünden gönderme"},
        {"shortcut_intro", "Create a shortcut called “Send to HızlıTV”. Enable Show in Share Sheet and accept URLs.", "“HızlıTV’ye gönder” adlı bir kestirme oluştur. Paylaşım Sayfasında Göster’i aç ve URL girdisi kabul et."},
        {"shortcut_request", "Add Get Contents of URL: use this page’s full address, choose POST and a JSON body. Add a Text field named url with Shortcut Input as the value. Add the following header.", "URL’nin İçeriğini Al: bu sayfanın tam adresini kullan, POST ve JSON seç. url adlı Metin alanının değerini Kestirme Girdisi yap. Aşağıdaki başlığı ekle."},
        {"shortcut_use", "In Safari, choose Share → Send to HızlıTV. Keep this receiver address and token; you only need another QR code if the TV’s address changes or pairing is forgotten. Keep the token private.", "Safari’de Paylaş → HızlıTV’ye gönder’i seç. Alıcı adresini ve anahtarı sakla; yalnız TV’nin adresi değişirse veya eşleştirme unutulursa yeniden QR gerekir. Anahtarı paylaşma."},
        {"clipboard", "For a second shortcut that sends a copied link, use Get Clipboard as the url value. The clipboard is read only when you run the shortcut.", "Kopyalanan bağlantıyı gönderen ikinci bir kestirme için url değerinde Panoyu Al sonucunu kullan. Pano yalnız kestirme çalıştırılınca okunur."},
        {"empty", "Enter a link or playlist.", "Bir bağlantı veya liste gir."},
        {"sending", "Sending…", "Gönderiliyor…"},
        {"offline", "Could not reach the TV. Use the same Wi-Fi and open a new QR code on the TV.", "TV’ye ulaşılamadı. Aynı Wi-Fi’a bağlan ve TV’den yeni QR kod aç."},
        {"invalid_request", "Invalid request", "Geçersiz istek"},
        {"expired", "Pairing expired.", "Eşleştirme süresi doldu."},
        {"not_found", "Not found", "Bulunamadı"},
        {"not_supported", "Not supported", "Desteklenmiyor"},
        {"pairing", "Pairing required", "Eşleştirme gerekli"},
        {"origin", "Cross-origin request blocked", "Farklı kaynaktan istek engellendi"},
        {"content_type", "Plain text or JSON required", "Düz metin veya JSON gerekli"},
        {"too_large", "The link or playlist must be at most 2 MB.", "Bağlantı veya liste en fazla 2 MB olabilir."},
        {"json", "A string field named url is required in JSON.", "JSON içinde url metin alanı gerekli."},
        {"accepted", "Sent to the TV. Choose a source on the TV.", "TV’ye gönderildi. Kaynak seçimini TV’de yap."},
        {"busy", "The TV is processing another link. Try again shortly.", "TV başka bir bağlantıyı inceliyor. Biraz sonra tekrar dene."}
    };
    static boolean turkish(String accepted) {
        boolean selected = false; double best = -1;
        for (String range : accepted.toLowerCase(Locale.ROOT).split(",")) {
            String[] parts = range.trim().split(";"); String tag = parts[0].trim();
            if (!(tag.equals("tr") || tag.startsWith("tr-") || tag.equals("en") || tag.startsWith("en-"))) continue;
            double quality = 1;
            try { for (int i = 1; i < parts.length; i++) if (parts[i].trim().startsWith("q=")) quality = Double.parseDouble(parts[i].trim().substring(2)); }
            catch (NumberFormatException ignored) { continue; }
            if (quality > 0 && quality <= 1 && quality > best) { best = quality; selected = tag.startsWith("tr"); }
        }
        return selected;
    }
    static String text(String key, boolean turkish) {
        for (String[] row : TEXT) if (row[0].equals(key)) return row[turkish ? 2 : 1];
        throw new IllegalArgumentException("Unknown phone text");
    }
    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    static String render(String token, boolean turkish) throws Exception {
        JSONObject en = new JSONObject(), tr = new JSONObject();
        for (String[] row : TEXT) { en.put(row[0], row[1]); tr.put(row[0], row[2]); }
        String page = TEMPLATE.replace("[[TOKEN]]", token).replace("[[language]]", turkish ? "tr" : "en")
            .replace("[[DICTIONARIES]]", new JSONObject().put("en", en).put("tr", tr).toString());
        for (String[] row : TEXT) page = page.replace("[[" + row[0] + "]]", escape(row[turkish ? 2 : 1]));
        return page;
    }
    private static final String TEMPLATE =
        "<!doctype html><html lang=\"[[language]]\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n" +
        "<title>[[title]]</title><style>body{color:#e8f0fb;background:#101b2b;font:17px system-ui;margin:0;padding:28px;max-width:650px}h1{font-size:29px}p{color:#b6c8dc;line-height:1.5}textarea{box-sizing:border-box;width:100%;min-height:180px;background:#1d3049;color:white;border:1px solid #7facc0;border-radius:12px;padding:15px;font:16px system-ui}button,select{background:#99dfdc;color:#102235;border:0;border-radius:12px;padding:13px 18px;font:600 16px system-ui;margin-top:14px}#status{white-space:pre-wrap}code{overflow-wrap:anywhere}</style>\n" +
        "<select id=\"language\" aria-label=\"Language / Dil\"><option value=\"en\">English</option><option value=\"tr\">Türkçe</option></select>\n" +
        "<h1 data-text=\"title\">[[title]]</h1><p data-text=\"intro\">[[intro]]</p>\n" +
        "<textarea id=\"input\" aria-label=\"[[input]]\" placeholder=\"[[example]]\" maxlength=\"2000000\"></textarea><br><button id=\"send\" data-text=\"send\">[[send]]</button><p id=\"status\" role=\"status\"></p>\n" +
        "<p data-text=\"local\">[[local]]</p><details><summary data-text=\"shortcut\">[[shortcut]]</summary><p data-text=\"shortcut_intro\">[[shortcut_intro]]</p><p data-text=\"shortcut_request\">[[shortcut_request]]</p><p>X-HizliTV-Token: <code>[[TOKEN]]</code></p><p data-text=\"shortcut_use\">[[shortcut_use]]</p><p data-text=\"clipboard\">[[clipboard]]</p></details>\n" +
        "<script>const dictionaries=[[DICTIONARIES]],b=document.getElementById('send'),i=document.getElementById('input'),s=document.getElementById('status'),picker=document.getElementById('language');let language=document.documentElement.lang;\n" +
        "function applyLanguage(value){language=value==='tr'?'tr':'en';const t=dictionaries[language];document.documentElement.lang=language;document.title=t.title;picker.value=language;document.querySelectorAll('[data-text]').forEach(e=>e.textContent=t[e.dataset.text]);i.placeholder=t.example;i.setAttribute('aria-label',t.input)}\n" +
        "applyLanguage(language);picker.onchange=()=>applyLanguage(picker.value);\n" +
        "b.onclick=async()=>{const t=dictionaries[language];if(!i.value.trim()){s.textContent=t.empty;return}b.disabled=true;s.textContent=t.sending;try{const r=await fetch(location.pathname,{method:'POST',headers:{'Content-Type':'text/plain;charset=UTF-8','Accept-Language':language,'X-HizliTV-Token':'[[TOKEN]]'},body:i.value.trim()});s.textContent=await r.text();if(r.ok)i.value=''}catch(e){s.textContent=t.offline}finally{b.disabled=false}};</script></html>\n";
}
