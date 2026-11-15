package tr.hizlitv.player;

import java.util.Locale;
import org.json.JSONObject;

/** Independent bilingual phone page; the TV pairing protocol is unchanged. */
final class PhonePage {
    private static final String[][] TEXT = {
        {"title", "Send to HızlıTV", "HızlıTV’ye gönder"},
        {"intro", "Paste a link or channel list, then confirm on the TV.", "Bağlantıyı veya kanal listesini yapıştır, sonra TV’de onayla."},
        {"input", "Link or channel list", "Bağlantı veya kanal listesi"},
        {"example", "https://… or #EXTM3U…", "https://… veya #EXTM3U…"},
        {"send", "Send to TV", "TV’ye gönder"},
        {"address", "Receiver address", "Alıcı adresi"},
        {"copy_address", "Copy address", "Adresi kopyala"},
        {"copy_token", "Copy token", "Anahtarı kopyala"},
        {"copied", "Copied", "Kopyalandı"},
        {"copy_failed", "Copy failed; select the text instead.", "Kopyalanamadı; metni seçerek kopyala."},
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
        "<title>[[title]]</title><style>body{color:#e8f0fb;background:#101b2b;font:17px system-ui;margin:0 auto;padding:20px;max-width:650px}" +
        "header{display:flex;align-items:center;gap:12px}h1{font-size:26px;margin:8px 0;flex:1}p{color:#b6c8dc;line-height:1.5}" +
        "textarea{box-sizing:border-box;width:100%;min-height:130px;background:#1d3049;color:white;border:1px solid #7facc0;border-radius:12px;padding:14px;font:16px system-ui}" +
        "button,select{background:#99dfdc;color:#102235;border:0;border-radius:12px;padding:12px 16px;font:600 16px system-ui}" +
        "select{background:#1d3049;color:#e8f0fb;padding:8px 10px;font-size:14px}#send{width:100%;margin-top:12px;padding:16px;font-size:18px}#send:disabled{opacity:.6}" +
        "#status{white-space:pre-wrap;min-height:1.5em;font-weight:600}#status.ok{color:#99dfdc}#status.err{color:#ffb4a8}" +
        ".small{font-size:14px}.copy{background:#1d3049;color:#e8f0fb;border:1px solid #7facc0;padding:8px 12px;font-size:14px;margin:4px 0}code{overflow-wrap:anywhere}summary{cursor:pointer;color:#e8f0fb}</style>\n" +
        "<header><h1 data-text=\"title\">[[title]]</h1><select id=\"language\" aria-label=\"Language / Dil\"><option value=\"en\">English</option><option value=\"tr\">Türkçe</option></select></header>\n" +
        "<p data-text=\"intro\">[[intro]]</p>\n" +
        "<textarea id=\"input\" aria-label=\"[[input]]\" placeholder=\"[[example]]\" maxlength=\"2000000\" autofocus></textarea><button id=\"send\" data-text=\"send\">[[send]]</button><p id=\"status\" role=\"status\"></p>\n" +
        "<p class=\"small\" data-text=\"local\">[[local]]</p><details><summary data-text=\"shortcut\">[[shortcut]]</summary><p data-text=\"shortcut_intro\">[[shortcut_intro]]</p><p data-text=\"shortcut_request\">[[shortcut_request]]</p>\n" +
        "<p><span data-text=\"address\">[[address]]</span>: <code id=\"address\"></code><br><button class=\"copy\" id=\"copy-address\" data-text=\"copy_address\">[[copy_address]]</button></p>\n" +
        "<p>X-HizliTV-Token: <code>[[TOKEN]]</code><br><button class=\"copy\" id=\"copy-token\" data-text=\"copy_token\">[[copy_token]]</button></p>\n" +
        "<p data-text=\"shortcut_use\">[[shortcut_use]]</p><p data-text=\"clipboard\">[[clipboard]]</p></details>\n" +
        "<script>const dictionaries=[[DICTIONARIES]],token='[[TOKEN]]',b=document.getElementById('send'),i=document.getElementById('input'),s=document.getElementById('status'),picker=document.getElementById('language'),address=location.origin+location.pathname;let language=document.documentElement.lang;\n" +
        "document.getElementById('address').textContent=address;\n" +
        "function applyLanguage(value){language=value==='tr'?'tr':'en';const t=dictionaries[language];document.documentElement.lang=language;document.title=t.title;picker.value=language;document.querySelectorAll('[data-text]').forEach(e=>e.textContent=t[e.dataset.text]);i.placeholder=t.example;i.setAttribute('aria-label',t.input)}\n" +
        "applyLanguage(language);picker.onchange=()=>applyLanguage(picker.value);\n" +
        "function show(text,ok){s.textContent=text;s.className=ok?'ok':'err'}\n" +
        "function copy(text,button){const t=dictionaries[language],done=ok=>{button.textContent=ok?t.copied:t.copy_failed;setTimeout(()=>button.textContent=dictionaries[language][button.dataset.text],1800)};" +
        "const a=document.createElement('textarea');a.value=text;a.setAttribute('readonly','');a.style.position='fixed';a.style.opacity='0';document.body.appendChild(a);a.select();a.setSelectionRange(0,text.length);let ok=false;try{ok=document.execCommand('copy')}catch(e){}a.remove();done(ok)}\n" +
        "document.getElementById('copy-address').onclick=e=>copy(address,e.target);document.getElementById('copy-token').onclick=e=>copy(token,e.target);\n" +
        "b.onclick=async()=>{const t=dictionaries[language];if(!i.value.trim()){show(t.empty,false);return}b.disabled=true;show(t.sending,true);try{const r=await fetch(location.pathname,{method:'POST',headers:{'Content-Type':'text/plain;charset=UTF-8','Accept-Language':language,'X-HizliTV-Token':token},body:i.value.trim()});show(await r.text(),r.ok);if(r.ok)i.value=''}catch(e){show(t.offline,false)}finally{b.disabled=false}};</script></html>\n";
}
