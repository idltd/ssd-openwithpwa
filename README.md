# ssd-openwithpwa

Android app that routes file opens to a PWA.

Configure one or more extension → URL mappings. When you open a matching file, the app encodes it as Base64 and opens the configured URL with the file data as a query parameter, in the browser of your choice.

## Setup

1. Install the APK on your Android device.
2. Open the app to reach the route configuration screen.
3. Tap **+ Add route**, enter the file extension and your PWA's base URL.
4. Tap **Find apps for this URL** and pick the browser where your PWA is installed.
5. Save. The app is now the handler for that extension.

## Requirements

- Android 8.0+
- The target PWA must be reachable at the configured URL and able to accept a query parameter containing Base64-encoded file data.
