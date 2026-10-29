# Publishing, signing and updates

[English](RELEASING.md) · [Türkçe](../YAYINLAMA.md)

Publish the `hizlitv` project root, not the surrounding private workspace. Local SDK/signing properties, keystores, APKs, device logs, real playlists and undo snapshots are excluded from Git. The AirPlay source is a pinned Git submodule with its own license; keep that relationship and attribution intact.

## Source checks and Git

```sh
python3 scripts/check_public.py
python3 scripts/check_translations.py
python3 -m unittest discover -s airplay/tests
git status --short
```

Review staged filenames before publishing. Never add a token or real provider URL just to make a demo easier. For this repository:

```sh
git remote add origin https://github.com/emerttosun/hizliTv.git
git push -u origin main
```

If `origin` already exists, inspect it with `git remote -v` rather than adding a duplicate. The GitHub repository must be accessible to your account. No credential belongs in the remote URL or a committed configuration file.

## CI artifacts versus releases

The Android workflow runs tests, lint, translation/privacy checks and a debug APK build. It also checks the optional migration bridge and AirPlay profile tool. It does not build the full receiver native stack or connect to a physical TV/iPhone.

`hizlitv-debug-apk` contains a development APK; `verification-reports` contains test/lint reports. Debug signing is machine-specific and can change between GitHub runners. Such artifacts are useful for testing, not a stable production update identity.

Public end-user releases should contain a versioned, consistently signed main APK plus its SHA256 and accurate release notes. Keep the key privately backed up; losing it prevents matching-signed updates to existing installations. Do not upload the keystore or its password. This project does not automatically publish a signed release from a push.

Without owner-provided signing details, release builds produce an **unsigned** APK:

```sh
./gradlew :launcher:assembleRelease
```

Owner signing can be supplied through ignored `local-signing.properties`:

```properties
storeFile=PATH_TO_OWNER_KEYSTORE
storePassword=OWNER_STORE_PASSWORD
keyAlias=OWNER_ALIAS
keyPassword=OWNER_KEY_PASSWORD
```

Equivalent private environment variables are `HIZLITV_KEYSTORE`, `HIZLITV_STORE_PASSWORD`, `HIZLITV_KEY_ALIAS`, and `HIZLITV_KEY_PASSWORD`. Existing installations require their original identity; do not generate a new key as an update workaround.

## Versions and rollback

Version name/code live in `launcher/build.gradle`. Player is a library with no separate APK version. Increase `versionCode` for updates and do not replace an already published APK with different bytes under the same version identity.

Downgrading a release APK can be refused by Android; `adb install -d` is not a universal solution. A controlled data-preserving rollback rebuilds older behavior with the original key and a **higher** versionCode. Uninstalling or `pm clear` deletes library/pairing data.

The AirPlay receiver has its own package, version, signature and upstream release stream. Our helper downloads a pinned original and verifies its recorded hash; it does not re-sign or silently upgrade that receiver.

Document actual validation in [testing](TESTING.md). A green CI run is not proof that all codecs, phones or websites work.
