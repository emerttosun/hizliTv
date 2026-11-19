# Changes

[English](CHANGELOG.md) · [Türkçe](../DEGISIKLIKLER.md)

## 0.10.0 — 19 November 2026

Install over 0.9.0 with the same signing key; the library, playback positions and phone pairing are kept.

**Phone sharing**
- The pairing QR code stays beside the library; pairing is created on the first visit, so there is no dialog to open first. OK on the panel enlarges it and offers a new QR code.
- The phone page puts the input first, shows a coloured status and adds Copy address / Copy token buttons for the iPhone shortcut. The request format is unchanged; existing shortcuts keep working.
- If the receiver's port is taken on a TV, a free port is chosen instead of failing.

**Dashboard**
- Four round tool icons (AirPlay, Memory, Settings, Stock home) replace seven labelled buttons; the focused icon's name appears in the greeting panel, which also shows RAM.
- Favorites and all apps are on one screen. Menu jumps between them where the remote has a Menu key; Back returns to favorites.
- Free memory moved into the Memory screen. AirPlay is hidden when no receiver is installed.

**Library**
- One Show button (All, Favorites or a group) replaces three; Undo appears only when there is something to undo.
- OK on an entry plays it directly. Links sent from a phone still ask on the TV.
- All library dialogs use the TV style; the list highlights only when it has focus.

**Player**
- New remote-first overlay: title, source, list position, LIVE badge, clock, progress and remaining time.
- OK play/pause, ◀ ▶ seek (faster while held), ▲ ▼ / CH± previous/next entry, hold OK (or Menu/Info) for options.
- Options: quality, audio, subtitles, subtitle size, picture (fit/zoom/stretch), speed and sleep timer. Each is a small `PlayerOption` module.
- Resume or start over, quiet reconnect after a dropped stream, readable error messages with Retry / Next / Back.
- Readable titles from file names, up to 4K on 4K panels with enough memory.

**Other Android TVs**
- `DeviceProfile` reads the panel and RAM at runtime; see [other Android TVs](PORTING.md) for the checklist.

**Project**
- Screenshots refreshed; CI actions updated and failures reported as annotations.
