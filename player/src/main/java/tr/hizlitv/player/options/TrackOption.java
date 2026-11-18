package tr.hizlitv.player.options;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import tr.hizlitv.ui.R;

/**
 * Quality, audio or subtitles, built from what the source actually offers. Unsupported or missing
 * tracks are never invented; the entry is hidden when the source has none of that type.
 */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class TrackOption implements PlayerOption {
    private final int type;
    TrackOption(int type) { this.type = type; }

    /** Row text for one track: "1080p · 5.2 Mbps" for video, the label or language otherwise. */
    static String label(Format f, int type, String fallback) {
        String label = f.label != null ? f.label : f.language != null ? f.language : fallback;
        if (type == C.TRACK_TYPE_VIDEO) {
            if (f.height > 0) label = f.height + "p";
            if (f.bitrate > 0) label += String.format(Locale.US, " · %.1f Mbps", f.bitrate / 1_000_000d);
        }
        return label;
    }
    private boolean hasTracks(Host host) {
        for (Tracks.Group g : host.player().getCurrentTracks().getGroups())
            if (g.getType() == type) for (int i = 0; i < g.length; i++) if (g.isTrackSupported(i)) return true;
        return false;
    }
    @Override public boolean available(Host host) { return hasTracks(host); }
    @Override public String title(Host host) {
        return host.context().getString(type == C.TRACK_TYPE_VIDEO ? R.string.text_quality : type == C.TRACK_TYPE_AUDIO ? R.string.text_audio_language : R.string.text_subtitles);
    }
    @Override public List<Choice> choices(Host host) {
        TrackSelectionParameters params = host.player().getTrackSelectionParameters();
        boolean disabled = params.disabledTrackTypes.contains(type), overridden = false;
        List<Choice> tracks = new ArrayList<>();
        for (Tracks.Group g : host.player().getCurrentTracks().getGroups()) {
            if (g.getType() != type) continue;
            TrackSelectionOverride current = params.overrides.get(g.getMediaTrackGroup());
            for (int i = 0; i < g.length; i++) {
                if (!g.isTrackSupported(i)) continue;
                boolean chosen = !disabled && current != null && current.trackIndices.contains(i); overridden |= chosen;
                String fallback = host.context().getString(R.string.text_option) + (tracks.size() + 1);
                TrackSelectionOverride override = new TrackSelectionOverride(g.getMediaTrackGroup(), Collections.singletonList(i));
                tracks.add(new Choice(label(g.getTrackFormat(i), type, fallback), chosen, () -> select(host, override, false)));
            }
        }
        List<Choice> out = new ArrayList<>();
        if (type == C.TRACK_TYPE_TEXT) {
            out.add(new Choice(host.context().getString(R.string.text_subtitles_off), disabled, () -> select(host, null, true)));
            out.add(new Choice(host.context().getString(R.string.text_automatic_preferred_language), !disabled && !overridden, () -> select(host, null, false)));
        } else out.add(new Choice(host.context().getString(R.string.text_automatic), !overridden, () -> select(host, null, false)));
        out.addAll(tracks);
        return out;
    }
    private void select(Host host, TrackSelectionOverride override, boolean off) {
        TrackSelectionParameters.Builder next = host.player().getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type, off);
        if (override != null) next.addOverride(override);
        host.player().setTrackSelectionParameters(next.build());
    }
}
