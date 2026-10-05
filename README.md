# Focuno

Focuno is an Android app that helps students preparing for GATE, JEE, NEET or UPSC stop doomscrolling. It counts screen time honestly, pauses the apps you choose, and makes focus time feel like progress. Everything stays on the phone: there is no account, no ads, no analytics and no internet permission.

## Install

1. On your Android phone, download the latest test build:
   **https://github.com/MathewsV-Manoj/startup/releases/download/focuno-debug/focuno-debug.apk**
2. Open the file and allow your browser or file manager to install apps when Android asks.
3. Open Focuno and follow the setup. It needs only two permissions to block apps: **Usage access** and **Display over other apps**.

Every push to this branch rebuilds the APK at the same link. A new build installs over the old one and keeps your data and permissions.

## What it does

**Focus**
- A timer (15 to 90 min), Pomodoro (25 min focus, 5 min break, 2 to 4 rounds) or a stopwatch.
- While it runs, time-eater apps are paused. Lock mode pauses every app except the ones you allow; phone, Settings and the keyboard always work.
- Strict mode: no ending early, no unlock. While Strict is off, the app warns that you can still end early.
- Tag a timer with a subject (Signals, Networks, Maths). Stats shows study time per subject.
- Background sounds generated on the phone: white noise, soft rain, deep waterfall, ocean waves.
- Notifications for "Break time", "Back to focus" and "Focus done", also with the screen off.
- A daily study goal ("1h 20m / 4h") and an exam countdown ("GATE 2027 · 123 days left").

**Block**
- Time slots: block an app at night, during study hours, or at your own times. You can block only Instagram Reels or YouTube Shorts and keep the rest of the app.
- Daily limits per app, by minutes, number of opens, or both.
- A time-eater budget: one daily limit for all time-eater apps together.
- Pause before opening: chosen apps show a 5-second breathing pause each time they open, then ask "Open it, or go back?".
- The pause screen offers "Go back" or "Focus 25 min instead". Unlocking a non-strict block takes a 15-second breathing pause and a written reason, gives 5 minutes, and then has a 30-minute cooldown.

**Stats**
- Today's screen time, split into helpful, okay and time-eater apps, plus every app with its time.
- Trends over 7 or 30 days: study time per subject, focus score, time-eater minutes, your worst hour, and urges resisted.
- A weekly report every Monday morning.
- Export everything as a CSV file.

**Me**
- Levels and points that never go down, three daily missions, badges, and a 12-week focus calendar.

**Home-screen widget**
- Today's screen time, your streak, the exam countdown, and a one-tap "Start 25 min focus" button with a live countdown.

## Permissions

| Permission | Why |
|---|---|
| Usage access | Count screen time and see which app is in front. Required. |
| Display over other apps | Show the pause screen over a blocked app. Required for blocking. |
| Accessibility (optional) | Faster blocking, and blocking only Reels or Shorts. Android 13+ restricts it for apps installed from a file, so Focuno works without it. |
| Notifications | Warn if blocking stops, focus timer alerts, weekly report. |
| Battery optimisation exemption | Stop the phone from closing Focuno in the background. |

Focuno never reads messages, text on screen, passwords, photos, contacts or location. For Reels or Shorts blocking it checks only a few technical screen-part names inside Instagram and YouTube.

## Build from source

Requirements: JDK 17 or newer and the Android SDK (compile SDK 37).

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions workflow in `.github/workflows/test-apk.yml` runs both on every push and publishes the APK to the link above.

## Project layout

```
app/src/main/java/com/startup/focuno/
  domain/      Pure rules with unit tests: usage counting, focus score, schedules, limits,
               Pomodoro phases, streaks, game rules, weekly report, exam countdown
  data/        Room database, settings (DataStore), repositories, CSV export
  service/     Blocking engine, accessibility and basic-mode hosts, monitor service,
               focus alarms, focus sounds, widget, workers
  ui/          Jetpack Compose screens: Focus, Block, Stats, Me, Settings, onboarding, pause screen
```

Kotlin, Jetpack Compose with Material 3, Hilt, Room, DataStore and WorkManager. Minimum Android 8.0 (API 26), target API 36.

## Known limits

- Blocking adds friction; it is not a lock. Focuno can always be turned off in Android Settings.
- The Instagram Reels and YouTube Shorts screen names may change when those apps update.
- Pomodoro alerts use an alarm that needs no special permission, so Android may deliver them a little late when the phone has been idle for a long time.
- Phone makers' battery savers (Xiaomi, Oppo, Vivo, Samsung and others) can stop background apps. The setup and Health check show the settings to change.
