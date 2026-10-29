package tr.hizlitv.player;
import org.junit.Test;
import static org.junit.Assert.*;
public class SourceAdapterTest {
    @Test public void syntheticRotatingFormatDecodesWithoutPrivateSiteFixtures() throws Exception {
        String html = "/* location % 241 % 65519 */ var rotating_123 = decode(\"wavI^yz6j^89G4^3v93^dntL^synthetic^PbSi^f5D5^sXVz^sW4t^7^R9L1^kh3u^/A==\".split(\"^\")); sources: [{ file: rotating_123 }]";
        assertEquals("https://media.example.org/demo.mp4", ClosePageDecoder.decode(html, "hdfilmcehennemi.mobi"));
        assertEquals("", ClosePageDecoder.decode(html, "example.org"));
        assertEquals("", ClosePageDecoder.decode("changed page format", "hdfilmcehennemi.mobi"));
    }
}
