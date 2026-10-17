# Focuno — Architecture

> AI-powered focus coach for students. Local-first, privacy-preserving, on-device.

## 1. Product summary

Focuno watches *how* you use your phone, learns your distraction patterns, computes a
**Focus Score**, and nudges you back into deep work — without hard-blocking (blocking is
opt-in). Everything runs on-device in V1.

Core loop:

```
UsageStats ──▶ SessionBuilder ──▶ FocusScoreEngine ──▶ Nudge/Insight
   │                                     │
   └──▶ DistractionClassifier (TFLite) ──┘
```

## 2. Module graph

```
:app
 ├── :feature:onboarding
 ├── :feature:focus
 ├── :feature:analytics
 └── :feature:blocking
         │            │
         ▼            ▼
     :core:domain  :core:designsystem
         │
         ├── :core:model
         ├── :core:common
         ▼
     :core:data ──▶ :core:datastore
         │      ──▶ :core:usage
         │      ──▶ :core:ml
         └── (implements :core:domain contracts)
```

Rules:
- **Feature modules never depend on each other.** They communicate through `:app`
  navigation and `:core:domain` use cases.
- **`:core:domain` has no Android dependencies** — pure Kotlin, unit-testable JVM module.
- **`:core:data` is the only module that knows about Room / DataStore / Android APIs.**
- `:core:usage` and `:core:ml` expose narrow interfaces; `:core:data` wires them into
  repositories.

## 3. Layering (Clean Architecture)

| Layer | Module | Responsibility |
|---|---|---|
| Model | `:core:model` | Immutable domain types, no deps |
| Domain | `:core:domain` | Repository interfaces + use cases |
| Data | `:core:data` | Room, DataStore, platform readers; repository impls |
| Platform | `:core:usage`, `:core:ml` | UsageStatsManager, TFLite |
| UI | `:feature:*` | Compose screens + ViewModels |
| Design | `:core:designsystem` | Theme, tokens, shared components |

## 4. Key domain models

- `FocusSession` — a contiguous block of focused time (start, end, interrupted?, dominant app)
- `AppUsage` — package, duration, category (`PRODUCTIVE` / `NEUTRAL` / `DISTRACTING`)
- `DistractionEvent` — timestamp + predicted probability of attention loss
- `FocusScore` — 0–100 for a day, with component breakdown
- `BlockRule` — package, schedule window, enabled flag
- `UserFocusProfile` — learned thresholds/personalization

## 5. Focus Score algorithm (V1)

Weighted, normalized, explainable:

```
score = 100 * (
    0.45 * deepWorkRatio        // focused minutes / total screen-on minutes
  + 0.30 * (1 - switchRate)     // app switches per hour, capped
  + 0.15 * (1 - distractionHitRate)
  + 0.10 * consistency          // vs. 7-day rolling window
)
```

Every sub-metric is surfaced in the analytics UI so the score is never a black box.
The engine (`FocusScoreEngine`) is pure Kotlin and fully unit-tested.

## 6. Distraction detection (V1 → V2)

- **V1 (this scaffold):** heuristic classifier built from switch rate, unlock counts,
  session length, time-of-day. Deterministic + testable. Interface
  `DistractionClassifier` in `:core:domain`.
- **V2:** replace with a quantized TensorFlow Lite model (`:core:ml`) fed the same
  feature vector. Keep the interface identical so nothing upstream changes.
- Feature vector is defined once in `:core:model` (`DistractionFeatures`) so training
  and inference can never drift.

## 7. Data flow & scheduling

- `UsageStatsRepository` reads `UsageStatsManager` events.
- `AggregationWorker` (WorkManager + Hilt) runs every 15 min, materializes sessions,
  recomputes today's `FocusScore`, writes to Room.
- UI observes Room via Flow → always reactive, offline, instant.

## 8. Permissions

| Permission | Why | Fallback |
|---|---|---|
| `PACKAGE_USAGE_STATS` (special) | screen-time + app usage | manual logging |
| `POST_NOTIFICATIONS` | nudges | in-app banner |
| Accessibility/Overlay | app blocking (opt-in) | timer-only blocking |
| `FOREGROUND_SERVICE` | live focus session | — |

No network permission in V1. That is a feature, and it is stated in onboarding.

## 9. Testing strategy

- **Unit (JVM):** domain use cases, `FocusScoreEngine`, `DistractionClassifier`,
  mappers. Run fast, no Android.
- **Data (Robolectric/instrumented):** Room DAOs, DataStore.
- **UI:** Compose tests per feature.
- Coverage gate enforced in CI (Kover, 70% on `:core:domain`).

## 10. Build & CI

- Gradle version catalog (`gradle/libs.versions.toml`), Kotlin DSL, configuration cache.
- GitHub Actions: `assembleDebug`, `testDebugUnitTest`, detekt, ktlint on every PR.

## 11. Deliberate non-goals for V1

- No accounts, no cloud sync, no ads, no network.
- No cross-device analytics.
- No iOS.

These keep the 4-month scope realistic and the privacy story clean. Each has a clear
extension point (repository interface) so V2 can add a backend without a rewrite.
