# Material Play

A controller-first game launcher for Android, built with Kotlin, Jetpack Compose and Material 3.

Material Play finds the games already installed on a device, keeps their metadata and artwork in
sync, launches them, and turns the phone into a console when a gamepad is connected. Everything on
screen is real: package scans come from `PackageManager`, artwork comes from the network or the
installed app icon, and play time is measured from actual launches.

## Highlights

- **Two interfaces**
  - **Normal Mode** – console-style launcher with a compact top navigation bar, a cinematic
    game backdrop driven by the selected cover, and searchable library views (grid, list, carousel).
  - **Gaming Mode** – full-screen, landscape, controller-navigable console UI with the same data.
- **UI scale** – Compact, Default and Large; applied through Compose density without changing the
  device DPI, so Compact suits phones.
- **Real game detection** – launcher activities plus `CATEGORY_GAME` and application-category
  metadata. Manual add, hide/exclude, favourites and manual metadata correction are supported.
- **Metadata enrichment** – RAWG and IGDB for text, SteamGridDB for portrait covers/hero/logo.
  Fuzzy title matching, manual candidate selection, local image cache and app-icon fallback.
- **Controller aware** – USB-C, Bluetooth and built-in pads via Android's input stack. Detects the
  pad, remembers it, and shows drawn vector glyphs that match the controller family.
- **Sessions and stats** – per-game play time, recent sessions and totals, measured from launch until
  the launcher returns to the foreground.
- **Customisation** – light/dark/system theme, six accents, dynamic colour (Android 12+),
  artwork/gradient/solid backgrounds, cover scale, blur strength, animations and button hints.

## Requirements

- Android 8.0 (API 26) or newer; Android 12+ for dynamic colour.
- JDK 17 and Android SDK Platform 35.
- A gamepad is optional, but Gaming Mode is designed for one.

## Build

The project uses the Gradle wrapper. From the repository root:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew :app:assembleDebug
```

The wrapper downloads Gradle 8.10.2 on first use. If a local Gradle 8.10.2 is already
installed, `/opt/gradle-8.10.2/bin/gradle :app:assembleDebug` produces the same APK without the
download.

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must contain `sdk.dir=/path/to/android-sdk` (or provide `ANDROID_SDK_ROOT`).
On a normal desktop build no extra configuration is needed.

> **ARM64 build hosts:** the Android Gradle Plugin downloads an x86_64 `aapt2`. This checkout is
> built on an ARM64 phone using a translated binary, configured machine-locally in
> `~/.gradle/gradle.properties` as `android.aapt2FromMavenOverride=/opt/android-tools/aapt2`.
> That override is intentionally not committed. Remove or adjust it on other machines.

## Metadata keys

Keys are entered in **Settings → Metadata providers** and stored only in this app's private
DataStore. Each provider is optional and can be used alone:

| Provider | What it adds | Where to get a key |
| --- | --- | --- |
| RAWG | Description, genres, screenshots, landscape art | <https://rawg.io/apidocs> |
| IGDB | Cover, release date, genres, rating | <https://api-docs.igdb.com> (Twitch client ID + secret) |
| SteamGridDB | Portrait cover, hero image, transparent logo | <https://www.steamgriddb.com/profile/preferences/api> |

Without any key the launcher still works offline: covers fall back to the installed app icon and
all library, launch, controller and session features remain available. **Enrich new games
automatically** can be disabled to keep scanning completely local.

## Normal Mode

- **Home** – continue playing, recent games, favourites and quick library shortcuts.
- **Library** – searchable grid/list of all tracked apps with sort, filter and favourites. Hidden
  games are kept with their history and can be restored from the **Hidden** chip.
- **Controllers** – connected devices, connection type, input capabilities, per-controller glyph
  profile override and a live input test.
- **Stats** – totals and per-game play time, recent sessions and history clearing.
- **Settings** – appearance, controller behaviour, metadata keys and library maintenance.

Adding a non-game app manually makes it a first-class library entry. Hiding a game only excludes it
from the launcher; the app itself is untouched.

## Gaming Mode

Gaming Mode is a separate activity, forced to landscape and drawn edge-to-edge. Navigation is a
compact top tab bar (Home, Library, Controllers, Stats, Settings) above a content area. Focus is
explicit and wraps predictably, so every control is reachable with the D-pad or left stick; touch
still works.

### Controls

| Action | Behaviour |
| --- | --- |
| A / Confirm | Launch a game, open details, activate a setting |
| B / Back | Return to the rail, close an overlay, or pause |
| Y / Tertiary | Open game details (Home) / toggle favourite (Library) |
| X / Quaternary | Context action where shown |
| Start | Pause menu |
| Select | Quick menu (battery, clock, shortcuts) |
| D-pad / left stick | Move focus |
| L1/R1, Start+Select, L3+R3 or Back+Start | Quick overlay chord (configurable) |

Keyboard fallbacks are available for testing: Enter/Space = Confirm, Escape = Back, Tab = Select.
Physical mapping is fixed per pad family; the glyph profile only changes the drawn symbols.

### Connection behaviour

- **Open Gaming Mode on connect** (default on) switches to Gaming Mode when a pad connects while the
  launcher is in the foreground.
- **Leave Gaming Mode on disconnect** returns to Normal Mode when the last pad disconnects.
- **Ask before leaving Gaming Mode** shows the pause/confirm dialog when the pad disconnects.
- **Background controller monitor** runs a small foreground service, allowing detection while
  another app is open. It requires the notification permission and is started automatically when
  either background option is enabled.
- **Open Gaming Mode from other apps** is best effort and requires **Open Gaming Mode on connect**
  to stay enabled. With **draw-over-apps** granted (Settings → Controller behaviour → Allow
  draw-over) the monitor can start the activity directly. Otherwise Android blocks background
  activity starts, so the app posts a high-priority notification and, on Android 14+, can use a
  full-screen intent only if that permission is granted. This is an Android platform restriction,
  not a missing feature.
- A Quick Settings tile and a launcher shortcut also open Gaming Mode.

### Session tracking

A session starts when a game is launched and ends when the launcher is foregrounded again. Because
Android gives no reliable "app closed" callback, play time is approximate: time spent in the game's
own menus is included, and a session that is never closed by returning to the launcher is capped by
the next launch. Values are labelled as approximate in Stats.

## Architecture

The project intentionally ships as **one Gradle `app` module** with strict package layering. Building
several Gradle modules on a phone-sized machine is slow and memory-hungry; a single module keeps
incremental builds practical here. The packages are kept acyclic:

```
core        small shared utilities (text normalisation, date formatting)
data        Room database, DAOs, DataStore settings, session/controller repositories
games       package scanning, library repository, launching, package-change receiver
metadata    provider clients, title matching, image store, refresh orchestration
gamepad     input devices, semantic router, monitor service, QS tile
controllerui vector glyph rendering, glyph profiles, appearance selection
gaming      Gaming Mode activity, screen, overlays
ui          Normal Mode screens, shared components, theme, view models
di          AppContainer – manual dependency wiring
```

- **UI:** Jetpack Compose + Material 3. Normal Mode uses a tab state plus a details overlay; Gaming
  Mode owns its focus state machine.
- **Data:** Room for games, sessions and controllers; Preferences DataStore for settings. Manual DI
  through `AppContainer`, with one shared `LauncherViewModel` base for both modes.
- **Input:** `GamepadManager` observes `InputManager` devices; `GamepadRouter` converts key and axis
  events into semantic `PadAction`s and handles stick repeat, so UI code never depends on raw key
  codes. A shared router instance lets both modes consume input without conflicting listeners.
- **Metadata:** provider-agnostic repository; title scoring ignores punctuation/case, penalises
  unmatched sequels, and requires a confidence threshold before applying a match. Images are cached
  locally and can be replaced from the device gallery.

## Permissions

| Permission | Why |
| --- | --- |
| `QUERY_ALL_PACKAGES` | Find installed games. Required for a launcher. |
| `POST_NOTIFICATIONS` | Controller notifications and the monitor service. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CONNECTED_DEVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Background controller monitoring. |
| `SYSTEM_ALERT_WINDOW` | Optional draw-over exemption to open Gaming Mode from other apps. |
| `USE_FULL_SCREEN_INTENT` | Notification fallback on Android 14+. |
| `VIBRATE`, `WAKE_LOCK` | Optional feedback and keeping the screen awake in Gaming Mode. |

No analytics, ads or telemetry. Network access is used only for configured metadata providers and
image downloads.

## Known limitations

- Play time is approximate (see above); games launched outside the launcher are not tracked.
- Background activity starts remain subject to Android/OEM restrictions even with draw-over granted.
- Metadata matching is heuristic. Use **Fix match** in game details to search and pick the right
  entry, or replace the artwork locally.
- Without API keys there is no scraped description/art; app icons are used as covers.
- Localization ships with English and Spanish. Spanish is used when the system language is Spanish, or
  when selected manually in Settings. Most system-facing strings are translated; a few internal
  diagnostic messages remain English.
- Gaming Mode is landscape-only by design.
- The debug build uses the `.debug` application ID, so it can be installed alongside a release build.

## Project layout

```
app/src/main/java/com/materialplay/launcher/
  MainActivity.kt            Normal Mode entry point
  MaterialPlayApp.kt         application, notification channels, service wiring
  di/AppContainer.kt         dependency graph
  core/ data/ games/ metadata/ gamepad/ controllerui/ gaming/ ui/
app/src/main/res/            themes, icons, shortcuts, backup rules
```

## Status

Version 1.0.0 (versionCode 1), debug build verified with `:app:assembleDebug`.
