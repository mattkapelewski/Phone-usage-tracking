# Phone Usage Tracker

An Android app with a 1x1 home-screen widget that shows your total phone
usage for the day. You choose which apps count toward that total.

## Features

- **Widget**: a 1x1 home-screen widget showing today's total usage (e.g.
  `2h15m`). Tap it to open the app.
- **App selection**: a screen listing every launchable app on your device
  with today's usage time and a checkbox — uncheck any app to exclude it
  from the total. Everything is included by default.
- **Live-ish updates**: the widget refreshes whenever you open the app or
  change the selection, and in the background every 15 minutes (the
  shortest interval Android's WorkManager allows for periodic work).

## How it works

- `UsageStatsRepository` reads foreground-time events from Android's
  `UsageStatsManager` for the current day (local midnight to now) and pairs
  `MOVE_TO_FOREGROUND` / `MOVE_TO_BACKGROUND` events per package to compute
  per-app durations.
- `AppPrefsRepository` persists the set of *excluded* package names in
  Jetpack DataStore. Only apps the user has explicitly unchecked are
  excluded — new apps count by default.
- `UsageWidgetProvider` (an `AppWidgetProvider`) renders the total via
  `RemoteViews`, and `WidgetUpdateWorker` (WorkManager) keeps it fresh in
  the background.
- App selection only lists apps with a launcher entry (`ACTION_MAIN` /
  `CATEGORY_LAUNCHER`), so you won't see background services or system
  components cluttering the list.

## Setup / permissions

This app relies on the special **Usage access** permission
(`PACKAGE_USAGE_STATS`), which can't be requested via a normal runtime
permission dialog. On first launch, tap **Grant usage access** on the home
screen — this opens system settings where you enable access for this app.

To add the widget: long-press your home screen → Widgets → **Phone Usage
Tracker** → drag the 1x1 widget onto your home screen.

## Project structure

```
app/src/main/java/com/phoneusage/tracker/
  UsageTrackerApp.kt        Application class; schedules the background worker
  data/                     UsageStatsRepository, AppPrefsRepository, models
  ui/                       MainActivity, AppSelectionActivity, adapter
  widget/                   UsageWidgetProvider, WidgetUpdateWorker
  util/                     Time formatting helpers
```

## Building

Standard Gradle/Android Studio project — open the root folder in Android
Studio (Giraffe or newer) and let it sync, or run:

```
./gradlew assembleDebug
```

Requires network access to Google's Maven repository (`dl.google.com` /
`maven.google.com`) and Maven Central to resolve the Android Gradle Plugin
and dependencies, plus an installed Android SDK (`compileSdk 34`,
`minSdk 26`). This particular build environment didn't have outbound access
to those hosts, so the project wasn't compiled in-session — it was written
and reviewed by hand against the standard Android/Gradle project layout.
