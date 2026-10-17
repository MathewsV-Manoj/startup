# Focuno

> Focus isn't forced. It's engineered.

Focuno is an AI-powered focus coach for students. It learns your attention
patterns **on-device**, computes a daily **Focus Score**, and gently nudges you
back into deep work — without hard-blocking unless you ask it to.

This repository is a **production-quality foundation**: a multi-module Android
app with clean architecture, a pure-Kotlin scoring/classification engine, unit
tests, and CI. It is intentionally local-first — no accounts, no network, no ads.

## Status

| Area | State |
|---|---|
| Architecture & module graph | ✅ complete |
| Focus Score engine + tests | ✅ complete |
| Heuristic distraction classifier + tests | ✅ complete |
| Room + DataStore persistence | ✅ wired |
| UsageStats capture | ✅ wired (`PACKAGE_USAGE_STATS`) |
| Compose UI (Focus/Analytics/Blocking/Onboarding) | ✅ scaffolded |
| Periodic aggregation worker | ✅ scheduled |
| TFLite model | 🔜 stub ready, model to be trained |
| Cloud sync / backend | ⛔ out of scope for V1 |

See [`docs/architecture.md`](docs/architecture.md) and
[`docs/roadmap.md`](docs/roadmap.md).

## Module layout

```
:app                 – DI wiring, navigation, entry point
:core:model          – pure domain types (no Android)
:core:common         – results, dispatchers, clock
:core:domain         – repository contracts, use cases, score engine, classifier
:core:data           – Room, repositories, aggregation worker
:core:datastore      – preferences (DataStore)
:core:usage          – UsageStatsManager reader
:core:ml             – TFLite classifier + heuristic binding
:core:designsystem   – theme, components
:feature:*           – focus, analytics, blocking, onboarding
```

Dependency rule: features never depend on each other; `:core:domain` never
depends on Android; only `:core:data` touches Room/platform APIs.

## Build

Requires JDK 17 and the Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew test                 # run JVM unit tests
./gradlew installDebug         # install on a connected device
```

Open the project in Android Studio (Ladybug or newer) for the easiest setup.

## How the pieces fit

1. `AggregationWorker` runs every 15 minutes, pulls usage via
   `AndroidUsageStatsReader`, and writes rows to Room.
2. `FocusScoreEngine` (pure Kotlin) turns usage into an explainable 0–100 score.
3. `DistractionClassifier` predicts attention loss. Today it's a transparent
   heuristic; a TFLite model can be dropped in with zero upstream changes.
4. Compose features observe Room through use cases → reactive, offline, instant.

## Extending to the cloud later

`:core:domain` exposes repository interfaces. Adding sync means writing a new
implementation (e.g. `RemoteUsageRepository`) and swapping the binding — no
feature or domain code changes. This is a deliberate design choice, not an
accident.

## Testing

```bash
./gradlew :core:domain:test
```

The domain module is pure Kotlin, so its tests run in milliseconds with no
emulator. That is where the scoring and classification logic lives.

## License

Add a license before publishing. Conversely, add nothing to `:core:model` that
requires Android — it is the one module everyone else trusts.
