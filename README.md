# ssd-openwithpwa

Android dispatcher app that intercepts "Open with" intents for configured file extensions and forwards the file to a target PWA (or any browser) via a URL parameter.

## What it does

When you open a file (e.g. `.ssd`) from a file manager or email client, this app catches the intent, encodes the file as Base64, and opens your configured PWA URL with the file data appended as a query parameter — e.g. `https://yourapp.example.com/?ssd=<base64>`.

The target browser/app is configurable per route, so you can send files directly to the browser where your PWA is installed, including Chrome WebAPKs (installed PWAs that appear as standalone Android apps).

## Setup

1. Install the APK on your Android device.
2. Open the app to reach the route configuration screen.
3. Tap **+ Add route**, enter the file extension and your PWA's URL.
4. Tap **Find apps for this URL** — installed browsers and any WebAPK for that URL will be listed. Pick the one where your PWA lives. `App:` prefixed entries are WebAPKs (the PWA installed as a standalone app); unprefixed entries are browsers.
5. Save. The app is now the handler for that extension.

## Requirements

- Android 8.0+
- The PWA must be reachable at the configured URL and able to accept a `ssd` query parameter containing Base64-encoded file data.
