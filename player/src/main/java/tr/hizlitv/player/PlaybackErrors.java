package tr.hizlitv.player;

import androidx.media3.common.PlaybackException;

/** Maps Media3 error codes to a few user-facing causes; plain Java so it is unit tested. */
final class PlaybackErrors {
    enum Kind { HTTP, NETWORK, FORMAT, DRM, OTHER }
    private PlaybackErrors() { }
    /** Connection problems that are worth a quiet retry once a stream has already played. */
    static boolean network(int code) {
        return code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            || code == PlaybackException.ERROR_CODE_TIMEOUT || code == PlaybackException.ERROR_CODE_IO_UNSPECIFIED;
    }
    static Kind kind(int code) {
        if (code == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS || code == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            || code == PlaybackException.ERROR_CODE_IO_NO_PERMISSION || code == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE) return Kind.HTTP;
        if (network(code)) return Kind.NETWORK;
        if (code >= 6000 && code < 7000) return Kind.DRM;
        if (code >= 3000 && code < 5000) return Kind.FORMAT;
        return Kind.OTHER;
    }
}
