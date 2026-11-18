package tr.hizlitv.player.options;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.ui.AspectRatioFrameLayout;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerOptionsTest {
    @Test public void menuOrderIsQualityAudioSubtitlesPictureSpeed() {
        java.util.List<PlayerOption> options = PlayerOptions.defaults();
        assertEquals(5, options.size());
        assertTrue(options.get(0) instanceof TrackOption); assertTrue(options.get(3) instanceof PictureOption); assertTrue(options.get(4) instanceof SpeedOption);
        try { options.add(new SpeedOption()); fail("the default list must not be changed at runtime"); } catch (UnsupportedOperationException expected) { }
    }
    @Test public void trackLabelsUseHeightBitrateLabelOrLanguage() {
        assertEquals("1080p · 5.2 Mbps", TrackOption.label(new Format.Builder().setHeight(1080).setAverageBitrate(5_200_000).setPeakBitrate(5_200_000).build(), C.TRACK_TYPE_VIDEO, "Option 1"));
        assertEquals("720p", TrackOption.label(new Format.Builder().setHeight(720).build(), C.TRACK_TYPE_VIDEO, "Option 1"));
        assertEquals("Türkçe", TrackOption.label(new Format.Builder().setLabel("Türkçe").setLanguage("tr").build(), C.TRACK_TYPE_AUDIO, "Option 1"));
        assertEquals("en", TrackOption.label(new Format.Builder().setLanguage("en").build(), C.TRACK_TYPE_TEXT, "Option 1"));
        assertEquals("Option 2", TrackOption.label(new Format.Builder().build(), C.TRACK_TYPE_AUDIO, "Option 2"));
    }
    @Test public void speedMultipliersAndPictureModes() {
        assertNull(SpeedOption.multiplier(1f));
        assertEquals("0.75x", SpeedOption.multiplier(0.75f)); assertEquals("1.5x", SpeedOption.multiplier(1.5f)); assertEquals("2x", SpeedOption.multiplier(2f));
        assertEquals(0, PictureOption.index(AspectRatioFrameLayout.RESIZE_MODE_FIT));
        assertEquals(2, PictureOption.index(AspectRatioFrameLayout.RESIZE_MODE_FILL));
        assertEquals(0, PictureOption.index(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)); // unknown modes fall back to Fit
    }
}
