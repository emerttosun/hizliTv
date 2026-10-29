# Install HızlıTV on Android TV or a Mi TV Stick

[English](INSTALLATION.md) · [Türkçe](../BASLANGIC.md)

The dashboard and Player install together as **one APK**. The optional AirPlay receiver is a separate app. Android TV 6/API 23 and later are targeted; the tested physical device runs Android 11. Other vendors/codecs require their own checks.

## Obtain an APK

Build it using the [beginner guide](GETTING_STARTED.md), or download the `hizlitv-debug-apk` artifact from a successful [GitHub Actions run](https://github.com/emerttosun/hizliTv/actions/workflows/android.yml). GitHub may require an account to download artifacts and wraps them in an archive. Extract the APK before installing it. A source-code archive is not an installer.

Development APK signatures can change between machines/runs. A future stable public release must use a consistent owner-managed release identity. Check [Releases](https://github.com/emerttosun/hizliTv/releases) for actually published versions; source availability alone does not mean a signed release has been published.

## Connect your own TV

On the TV, enable developer options by repeatedly pressing Build in About, then enable the debugging option your device provides. Accept the authorization request yourself. TV/computer must be on the same trusted network for network ADB.

For a device that exposes classic network ADB:

```sh
adb connect TV_IP:5555
adb devices
```

Use the TV's actual address. Not every vendor exposes port 5555. Newer wireless-debugging implementations may require `adb pair ADDRESS:PAIRING_PORT` followed by a separate connection address/port shown on the TV. `unauthorized` means approval is still missing.

Install the built APK:

```sh
adb -s TV_SERIAL install -r launcher/build/outputs/apk/debug/launcher-debug.apk
adb -s TV_SERIAL shell am start -n tr.hizlitv.home/.MainActivity
```

`TV_SERIAL` is the identifier from `adb devices`. `-r` updates an existing matching-signed installation; it does not bypass signature checks. HızlıTV appears in the normal TV app list. HOME still opens the stock launcher.

## First run and language

Select EN/TR on the dashboard to choose System language, English or Türkçe. The preference applies to HızlıTV only. Unsupported system languages use English. A fresh installation follows the supported device language automatically.

Open Player, choose Add from phone, scan the QR code, and keep the library visible while sending. Confirm playback or playlist import on the TV. [iPhone sharing](IPHONE.md) and [IPTV](IPTV.md) cover the rest.

Core use requires no root, DUMP or local-ADB pairing. Detailed diagnostics and direct app closure require the separate optional authorizations described in [security](../../SECURITY.md). Do not grant advanced permissions just to play a video.

## AirPlay and existing installations

Use the separate [AirPlay guide](../../airplay/README.md) for its original receiver APK, pinned source and reversible profiles. Main-app installation does not apply those profiles or enable receiver autostart.

If you used the former separate `tr.hizlitv.player` app, read [migration](MIGRATION.md) before deleting or updating it. Private app data cannot be read by an unrelated APK, even if both apps came from a similar-looking repository.
