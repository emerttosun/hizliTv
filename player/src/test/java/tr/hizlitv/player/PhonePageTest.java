package tr.hizlitv.player;
import org.junit.Test;
import static org.junit.Assert.*;
public class PhonePageTest {
    @Test public void languageNegotiationHonorsQualityAndUsesEnglishFallback() {
        assertFalse(PhonePage.turkish("")); assertFalse(PhonePage.turkish("fr-FR"));
        assertTrue(PhonePage.turkish("tr-TR,tr;q=0.9,en;q=0.8"));
        assertFalse(PhonePage.turkish("tr;q=0,en-US;q=0.6"));
        assertFalse(PhonePage.turkish("tr;q=0.1,en;q=0.9"));
        assertFalse(PhonePage.turkish("tr;q=NaN,en;q=0.5"));
    }
    @Test public void bothPagesRenderWithoutTemplateLeaksAndKeepThePairingProtocol() throws Exception {
        String token=PhonePortal.newToken();
        String en=PhonePage.render(token,false),tr=PhonePage.render(token,true);
        assertTrue(en.contains("lang=\"en\"")); assertTrue(tr.contains("lang=\"tr\""));
        assertTrue(en.contains("Send to TV")); assertTrue(tr.contains("TV’ye gönder"));
        assertFalse(en.contains("[[")); assertFalse(tr.contains("[["));
        assertTrue(en.contains("X-HizliTV-Token")); assertTrue(en.contains(token));
        assertTrue(tr.contains("location.pathname")); assertTrue(tr.contains("value=\"en\""));
    }
}
