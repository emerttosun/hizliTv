# IPTV playlists and video links

[English](IPTV.md) · [Türkçe](../IPTV.md)

The same built-in Player handles channels and individual videos. No subscription or working channel package is included. Use sources you are authorized to access.

## M3U format

```m3u
#EXTM3U
#EXTINF:-1 group-title="News",Example News
https://media.example.org/news/master.m3u8
#EXTINF:-1 group-title="Films",Example Film
https://media.example.org/film.mp4
```

These are placeholders. Replace them with your own working source URLs. `#EXTINF` supplies the displayed name; `group-title` or `#EXTGRP` supplies a group. A comma inside a quoted attribute is not treated as the title separator. Relative links can be resolved against a playlist URL.

Supported optional headers include `#EXTVLCOPT:http-referrer=` and `#EXTVLCOPT:http-user-agent=`. They are validated for control-character injection. A provider's authentication values can still be sensitive; never commit your real list or account URL to Git.

## Import

Use Player → Add from phone and paste M3U text or a playlist URL. Alternatively use Link/playlist on the TV. Review the channel count and choose Add playlist. Filter by group, search, favorite entries, and press OK to confirm playback.

Reimporting the same URL updates that entry and preserves its favorite status. A different URL can create a new entry; remove old ones with hold OK. Undo reverts only the most recent library change. There is no scheduled provider refresh or EPG feature.

The input/page limit is 2 MB, and the private library is capped at 5000 entries / 8 MB. HLS media manifests and channel playlists can both use `.m3u8`; the resolver examines content to distinguish them. An HLS manifest is played as video, not imported as a channel list.

## Individual pages and tracks

Direct MP4/HLS/DASH links and supported HTML source pages can be sent from the same phone page. The app asks before playing or saving. A saved page can be resolved again because extracted media URLs may expire.

The resolver reads supported HTML elements/metadata and limited iframe chains. It does not run arbitrary JavaScript, bypass login/CAPTCHA/DRM, or remove ads embedded in a stream. Subtitle, audio and quality choices must actually be present and supported. Network/server issues and device codecs can still cause playback problems.

See [source architecture](ARCHITECTURE.md) for adapters and limits, and [examples/kanallar.m3u](../../examples/kanallar.m3u) for a small template.
