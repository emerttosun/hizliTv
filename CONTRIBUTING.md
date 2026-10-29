# Contributing

[English](CONTRIBUTING.md) · [Türkçe](docs/CONTRIBUTING.tr.md)

Keep HızlıTV small, understandable and comfortable with a remote on modest Android TV hardware. Clear failures, easy installation and preserving user data matter as much as new features.

## Start here

1. Build a debug APK with the [beginner guide](docs/en/GETTING_STARTED.md).
2. Read the [architecture](docs/en/ARCHITECTURE.md) and choose the responsible module.
3. Create a small branch addressing one user problem. Include migration and rollback when persistent data changes.
4. Run relevant tests, then the CI checks. Distinguish real-TV verification from unit tests.

```sh
python3 scripts/check_public.py
python3 scripts/check_translations.py
python3 -m unittest discover -s airplay/tests
./gradlew :media-core:test :player:testDebugUnitTest
./gradlew :launcher:lintDebug :player:lintDebug :tv-ui:lintDebug
./gradlew :launcher:assembleDebug
```

## Code and translations

- Keep UI free of storage and network work. `LibraryScreen` renders, `LibraryRepository` schedules persistence, `Store` stores, and `Resolver` finds media.
- Prefer Android-independent URL, playlist and search rules in `media-core`.
- Never hardcode a device IP, provider account or developer filesystem path. Optional integration packages belong in `AppTargets`.
- Implement new source formats through `SourceAdapter`; use synthetic fixtures and never commit private site responses or access codes.
- Tie work to Activity lifecycle. Avoid persistent services, continuous redraw and remote UI dependencies.
- Preserve predictable DPAD, OK and Back behavior. Never silently change HOME or volume.
- Put English UI text in `tv-ui/src/main/res/values/strings.xml` and Turkish in `values-tr/strings.xml`, with matching placeholders and plurals. The app-only language setting must not change Android's global language.
- The phone page has its own English/Turkish catalog in `PhonePage`; preserve the token protocol and test both pages.
- Update both documentation languages when behavior changes. English is the primary README.
- Resolve compatibility issues narrowly; do not disable lint globally. Test changed behavior rather than repeating implementation details.

## AirPlay and review

The receiver remains a separate, unmodified upstream GPL application pinned by Git submodule. Preserve attribution and native dependency licenses. HızlıTV's Gradle build does not build or bundle it. See [AirPlay](airplay/README.md) before changing its integration.

A PR should explain the user-visible change, verification and remaining limits. Include actual device screenshots without pairing secrets for UI changes. Do not claim untested Android versions, iPhone workflows or sites work.
