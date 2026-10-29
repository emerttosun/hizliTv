# From source code to your first TV app

[English](GETTING_STARTED.md) · [Türkçe](../BASLANGIC.md)

HızlıTV is a native Java Android TV app. You can change it and build your own APK without rooting the TV or replacing its firmware.

## Vocabulary

| Term | Meaning here |
| --- | --- |
| Source code | Human-readable Java files describing behavior and screens |
| APK | The package installed on Android; downloading source does not install it |
| JDK 17 | Java compiler and tools used for this project |
| Android SDK | Android APIs, build tools and ADB |
| Gradle | Builds modules, dependencies and resources into an APK |
| Gradle Wrapper | Checked-in scripts that select and download the required Gradle version |
| Activity | An Android component that owns a screen and its lifecycle |
| View | A native button, label, list or other visual component |
| DPAD / focus | Remote-control navigation and the currently selected element |
| Manifest | Declares components, permissions and TV requirements |
| Signing identity | Identifies an app's owner; matching signatures are needed for in-place updates |

`minSdk 23` means Android 6 or later. `compileSdk 35` selects the API definitions used to compile. `targetSdk 35` selects the Android behavior contract; it is not the minimum Android version.

## Prepare the tools

1. Install [Android Studio](https://developer.android.com/studio).
2. In SDK Manager install Android SDK Platform 35, Build Tools 35.0.0 and Platform-Tools.
3. Select JDK 17 for Gradle. Check `java -version` if building in a terminal; Android Studio and the terminal can use different JDKs.
4. Clone the repository and open the folder containing `settings.gradle` in Android Studio:

   ```sh
   git clone https://github.com/emerttosun/hizliTv.git hizlitv
   cd hizlitv
   ```

The first Gradle sync downloads dependencies. Supply the SDK location through Android Studio, `ANDROID_HOME`, or your own `local.properties`:

```properties
sdk.dir=ABSOLUTE_PATH_TO_YOUR_ANDROID_SDK
```

This is a local file and is ignored by Git. Do not copy another developer's SDK path or signing key.

## Build and test

```sh
./gradlew :media-core:test :player:testDebugUnitTest
./gradlew :launcher:assembleDebug
```

On Windows run `.\gradlew.bat`. On macOS/Linux, use `chmod +x gradlew` once if the executable flag was lost. The output is `launcher/build/outputs/apk/debug/launcher-debug.apk`. `player` is a library and does not produce a second app APK.

Debug signing is automatic. A debug build from another machine or CI run may not match an existing app's signature. Do not uninstall an app just to silence an update error: uninstalling removes private library/pairing data. Read [signing and updates](RELEASING.md).

Follow [installation](INSTALLATION.md) to connect your TV. Main-app builds do not require initializing the optional AirPlay submodule or installing a native compiler toolchain.

## Make a small change

1. Change the `TEXT` color in `tv-ui/.../TvUi.java`; it affects common headings and button text.
2. Rebuild with `./gradlew :launcher:assembleDebug`.
3. Update with the same signing identity and `adb install -r`.
4. Verify readability and focus on the TV.
5. Restore the source change and rebuild. Editing a Java file alone does not change the app installed on the TV.

For language changes, edit matching English `values/strings.xml` and Turkish `values-tr/strings.xml` entries in `tv-ui`, then run `python3 scripts/check_translations.py`. Do not translate protocol keys, package IDs or shell command names.

Continue with [architecture](ARCHITECTURE.md), [Android TV navigation](https://developer.android.com/training/tv/get-started/navigation), and [Media3](https://developer.android.com/media/media3/exoplayer).
