# 7Movies Lite — Android starter project

Native Android app built with Kotlin + Jetpack Compose. It provides a minimal search UI and opens matching 7Movies title pages in the browser.

## Important integration limitation
7Movies does not expose a documented public search API. The project therefore includes a best-effort HTML search adapter configured for:

`https://7movies.ws/search?query=...`

That route and the HTML selectors have **not been verified** against the live site's internal search implementation. The app is a starter project, not a verified working release. If the site uses a different route, update `SEARCH_URL` and the Jsoup selectors in `SevenMoviesRepository`.

The app does not extract, proxy, or play video streams. Selecting a result opens its 7Movies page in the browser.

## Build
1. Install Android Studio (current stable) and Android SDK 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync.
4. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Install `app/build/outputs/apk/debug/app-debug.apk` on your Android phone.

## Requirements
- JDK 17
- Android SDK 35
- Internet connection

## Safety
7Movies uses third-party playback providers. Avoid entering personal or payment information into unknown pop-ups or player pages.
