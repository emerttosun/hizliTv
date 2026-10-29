# Verification and practical limits

[English](TESTING.md) · [Türkçe](../TEST.md)

Version **0.9.0**, embedded Player / single APK. Recorded on 29 October 2026.

## Automated checks

```sh
python3 scripts/check_public.py
python3 scripts/check_translations.py
python3 -m unittest discover -s airplay/tests
./gradlew :media-core:test :player:testDebugUnitTest
./gradlew :launcher:lintDebug :player:lintDebug :tv-ui:lintDebug
./gradlew :launcher:assembleDebug :launcher:assembleRelease
./gradlew -PincludeLegacyBridge=true :legacy-bridge:lintDebug :legacy-bridge:assembleDebug
```

The main JVM suite has **18 passing tests** (8 media-core, 10 player), with no failures or skipped tests. The AirPlay integration tool has **3 passing Python tests**. Android lint and debug/release assembly pass locally. The translation checker verifies **226 matching English/Turkish entries**, including plural forms and format placeholders. These counts describe this version, not all future commits.

| Area | Evidence |
| --- | --- |
| M3U and URL checks | Groups/titles, quoted commas, relative URLs, stream headers, invalid entries and a 5000-entry limit; rejects header injection |
| Search | Group/favorite intersections, unchanged source list and English/Turkish case folding |
| Resolver | Synthetic local HTML/iframe/video/subtitle fixtures, playlist versus HLS classification, HTTP errors and injectable source adapters |
| Phone server | Token/origin validation, JSON, stable restart and redacted diagnostics |
| Phone localization | Accept-Language quality negotiation, English fallback and both rendered pages preserving the token protocol |
| AirPlay profiles | Validated settings/PIN/volume invariants, rejected invalid settings, partial-failure undo state |

Socket tests bind temporary loopback ports. They do not require real subscription lists, provider accounts or private movie-page responses. JVM `org.json` is a test dependency; Android supplies its implementation at runtime.

## Physical-device evidence

The reference device is an Android 11 Mi TV Stick. Earlier 0.8.1/0.8.3 versions were installed with the existing signing identity and data retained. Their actual screenshots are included in the repository.

Verified on that device:

- The Player card opens an Activity inside `tr.hizlitv.home`; HOME still resolves to the stock launcher.
- Migration retained five playback positions and the phone pairing/port; the old library was empty. The migration provider rejected a shell caller with a different signing identity.
- Phone-protocol POST returned HTTP202 and opened a native confirmation. Acceptance alone was not treated as playback success.
- A synthetic two-channel M3U import displayed two entries; Undo returned to the original empty library.
- Source, playlist, multi-source and error dialogs matched the shared TV style. Initial Play focus, directional movement and a single OK on the source chooser were checked.
- A public ExoPlayer sample reached its first video-frame callback. A short 320×180 sample rendered 141 frames with zero dropped frames in one 0.8.1 run. This is not a long-duration or 1080p playback guarantee. Hardware video surfaces can appear black in screenshots; a black PNG alone does not prove success or failure.
- Returning from playback released the player; leaving the library closed the phone listener.

Version 0.9.0 was also installed on that device with the original signing identity. The dashboard, embedded library and source confirmation were checked in both English and Turkish. The English playlist confirmation and invalid-link message, initial Play focus, both phone-page language responses and the existing pairing address were verified. System language was restored afterward; the library stayed empty and stock HOME stayed assigned. Current screenshots show this version. Android TV dialog buttons focus on the first injected touch, so confirmations were completed using the real DPAD/OK path rather than treating a focus change as a click.

The user previously confirmed real iPhone sharing, picture and sound. The phone-page language/protocol checks in 0.9.0 used an HTTP client; they do not constitute a fresh physical-iPhone test. Audio/subtitle combinations, additional senders and long-duration playback need their own checks.

## Performance evidence

The home screen samples system RAM only while visible and cancels callbacks when paused. Library disk/index work uses a serial background worker; list rows are reused. Search is debounced by 150 ms. The phone listener does not run behind playback. ExoPlayer is allocated only for video playback and released on exit.

A previous short device sample, before localization:

| Measurement | Result |
| --- | --- |
| Home cold start, three samples | 621 / 621 / 633ms; median 621 ms |
| Embedded Player Displayed event, two samples | 133 / 146ms |
| Home process PSS | About 63 MiB |
| Library process PSS | About 49 MiB |
| 20 spaced directional keys after focus-frame adjustment | 4 late frames out of 33; p90 31 ms |

Cold-start timing used force-stop of our own app followed by `am start -W`. Memory is process PSS from `dumpsys meminfo`, not the RAM panel's system-wide occupancy. These small samples are not statistically controlled benchmarks. Temperature, network, source quality, Android caches and other processes vary. A separate rapid 60-key stress test was slower; no 60 fps or zero-stutter guarantee is made.

## Distribution scope

GitHub Actions runs the same main checks, optional migration-tool build and Python profile checks. Inspect the actual run status rather than inferring success from a workflow file. It does not connect to a physical TV/iPhone or compile the entire upstream AirPlay native stack.

Android 6/API 23 compatibility is targeted by compilation and lint; the physical reference device runs Android 11. Other Android versions, vendors, sender versions, codecs and source sites remain separate compatibility work. The app cannot remove slow source servers or guarantee every page exposes a playable stream.

The 0.9.0 on-device playback check also reached `video_first_frame` and `player_ready` in the embedded Player. The public 320×180 sample rendered 3463 frames with zero dropped frames during this run. Its first-frame event arrived about 8.6 seconds after the play request, including source/network preparation; a 7-second observation was too short. This is one sample, not a startup-speed or codec guarantee. The sample was not added to the library.
