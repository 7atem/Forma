# Forma / فورمة

Independent fitness and nutrition project with an Android app and native iPhone/Apple Watch companion source. Working name; no public brand or store listing has been selected. Nemory remains unchanged at `D:\Vault Brain`; see [its preservation record](docs/NEMORY_PRESERVATION.md).

## Current version: 0.7.0-alpha07

The procedural **3D motion library** (12 exercises; [details](docs/EXERCISE_MOTION.md)) is currently hidden behind a feature flag (`ENABLE_PROCEDURAL_3D_MOTION = false` on Android, `ExerciseGuideFlags.enableProcedural3DMotion = false` on Apple) so both apps present **Videos**, **Photos**, and **2D muscle highlights** only.

Offline-first bilingual logging with **74** original two-phase 3D exercise illustrations (`960×640` WebP), **21** detailed muscle regions (including Forearms & grip), **37** optional bundled AI demonstration videos (none reviewed yet; release blocked until qualified review), optional iPhone-only `.usdz` rigged-model loader with no models bundled yet ([pipeline](docs/RIGGED_3D_MODELS.md)), named variation guides, an enlarged phase viewer, main-muscle & Home/Gym filtered exercise library, user-defined **Custom Exercises** (with primary muscle, equipment, set logging & personal bests), 7-day body muscle heat map, 1-tap Freestyle workouts, cardio and timed-activity logging (time, optional distance, pace for walks and runs; no calorie estimates), one-handed set nudges & dumbbell per-hand clarity, warm-up/RPE/RIR sets, live PR & estimated 1RM analytics, Olympic barbell plate visualizer + warm-up ramp, 12-week recorded-workout grid, encrypted Room v10 storage (SQLCipher), 7/30-day nutrition trends against your own targets, 12-week training trends, opt-in reminders (chosen days and times, rotating encouraging messages, skip already-logged days, +250 ml water button), read-only Android Health Connect import (weight, exercise sessions, and sleep), editable saved training programs, set corrections and date backfill, a sticky rest timer with lock-screen chronometer, session search/load history, sourced food/recipe portions with **42 curated offline starter whole foods & Arabic/Middle Eastern staples** (USDA FDC CC0 & FAO Near East references), optional free/keyless **Open Food Facts (ODbL)** name & barcode lookup (with native camera scanner on Android & iOS), offline Food-Swap calculator, saved meal combos, Ramadan Suhoor/Iftar slots, meal corrections, macro donut split, daily hydration & personal Supplements/Vitamins checklist, weight + expanded tape measurements (waist, neck, chest, arms, hips, thighs) + BMI (number only, from your height and logged weight), read-only sleep duration/stages from Health Connect or Apple Health & 7-day rolling trend charts, Android home-screen widget, full CSV export (Training, Nutrition, Measurements/Hydration), evidence-based weekly reviews, explicit check-ins/schedule proposals, and portable password-encrypted backup/restore (`FORMABK1`).

Optional Gemini Nano on compatible AICore devices can select existing report highlights. No raw model claims, automatic diet/exercise prescriptions, paid cloud APIs, or user-data telemetry. Application `INTERNET` permission is scoped strictly to free, public, keyless food lookup (Open Food Facts, ODbL); core logging and reviews work offline without a model or network. Actual Gemini Nano inference still needs supported physical-device verification.

The optional starter whole-food catalog uses public-domain USDA FoodData Central (CC0) reference values and requires explicit user import into the editable local library; logged portions always retain immutable snapshots. Public release still requires the reviews, device tests and user study in [the roadmap](docs/ROADMAP.md).

## Build

JDK 21 and Android SDK platform 36. Configure ignored `local.properties` or `ANDROID_HOME`.

```powershell
.\gradlew.bat :core:coach:test :core:wearables:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease --max-workers=1
```

Run Gradle sequentially on Windows. Use `:app:connectedDebugAndroidTest` on a disposable stable emulator. Do not clean while a Gradle/lint process still owns files. The release APK is unsigned; no Nemory signing material is used.

See [implementation and backup format](docs/DAILY_WORKFLOW_IMPLEMENTATION.md), [architecture](docs/ARCHITECTURE.md), [reuse provenance](docs/NEMORY_REUSE_AUDIT.md), [validation](docs/VALIDATION.md), [study protocol](docs/USABILITY_STUDY.md) and [release gates](docs/RELEASE_CHECKLIST.md).

Generated APKs, manifests and screenshots are under ignored `artifacts/`. This is a development alpha, not a complete adaptive-coaching release.

The Android watch icon exports selected programs and previews/imports completed workout files. The native iOS/watchOS source under `apple/` includes a standalone 4-tab iPhone app (Train, Eat, Progress, Exercises + Watch/Android Bridge) and offline Apple Watch logger. The iPhone app can optionally **read** (never write) workouts and body weight from Apple Health after explicit permission; imported items are labelled and kept separate from Forma logs. Apple stores local state (programs, workouts, phone meals and weights) in OS-protected `forma.json` rather than Android SQLCipher. **GitHub Actions `macos-15` CI verifies `swift test`, `xcodegen`, iOS/watchOS Simulator builds, and an unsigned ARM64 iOS IPA; physical paired iPhone + Apple Watch device tests remain required.** Apple Watch needs a paired iPhone; Android↔iPhone uses an explicit file bridge. No HealthKit recording or live Android sync is claimed. See [Apple workflow](docs/APPLE_WATCH.md), [feature research](docs/COMPETITOR_FEATURE_RESEARCH.md) and [muscle catalog](docs/EXERCISE_MUSCLES.md).

Current checks and installable APK paths are in [validation](docs/VALIDATION.md). Public adaptive coaching and Apple distribution remain pending their documented gates.

Alpha04 improves the sticky header, adaptive phone/tablet navigation, Today progress, compact session logging, draft-preserving training sections and responsive food/measurement forms. [Illustrations, prompt provenance and UX details](docs/EXERCISE_DEMOS_AND_UX.md). Images are generated examples; qualified exercise review remains pending before public release.

Alpha05 adds equipment/primary-muscle filters, favorites, personal notes, similar-muscle exploration, searchable workout/program exercise selection, program duplication, repeat-as-new workouts, completed-set CSV, an exact plate calculator and confirmed meal-day copy. [Implementation/compatibility](docs/ALPHA05_EXPANSION.md) and [official-source product review](docs/COMPETITOR_REVIEW_2026_11_04.md).

Alpha06 adds user-entered sets/rep ranges/per-exercise rest and program notes, linked supersets/circuits, persisted-count workout navigation, a locale-aware history calendar, bilingual/diacritic-tolerant exercise search and a grouped meal diary with user-target progress. Detailed plans survive session snapshots, repeat, encrypted backup and v2 Watch transfer; readers keep legacy v1 support. [18-product workflow review and implementation choices](docs/PRODUCT_WORKFLOW_REVIEW_ALPHA06.md). Apple native builds and qualified exercise review remain pending.

Alpha06 implementation and compatibility: [guided workflows](docs/ALPHA06_WORKFLOWS.md); executed checks: [validation](docs/VALIDATION.md).
