package tr.hizlitv.player;

import androidx.media3.common.PlaybackException;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackErrorsTest {
    @Test public void errorCodesMapToUserFacingCauses() {
        assertEquals(PlaybackErrors.Kind.HTTP, PlaybackErrors.kind(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS));
        assertEquals(PlaybackErrors.Kind.NETWORK, PlaybackErrors.kind(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED));
        assertEquals(PlaybackErrors.Kind.FORMAT, PlaybackErrors.kind(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED));
        assertEquals(PlaybackErrors.Kind.FORMAT, PlaybackErrors.kind(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED));
        assertEquals(PlaybackErrors.Kind.DRM, PlaybackErrors.kind(PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR));
        assertEquals(PlaybackErrors.Kind.OTHER, PlaybackErrors.kind(PlaybackException.ERROR_CODE_UNSPECIFIED));
    }
    @Test public void onlyConnectionDropsAreRetriedQuietly() {
        assertTrue(PlaybackErrors.network(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT));
        assertFalse(PlaybackErrors.network(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS));
        assertFalse(PlaybackErrors.network(PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR));
    }
}
