# Running HızlıTV on another Android TV

[English](PORTING.md) · [Türkçe](../YENI_TV.md)

HızlıTV is one APK written in plain Java with no native libraries, so the same build runs on ARM and x86 Android TV / Google TV devices from Android 6 (API 23) up. Model-specific values are read from the device at runtime instead of being written into the code. This page lists what adapts by itself, what to check on a new TV, and the few places to edit if a model needs something different.

Only the Mi TV Stick (Android 11, 1080p) has been tested on real hardware. Everything below about other models is a checklist, not a compatibility claim.

## What adapts automatically

| Area | How it adapts | Code |
| --- | --- | --- |
| Video resolution | 2160p on a 4K panel with at least about 1.8 GB RAM, otherwise 1080p. The panel size comes from the display's supported modes, so a 4K TV that runs its menus at 1080p still gets 4K video | `DeviceProfile.maxVideoSize` |
| Playback buffer | 24 MB (the tested value) whenever video is capped at 1080p; with 4K playback 48 MB, or 96 MB from 3 GB RAM | `DeviceProfile.bufferBytes` |
| Layout | Sizes are in dp and the TV reports about 960×540 dp for both 1080p and 4K, so cards, panels and text keep their proportions | `HomeScreen`, `LibraryScreen` |
| Remote without a Menu key | Google TV remotes often have no Menu key. In the player, **hold OK** (or press Menu/Info if present) for options. On the dashboard, scroll down to reach all apps | `PlayerActivity.dispatchKeyEvent` |
| Stock launcher | HOME and the Stock home icon resolve the device's own launcher, Android TV or Google TV | `AppTargets.stockHome` |
| AirPlay icon | Hidden unless a supported receiver is installed | `AppTargets.AIRPLAY` |
| Phone receiver port | Keeps the saved port (default 8198). If another app on the TV already uses it, a free port is chosen and the QR code shows it | `PhoneCompanion.start` |
| Free memory | Disabled with an explanation on Android 14 and later, where Android no longer allows it | `MemoryController.relax` |
| Language | Follows the device language for English/Turkish, English otherwise | `tv-ui` resources |

Every start writes one line describing the device, which helps when comparing TVs:

```sh
adb logcat -s HizliTV.Device
```

It prints the model, Android version, panel size, RAM and the chosen video cap and buffer. It contains no user data.

## Installing on a new TV

1. Open **Settings → System → About** (on some TVs **Device preferences → About**) and press **Android TV OS build** seven times to enable developer options.
2. In **Developer options**, enable **USB debugging**. Note the TV's IP address from its network settings.
3. From the computer, on the same network:

   ```sh
   adb connect TV_IP:5555
   ```

   Approve the prompt on the TV. If the connection is refused, the TV may use **Wireless debugging** instead: open it in Developer options, choose pairing with a code, then run `adb pair TV_IP:PAIRING_PORT` and `adb connect TV_IP:PORT` with the values the TV shows. Menu names vary by manufacturer.
4. Install and open:

   ```sh
   adb -s TV_IP:5555 install -r launcher/build/outputs/apk/release/launcher-release.apk
   adb -s TV_IP:5555 shell am start -n tr.hizlitv.home/.MainActivity
   ```

A new TV has no earlier HızlıTV, so any signature installs. To update later without losing the library, keep using the same signing key. Build releases with your own `local-signing.properties` (see [releasing](RELEASING.md)); debug builds from different machines or CI runs may not update each other.

## Checklist for a new model

| Check | How | If it fails |
| --- | --- | --- |
| Device summary | `adb logcat -s HizliTV.Device` | The panel or RAM is misread: adjust the rules in `DeviceProfile` |
| Dashboard fits and focus is visible | Move across icons, favorites and all apps | Sizes live in `HomeScreen` |
| Remote keys | OK, hold OK, ◀ ▶, ▲ ▼, CH±, Back, Play/Pause in the player | Add the remote's key code to `PlayerActivity.dispatchKeyEvent` |
| 4K / HDR playback | Play a 4K sample, then open hold OK → Quality | If 4K stutters, lower the cap in `DeviceProfile.maxVideoSize` |
| Surround audio | Play AC-3/E-AC-3 through the TV speakers and through HDMI ARC/eARC | Media3 picks passthrough or decoding from what the TV reports; check the audio output setting on the TV |
| Phone sharing | Scan the library QR code from a phone on the same Wi-Fi | Some routers isolate clients ("AP isolation"); allow local traffic |
| Memory screen | Open Memory | Process lists need optional permissions; Free memory is unavailable on Android 14+; closing selected apps needs network ADB on port 5555, which many Google TVs do not offer |
| AirPlay receiver | Install the companion, then check the icon | Install the build that matches `adb shell getprop ro.product.cpu.abilist` (some TVs run 32-bit Android on 64-bit chips) |

## Where to change things for a model

Most models need no code change. When one does, the changes stay in a few places:

- **Video cap and buffer:** `tv-ui/src/main/java/tr/hizlitv/ui/DeviceProfile.java`, with unit tests in `tv-ui/src/test`.
- **Remote key mappings:** `player/src/main/java/tr/hizlitv/player/PlayerActivity.java` (`dispatchKeyEvent`).
- **Preinstalled favorites, AirPlay packages, stock launcher:** `launcher/src/main/java/tr/hizlitv/home/AppTargets.java`.
- **Apps the Memory screen must never close** (launchers, settings): `LocalAdb.PROTECTED` and `AppManagerActivity.protectedPackage`. Add the TV maker's launcher or system apps there.
- **Package visibility:** add new launcher or receiver package names to `<queries>` in `launcher/src/main/AndroidManifest.xml`.

Keep the English and Turkish strings in step (`python3 scripts/check_translations.py`) and run the checks in [testing](TESTING.md) before installing.
