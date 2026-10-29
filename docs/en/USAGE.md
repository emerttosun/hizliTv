# Remote-control usage

[English](USAGE.md) · [Türkçe](../KULLANIM.md)

Open HızlıTV in the stock TV app list. Directions move focus, OK opens a card, and holding OK pins/unpins an app. HOME stays assigned to the original launcher. Menu switches All apps/Favorites; Back returns from All apps to Favorites, then leaves the app.

The seven visible shortcuts open Player, All apps, an installed AirPlay receiver, app-memory diagnostics, explicit memory cleanup, Android Settings, and the stock home screen. The EN/TR button chooses the app language without changing the system language.

## Library

- **Add from phone:** show QR instructions. Existing shortcuts do not need a new QR code unless addressing/pairing changed.
- **Link / playlist:** enter a direct link, supported web page, M3U URL or M3U text.
- **Undo:** revert the most recent library edit; there is only one undo level.
- **All / Favorites / Groups / Search:** filter the library, including English/Turkish text.
- **OK on an entry:** open the source/playback confirmation. Saved web-page entries are resolved again when needed.
- **Hold OK:** favorite or remove the entry.
- **HızlıTV:** return to the dashboard inside the same app.

Incoming links use the themed source prompt. Play starts playback; Add to library saves an entry; Cancel leaves it unchanged. Multiple sources are chosen before confirmation. Playlists have a separate Add playlist confirmation. No phone request automatically starts playback or saves a list.

## Video

OK displays Media3 controls. Menu opens source-provided quality, audio and subtitle options. Unsupported or missing tracks are not invented. Back hides a visible control panel first, then exits playback. Video surroundings are black and aspect ratio is retained.

Volume keys control the device's media stream. The HDMI TV's volume can be a second, independent level. The app does not force volume on every start. A separate AirPlay receiver can affect system volume depending on its configuration; the supplied optional profiles disable sender-driven synchronization.

## Memory tools

RAM occupancy includes Android caching, so a high percentage alone is not a fault. Total/free RAM needs no DUMP permission. Detailed process data depends on optional diagnostics/usage access and Android behavior. A process listed in memory is not necessarily a foreground app.

Free memory requests only Android-allowed cleanup, after the user chooses it. Android can restart apps and restrict cleanup on newer releases. Direct selected-app closure uses a separate explicit local-ADB consent/pairing flow. Do not grant it merely to play media. [Security](../../SECURITY.md) explains scope and undo.

## Phone and AirPlay

Keep the Player library visible when sending; its listener stops outside that screen, including during playback. The phone page uses English/Turkish browser negotiation and a language switcher. [Shortcuts](IPHONE.md) sends a URL rather than mirroring the entire phone screen.

AirPlay is independent: open the receiver from its shortcut, then use the Apple device's Screen Mirroring menu. See [receiver setup](../../airplay/README.md). Its own UI and permissions belong to the upstream app.
