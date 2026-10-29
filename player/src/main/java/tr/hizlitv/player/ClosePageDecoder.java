package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONTokener;

/** Static URL transformation for the observed Close player, not a JavaScript interpreter.
 * No eval, DOM, ads, credential access or DRM decryption. A changed format must fail closed. */
final class ClosePageDecoder {
    static String decode(String html, String host) throws Exception {
        if (!host.equals("hdfilmcehennemi.mobi")) return "";
        if (!html.contains("% 241") || !html.contains("% 65519") || !html.contains("location")) return "";
        // Both the variable names and separator change on every page request.
        // Follow only the identifier explicitly referenced by the player's sources.
        Matcher source = Pattern.compile("\\bsources\\s*:\\s*\\[\\s*\\{\\s*file\\s*:\\s*([A-Za-z_$][\\w$]*)").matcher(html);
        if (!source.find()) return "";
        Matcher m = Pattern.compile("var\\s+" + Pattern.quote(source.group(1)) +
            "\\s*=\\s*[\\w$]+\\((\"(?:[^\"\\\\]|\\\\.)*\")\\.split\\((\"(?:[^\"\\\\]|\\\\.)*\")\\)\\)").matcher(html);
        if (!m.find()) return "";
        String literal = (String)new JSONTokener(m.group(1)).nextValue();
        String separator = (String)new JSONTokener(m.group(2)).nextValue();
        if (separator.isEmpty() || separator.length() > 4) return "";
        if (literal.length() > 65536) throw new IllegalArgumentException("Kaynak verisi sınırı aşıldı.");
        ArrayList<String> chunks = new ArrayList<>(java.util.Arrays.asList(literal.split(Pattern.quote(separator), -1)));
        int n = chunks.size() - 2;
        if (n < 10 || n > 4096) return "";
        String steps = chunks.remove(8 + n % 5);
        String seed = chunks.remove(n % 7) + host;
        if (!steps.matches("[a-z37]{1,32}")) return "";
        StringBuilder joined = new StringBuilder(); for (String s : chunks) joined.append(s);
        String value = joined.toString(); int hash = 0, sum = 0;
        for (int i = 0; i < seed.length(); i++) {
            int c = seed.charAt(i); hash = (hash * 37 + c) % 241; sum = (sum + ((c << 1) ^ i)) & 255;
        }
        int state = (hash * 3 + sum) % 256, step = sum % 11 + 5;
        long random = (sum * 251 + hash) % 65519 + 1;
        for (int i = steps.length() - 1; i >= 0; i--) {
            char c = steps.charAt(i);
            if (c == '7') value = base64(value);
            else if (c == '3') value = new StringBuilder(value).reverse().toString();
            else {
                int shift = (26 - (c - 96) % 26) % 26; StringBuilder rotated = new StringBuilder();
                for (char x : value.toCharArray()) {
                    int base = x >= 'A' && x <= 'Z' ? 65 : x >= 'a' && x <= 'z' ? 97 : -1;
                    rotated.append(base < 0 ? x : (char)((x - base + shift) % 26 + base));
                }
                value = rotated.toString();
            }
        }
        int[] indexes = new int[value.length()];
        for (int i = value.length() - 1; i >= 1; i--) { random = (random * 97 + 41) % 65519; indexes[i] = (int)(random % (i + 1)); }
        char[] chars = value.toCharArray();
        for (int i = 1; i < chars.length; i++) { int j = indexes[i]; char c = chars[i]; chars[i] = chars[j]; chars[j] = c; }
        StringBuilder result = new StringBuilder();
        for (char c : chars) { state = (state * 5 + step) % 256; result.append((char)(c ^ state)); state = (state + c) % 256; }
        return Urls.http(result.toString());
    }
    private static String base64(String text) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int acc = 0, bits = 0;
        for (char c : text.toCharArray()) {
            if (c == '=') break;
            int digit = alphabet.indexOf(c); if (digit < 0) throw new IllegalArgumentException("Kaynak biçimi değişmiş.");
            acc = (acc << 6) | digit; bits += 6;
            if (bits >= 8) { bits -= 8; bytes.write((acc >> bits) & 255); }
        }
        return new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
    }
}
