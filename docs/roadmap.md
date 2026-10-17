# Focuno — 4-Month Solo Roadmap

Goal: ship a production-quality **local-first** Android app to Play Store internal
testing by Month 4. ~10–12 focused hours/week.

## Milestone 0 — Foundation (Week 1–2) ✅ scaffolded
- [x] Multi-module Gradle project + version catalog
- [x] Clean architecture skeleton (model/domain/data/platform/ui)
- [x] Focus Score engine + heuristic distraction classifier
- [ ] Build green on your machine (`./gradlew assembleDebug`)
- [ ] Add your signing config + Firebase Crashlytics (optional, later)

## Milestone 1 — Usage capture (Week 3–5)
- [ ] Request `PACKAGE_USAGE_STATS` with a clear rationale screen
- [ ] `UsageStatsReader`: real queries (events + totals)
- [ ] `SessionBuilder`: merge events into `FocusSession`s
- [ ] Room persistence + `AggregationWorker`
- **Exit:** app shows yesterday's and today's real screen time per app.

## Milestone 2 — Focus Score (Week 6–8)
- [ ] Wire `FocusScoreEngine` to real aggregates
- [ ] Analytics screen: score, breakdown, 7/30-day trend charts
- [ ] App categorization (let users tag apps productive/distracting)
- **Exit:** daily score updates and is explainable.

## Milestone 3 — Nudges (Week 9–11)
- [ ] `DistractionClassifier` heuristics tuned on your own data
- [ ] Foreground service + notification nudges during focus sessions
- [ ] Focus sessions ("start deep work") with live timer
- **Exit:** app interrupts you 2–3× and it feels *helpful*, not annoying.

## Milestone 4 — Blocking + polish (Week 12–14)
- [ ] Opt-in scheduled app blocking (Accessibility/overlay)
- [ ] Onboarding flow + privacy explainer
- [ ] Compose UI polish, dark mode, empty states, accessibility
- [ ] Play Store listing, screenshots, internal testing track

## Milestone 5 — ML (Week 15–16, stretch)
- [ ] Export labeled data from your own usage
- [ ] Train a small TFLite model on the `DistractionFeatures` vector
- [ ] Swap `HeuristicDistractionClassifier` → `TfliteDistractionClassifier`
- [ ] A/B evaluate offline; ship only if better than heuristics

## Sequencing advice
1. **Real data before ML.** Do not train a model until Milestone 1 works. You will
   otherwise optimize against fake data.
2. **Ship to internal testing at the end of Month 3.** Real testers expose more than any
   roadmap.
3. **Keep the practice of `#buildinpublic`** — it doubles as your user research and your
   marketing for the eventual launch.
4. **Cut scope before quality.** A polished 3-screen app beats a broken 8-screen one.

## Guardrails (the solo-founder failure modes)
- Pick **one** lane by Month 4: this app or RF/defense research. You can revisit after.
- Timebox: assign coding to fixed blocks; protect coursework and placement prep.
- Track a single north-star: *daily active focused minutes created for users.*
