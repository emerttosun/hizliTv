# Migrate the former separate Player

[English](MIGRATION.md) · [Türkçe](../GECIS.md)

Fresh installations need only the HızlıTV APK. `player` is now an embedded Android library. The optional migration bridge is for existing installations that previously used both `tr.hizlitv.home` and `tr.hizlitv.player`.

Android isolates private app data by package. The new app cannot simply read the old package's files, even if both apps have similar names. Uninstalling the old Player removes its private data.

## Controlled migration

1. Preserve the old APK/source and signing identity. Do not clear either app's data.
2. Build the bridge and new app using the **same owner key** as the existing installations:

   ```sh
   ./gradlew -PincludeLegacyBridge=true :legacy-bridge:assembleRelease :launcher:assembleRelease
   ```

3. Update the old Player package with the bridge APK first, then update HızlıTV, using `adb install -r`. The bridge has the old package ID, no launcher UI and no network service.
4. Open Player inside HızlıTV. Before starting the phone listener, a repository worker requests the bounded snapshot from the bridge. The provider verifies the caller's signing certificate before reading data.
5. Verify library/favorites, undo, playback positions and the existing QR/shortcut address before removing anything.

If the old package exists without a valid bridge, the new app does not silently create an empty replacement library. If both private areas already contain separate libraries, it refuses to overwrite them automatically. A differently signed APK is not a valid migration path.

## Backups and rollback

The untouched snapshot is retained as private `files/legacy-player-backup.zip`. Fixed entry names, sizes, JSON contents and a final completion marker are validated. The import preserves pairing token/port and hashed playback positions. `player-migration-result.json` stores a private result summary; diagnostics expose counts/status, not credentials.

Keep the bridge installed but stopped until the new app is verified. This preserves the old private data for rollback without a visible old Player icon or active network service. Fresh users do not install it.

While its data remains, rebuilding the old Player source with the same key and **versionCode above100** can restore the old UI in place. Changes made in the new app are not automatically reverse-migrated. After uninstalling the bridge, reinstalling its old APK will not automatically restore the snapshot; a controlled same-owner maintenance build is needed to recover from the new app's private archive. Do not uninstall/clear HızlıTV because that also deletes its private backup.

The migration tool, private snapshots and receiver are not part of the main end-user APK distribution.
