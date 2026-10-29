# Permissions, local data and security boundaries

[English](SECURITY.md) · [Türkçe](docs/SECURITY.tr.md)

## Basic use

The single APK declares normal network permissions and optional device-tool permissions. The launcher and Player work without diagnostic or ADB access. No camera, microphone, contacts or shared-storage permission is requested: the TV generates a QR code; the phone reads it.

The library, playback positions, pairing identity and operation journal remain in Android's private application storage. URLs can contain provider credentials. Full URLs, request bodies and pairing tokens are not written to general Logcat. Uninstalling or clearing application data removes these records. Real playlists, accounts, device backups and signing keys are excluded from the source repository.

The phone receiver is intended for a trusted local network. Its pairing URL contains a secret; POST also requires `X-HizliTV-Token`. The server checks the path, token, origin, content type and size. It provides no permissive CORS, shell execution, file server or open proxy. It uses **HTTP**, so LAN traffic is not encrypted. Do not forward its port to the internet. **Forget pairing** invalidates the current URL. The receiver stops when the library screen is not visible.

Source resolution does not bypass login, DRM or CAPTCHA and does not execute arbitrary website JavaScript. User confirmation is required before playback or playlist import.

## Optional device tools

| Permission / feature | Purpose | Limit |
| --- | --- | --- |
| `KILL_BACKGROUND_PROCESSES` | User-requested background-memory cleanup | Android controls the result and may restart apps. Other-app cleanup is not requested on Android 14+. |
| `DUMP` | Read process diagnostic reports | Explicit owner grant; exposes broader system-service diagnostics. |
| `PACKAGE_USAGE_STATS` | Additional read access required by some firmware | Explicit owner grant; availability varies. |
| Local ADB | Stop selected unprotected apps using a user-approved debugging connection | Broader device control than diagnostic reads; optional, separately explained and paired. |

System RAM totals use `ActivityManager.getMemoryInfo`; no broad diagnostic permission is needed. If detailed process data is unavailable, the UI says so. On a device you own, optional grants are:

```sh
adb shell pm grant tr.hizlitv.home android.permission.DUMP
adb shell pm grant tr.hizlitv.home android.permission.PACKAGE_USAGE_STATS
```

Replace `grant` with `revoke` to undo. Firmware may reject a command; access is never assumed. Android's Usage Access settings may also be available.

The local ADB client connects only to loopback. The user starts pairing and approves Android's debugging prompt. Its key remains in private app storage. Root is not required. Some firmware requests approval on every connection; persistent authorization is not guaranteed. Forgetting the app's connection and revoking Android debugging authorizations are separate actions.

Force-stop applies only to explicitly selected packages outside the protection list. It does not clear data or cache, but ongoing work stops. Open the app again to resume use. HOME assignment and system volume are never changed automatically.

## Legacy migration

The optional same-signature `legacy-bridge` checks the caller UID's signing certificate before reading data; other signatures and ADB shell are rejected. It exposes no network, launcher or command interface. Only bounded, fixed-name backup data is streamed. The new Player validates the archive and imports it privately without logging its pairing token. See [migration and rollback](docs/en/MIGRATION.md).

## Optional AirPlay receiver

AirPlay Server is a separate GPL-3.0 application with its own network services and settings. It is not required for HızlıTV. The checked-in integration pins its source/version and validates the original APK checksum. Profiles require pairing PINs and leave receiver-volume synchronization disabled; applying a profile is an explicit command with a private undo record. See [AirPlay setup](airplay/README.md). Do not publish `.state` files or private receiver backups.

## Reporting and publishing

`python3 scripts/check_public.py` checks the distributable file allowlist for obvious private-data patterns; it is not a complete security audit. Review staged changes too. Local SDK/signing properties, keys, APKs and build output are ignored.

Before reporting an issue, remove pairing URLs, provider credentials, private keys and identifying logs. Include the affected version and redacted reproduction steps. If no private reporting channel is configured, request one from the repository owner before disclosing sensitive details publicly.
