# HızlıTV

[English](README.md) · [Türkçe](README.tr.md)

[![Android](https://github.com/emerttosun/hizliTv/actions/workflows/android.yml/badge.svg)](https://github.com/emerttosun/hizliTv/actions/workflows/android.yml)

A lightweight Android TV dashboard with a built-in video/IPTV player, a local phone sharing page, and optional device tools. Designed for remote controls and modest hardware such as the Mi TV Stick. **No root is required, and the stock HOME button stays unchanged.**

The app supports **English and Turkish**. It follows the device language when supported, falls back to English, and offers an app-only EN/TR language selector on the home screen. The phone sharing page also supports both languages.

![HızlıTV home screen](docs/images/home-en.png)

**Current version: 0.9.0.** The dashboard and Player are one APK: `tr.hizlitv.home`. AirPlay is an optional **separate** open-source companion; its pinned source, setup profiles and undo tool are included under [airplay](airplay/README.md).

## What it does

| Feature | Behavior |
| --- | --- |
| TV dashboard | Installed-app cards, favorites, clear DPAD focus and visible shortcuts |
| Built-in Player | Direct video links, HLS/DASH streams, M3U channel lists and a private library |
| Phone sharing | Scan a local QR page, paste a link/list, or use an iPhone Share Sheet shortcut |
| Source discovery | Looks for supported video sources and subtitle tracks on a supplied page, then asks before playing or saving |
| Playback controls | Source-provided quality, audio and subtitle options; black video surroundings; saved VOD positions |
| Memory panel | Current system RAM information, plus optional diagnostics and selected-app management |
| AirPlay shortcut | Opens the independent receiver; no AirPlay protocol code is linked into the main APK |

There are no accounts, cloud uploads, advertising SDKs or usage analytics. Your library, playback positions and phone pairing stay in app-private storage. One library change can be undone. Content providers, subscriptions and working channel lists are not supplied.

## Install it

**Cloning a repository downloads source code; it does not install an app.** Choose a route:

1. **Build an APK yourself** using the commands below or Android Studio.
2. **Use the debug APK from a successful GitHub Actions run:** open [Actions](https://github.com/emerttosun/hizliTv/actions/workflows/android.yml), choose a green run, and download its `hizlitv-debug-apk` artifact. GitHub may require signing in and delivers artifacts as an archive containing the APK. These are development builds, not a stable release channel.

If signed public releases are published, they will appear on [Releases](https://github.com/emerttosun/hizliTv/releases). Do not assume a release APK exists just because source code is available. Debug signatures can differ across machines or CI runs; see [updates and signing](docs/en/RELEASING.md) before replacing an existing installation.

[Installation guide](docs/en/INSTALLATION.md) · [Turkish setup](docs/BASLANGIC.md)

## Build from source

Requirements: **JDK 17**, Android SDK Platform 35 and Build Tools 35.0.0. Android Studio can install the SDK components. The checked-in Gradle Wrapper selects Gradle; you do not need a global Gradle installation.

```sh
git clone https://github.com/emerttosun/hizliTv.git hizlitv
cd hizlitv
./gradlew :launcher:assembleDebug
```

On Windows use `gradlew.bat`. The single app APK is:

```text
launcher/build/outputs/apk/debug/launcher-debug.apk
```

Set your SDK location through Android Studio, `ANDROID_HOME`, or your own ignored `local.properties`. Signing keys and personal SDK paths are not part of this repository. The `player` module is an Android library, not a second installable app.

For a TV that supports classic network ADB, after enabling debugging and approving your computer:

```sh
adb connect TV_IP:5555
adb devices
adb -s TV_SERIAL install -r launcher/build/outputs/apk/debug/launcher-debug.apk
adb -s TV_SERIAL shell am start -n tr.hizlitv.home/.MainActivity
```

Replace `TV_IP` and `TV_SERIAL` with your own device. Wireless debugging varies by Android version and vendor; port 5555 is not universal. Start with the [beginner guide](docs/en/GETTING_STARTED.md) if these tools are new to you.

## First use

1. Open HızlıTV from the TV’s normal app list. Use directional keys and OK; hold OK to pin/unpin an app.
2. Choose **Player → Add from phone**, scan the QR code, and keep both devices on the same local network.
3. Paste a supported video page, direct stream or M3U text/URL. Choose **Play**, **Add to library**, or **Add playlist** on the TV.
4. For repeat iPhone sharing, create the documented [Share Sheet shortcut](docs/en/IPHONE.md). Automatic background clipboard synchronization is not implemented.

**Keep the Player library screen open when sending.** Its local HTTP listener stops when the library is no longer visible, including while a video is playing. It is not an always-on cloud casting service. Treat the QR address/token as a private pairing credential; use a trusted local network.

## IPTV and supported sources

You can send M3U text or a playlist URL, filter entries by group, search, mark favorites, and play channels in the built-in player. See the [playlist example](examples/kanallar.m3u) and [IPTV guide](docs/en/IPTV.md). The example uses placeholder URLs and is not a working channel package.

The resolver reads supported HTML/video/source/track elements and bounded iframe links. It does not execute arbitrary page scripts. Not every video site works: DRM, sign-in and CAPTCHA bypass are not implemented. Ads embedded in a stream are not removed. Source availability, codecs and network/server quality affect playback; “never buffers” or universal site compatibility is not promised. EPG and scheduled playlist refresh are not implemented.

## Optional AirPlay companion

[AirPlay Server for Android](https://github.com/jqssun/android-airplay-server) by jqssun provides the receiver. **Its source is pinned as a Git submodule**, not claimed as original HızlıTV protocol code. Initialize it when you need the receiver source:

```sh
git submodule update --init --recursive airplay/receiver
```

[AirPlay setup](airplay/README.md) includes a checksum-verified download of the tested upstream APK, balanced/low-latency profiles, explicit application and per-step undo records. Installing the main app does not install or configure the receiver automatically. It remains a separate GPL-3.0 app with its own build requirements, permissions and update/signing identity. DRM-protected Apple content is not supported by that receiver.

## Permissions and device support

The main app targets **Android TV 6 / API 23 and later**, with real-device testing on an **Android 11 Mi TV Stick**. Other devices, Android versions and codecs need their own testing. The separate receiver’s compatibility is described in its upstream documentation.

| Operation | Additional authorization |
| --- | --- |
| Dashboard, Player, QR sharing and total RAM | No diagnostic grants or root required |
| Detailed process list | Optional `DUMP` and usage-access grants; support depends on Android/vendor |
| Directly stop selected apps | Optional, explicitly approved on-device ADB pairing; broader device control |
| Memory cleanup | Limited by Android; apps can restart, and Android 14+ restricts cross-app cleanup |

These privileges are **not granted automatically** on another user’s TV. RAM occupancy includes useful caching and is not itself proof of a performance problem. Read [Security and permissions](SECURITY.md) before enabling advanced tools.

## How the code is organized

| Module / folder | Responsibility |
| --- | --- |
| `launcher` | The single app package, installed apps/favorites and optional device tools |
| `player` | Embedded library, local sharing, source adapters and Media3 playback |
| `media-core` | Plain-Java media models, M3U parsing, URL checks and search indexing |
| `tv-ui` | Native colors/buttons/dialogs and English/Turkish localization |
| `legacy-bridge` | Optional same-signature migration from the former two-APK setup |
| `airplay` | Pinned upstream receiver, attribution and reversible integration tooling |

Views use native Android components, static gradients and reusable list rows. File operations and source resolution run off the UI thread. ExoPlayer is created when playback starts and released when it stops. No WebView-based TV interface, remote artwork service or continuous background animation is needed.

## Learn and contribute

| English | Türkçe |
| --- | --- |
| [Beginner concepts and first build](docs/en/GETTING_STARTED.md) | [Temel kavramlar ve ilk derleme](docs/BASLANGIC.md) |
| [Installation](docs/en/INSTALLATION.md) | [Kurulum](docs/BASLANGIC.md) |
| [Architecture](docs/en/ARCHITECTURE.md) | [Mimari](docs/MIMARI.md) |
| [Design](docs/en/DESIGN.md) | [Tasarım](docs/TASARIM.md) |
| [Remote-control usage](docs/en/USAGE.md) | [Kumandayla kullanım](docs/KULLANIM.md) |
| [iPhone and Shortcuts](docs/en/IPHONE.md) | [iPhone ve Kestirmeler](docs/IPHONE.md) |
| [IPTV / M3U](docs/en/IPTV.md) | [IPTV / M3U](docs/IPTV.md) |
| [Testing and limits](docs/en/TESTING.md) | [Doğrulama ve sınırlar](docs/TEST.md) |
| [Publishing and signing](docs/en/RELEASING.md) | [Yayınlama ve imzalama](docs/YAYINLAMA.md) |
| [Old Player migration](docs/en/MIGRATION.md) | [Eski Player’dan geçiş](docs/GECIS.md) |
| [Security](SECURITY.md) | [Güvenlik](docs/SECURITY.tr.md) |
| [Contributing](CONTRIBUTING.md) | [Katkı rehberi](docs/CONTRIBUTING.tr.md) |
| [AirPlay](airplay/README.md) | [AirPlay](airplay/README.tr.md) |

Run the checks before submitting a change:

```sh
python3 scripts/check_public.py
python3 scripts/check_translations.py
python3 -m unittest discover -s airplay/tests
./gradlew :media-core:test :player:testDebugUnitTest :launcher:lintDebug :player:lintDebug :tv-ui:lintDebug :launcher:assembleDebug
```

GitHub Actions checks the main app and integration tooling. It does not test a physical TV/iPhone or build the entire upstream AirPlay native stack.

## License

Original HızlıTV code is **MIT**. Bundled AdbLib is **BSD-3-Clause**; Media3/ZXing are **Apache-2.0**, and jsoup is **MIT**. The separate AirPlay receiver is **GPL-3.0**, with its upstream native dependencies retaining their own licenses. See [NOTICE](NOTICE) and [receiver attribution](airplay/README.md). This independent project is not affiliated with Apple, Samsung or Xiaomi.
