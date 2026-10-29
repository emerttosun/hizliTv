# iPhone sharing and Shortcuts

[English](IPHONE.md) · [Türkçe](../IPHONE.md)

This sends a URL/list to the TV's player. It does not mirror the phone screen and does not provide automatic cross-device clipboard synchronization.

## Start with the QR page

1. Connect phone and TV to the same trusted local network.
2. Open Player → Add from phone on the TV.
3. Scan the QR using the iPhone camera. Safari opens a local HTTP page.
4. Paste a supported page, direct video link, M3U text or playlist URL. Press Send to TV.
5. Confirm Play or Add playlist on the TV.

Keep the library screen visible. The listener stops when leaving it or starting playback. The page supports English/Turkish and offers a language selector; its language can differ from the TV UI language.

The full address includes a private pairing token. Keep it private. Matching-signed app updates preserve it. Forget pairing invalidates the existing URL/shortcut. DHCP address changes can require updating the address even when the token remains the same.

## Safari Share Sheet shortcut

Create a shortcut in Apple's Shortcuts app:

1. Name it **Send to HızlıTV** and enable **Show in Share Sheet**. Accept **URLs** as input, not Apps.
2. Add **Get Contents of URL**. Replace its URL value with the **complete HTTP address from the QR page**, including the port and token path. Do not use the film page's URL here.
3. Set method **POST**, request body **JSON**.
4. Add a **Text** field named **`url`**. Its value must be the **Shortcut Input variable**, not the literal text “Shortcut Input”.
5. Add one header: **`X-HizliTV-Token`**, with the token shown on the QR page. Remove any extra blank header rows.
6. Optionally append Quick Look to inspect the server response while troubleshooting.

In Safari, open a video page and choose Share → Send to HızlıTV. The shortcut receives that page's URL, posts it to the TV receiver, and the TV asks what to do. HTTP202 means accepted; source resolution/playback confirmation happen afterward.

For a clipboard shortcut, use **Get Clipboard** as the JSON `url` value and run it explicitly. There is no background clipboard watcher. Running a Share Sheet shortcut from the editor's Play button may have no URL input; test from Safari's Share menu or supply a clipboard input intentionally.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| TLS/secure connection error | Use explicit `http://`, not `https://`; the local receiver is HTTP |
| Network connection lost | Keep Player's library visible; check IP, port and same network; refresh the QR page in Safari |
| Invalid request | POST + JSON + text-valued `url`; no blank header rows |
| Pairing required | Full token path and matching X-HizliTV-Token header |
| Accepted but no film yet | Look at the TV's source/error prompt; 202 is not a playback guarantee |
| Previously worked, now unreachable | TV's IP may have changed; update the saved receiver URL |

No router port forwarding is required. Do not expose the receiver to the internet. The page's token is a credential; remove it from public screenshots/issues.
