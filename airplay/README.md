# AirPlay companion

[English](README.md) · [Türkçe](README.tr.md)

HızlıTV opens **AirPlay Server for Android**, an independent open-source receiver by **jqssun and upstream contributors**. Its source is committed as a pinned Git submodule at [receiver](receiver); it is **not linked into HızlıTV’s MIT-licensed launcher/player APK**. The receiver remains GPL-3.0. See [RECEIVER-LICENSE](RECEIVER-LICENSE) and [upstream.json](upstream.json).

The pinned version is **v0.0.36**, commit `a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea`. It is the upstream version previously tested with an iPhone/iPad on the project’s Android 11 Mi TV Stick. No receiver protocol or binary has been relabeled as HızlıTV’s original work.

## Install the tested receiver

1. Connect to your own TV with ADB and approve its debugging request. See the [installation guide](../docs/en/INSTALLATION.md).
2. Download the pinned, original APK and verify its SHA256 automatically:

   ```sh
   python3 airplay/manage.py download
   ```

3. Install and open it:

   ```sh
   adb -s TV_SERIAL install -r airplay/.cache/AirPlay-0.0.36.apk
   adb -s TV_SERIAL shell am start -n io.github.jqssun.airplay/.MainActivity
   ```

4. Keep your Apple device and receiver on the same local network. Open Screen Mirroring on the iPhone/iPad or Mac and choose the receiver’s displayed name. Enter the on-screen PIN if requested.

`TV_SERIAL` is the device identifier shown by `adb devices`. The download retains upstream’s signature. If an existing receiver has a different signing identity, stop and resolve that mismatch instead of deleting its data. The public Git repository contains source references and profiles, not an embedded receiver APK.

## Optional profiles and undo

Profiles are explicit setup choices. Installing or opening HızlıTV does not apply them automatically.

| Profile | Settings |
| --- | --- |
| `balanced` | 1080p / 30 FPS limit, automatic audio buffering, PIN required |
| `low-latency` | Same video limit, low-latency option, fixed 20 ms audio cushion, PIN required |

Both disable sender-driven system volume synchronization. Neither profile changes the TV’s system volume directly. A smaller buffer can stutter on an unstable network; low-latency mode is not a zero-latency guarantee.

Preview without connecting to a device:

```sh
python3 airplay/manage.py apply --profile balanced --dry-run
```

Apply only to the device you authorized:

```sh
python3 airplay/manage.py --serial TV_SERIAL status
python3 airplay/manage.py --serial TV_SERIAL apply --profile balanced --name LivingRoom
```

Each apply reads the actual previous values, writes a private undo file **before changes**, records each completed setting, and verifies the result. The command prints the undo file’s path. Restore it with:

```sh
python3 airplay/manage.py --serial TV_SERIAL restore airplay/.state/SAVED_FILE.json
```

Restore also creates its own undo record. `.state/` and `.cache/` are ignored by Git. Do not publish private device state. A device disconnect can leave a partial change; the saved original values remain available for recovery. Restore cannot roll back unrelated edits made after the snapshot. `--adb PATH` and `--port PORT` select a different ADB executable/server if needed.

## Build the receiver from source

Initialize its source and native dependencies:

```sh
git submodule update --init --recursive airplay/receiver
cd airplay/receiver
./gradlew assembleDebug
```

This is a **separate build**. The pinned upstream project currently declares JDK 21 in its CI, SDK 37 and NDK 27.0.12077973 in its build files, plus native CMake dependencies. HızlıTV’s SDK 35 build does not build the receiver. Follow the pinned [upstream build files](https://github.com/jqssun/android-airplay-server/blob/a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea/app/build.gradle.kts) and [upstream CI](https://github.com/jqssun/android-airplay-server/blob/a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea/.github/workflows/apk.yml); our CI tests the integration/profile tool and the HızlıTV app, not this full native stack. New builds use your own signing identity and cannot necessarily replace the original upstream APK in place.

Upstream documents Android 7+ support, screen/audio mirroring and unprotected media playback. DRM-protected playback is not supported. Features, Apple sender versions, codec support and latency should be checked with your own device. This project is not affiliated with Apple.
