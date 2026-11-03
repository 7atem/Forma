# Forma / فورمة

Independent fitness and nutrition project with an Android app and native iPhone/Apple Watch companion source. Working name; no public brand or store listing has been selected. Nemory remains unchanged at `D:\Vault Brain`; see [its preservation record](docs/NEMORY_PRESERVATION.md).

## Current version: 0.3.0-alpha03

Offline bilingual logging with front/back muscle highlighting, weekly muscle counts, warm-up/RPE sets, previous-value copy, best recorded working loads, and encrypted Room storage, editable saved training programs, set corrections and date backfill, a rest timer, session search/load history, sourced food/recipe portions, meal corrections, measurements and charts, evidence-based weekly reviews, explicit check-ins/schedule proposals, and portable password-encrypted backup/restore.

Optional Gemini Nano on compatible AICore devices can select existing report highlights. No raw model claims, automatic diet/exercise prescriptions, cloud LLM or application INTERNET permission. Logging and reviews work without a model. Actual inference still needs supported physical-device verification.

The current library contains user-entered foods and sources; no nutrition catalog is preloaded. Public release still requires the reviews, device tests and user study in [the roadmap](docs/ROADMAP.md).

## Build

JDK 21 and Android SDK platform 36. Configure ignored `local.properties` or `ANDROID_HOME`.

```powershell
.\gradlew.bat :core:coach:test :core:wearables:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease --max-workers=1
```

Run Gradle sequentially on Windows. Use `:app:connectedDebugAndroidTest` on a disposable stable emulator. Do not clean while a Gradle/lint process still owns files. The release APK is unsigned; no Nemory signing material is used.

See [implementation and backup format](docs/DAILY_WORKFLOW_IMPLEMENTATION.md), [architecture](docs/ARCHITECTURE.md), [reuse provenance](docs/NEMORY_REUSE_AUDIT.md), [validation](docs/VALIDATION.md), [study protocol](docs/USABILITY_STUDY.md) and [release gates](docs/RELEASE_CHECKLIST.md).

Generated APKs, manifests and screenshots are under ignored `artifacts/`. This is a development alpha, not a complete adaptive-coaching release.

The Android watch icon exports selected programs and previews/imports completed workout files. Native companion source supports offline watch logging, muscle diagrams and queued WatchConnectivity results. **Apple source has not been built here: Xcode/macOS and paired-device tests are required.** Apple Watch needs a paired iPhone; Android↔iPhone uses an explicit file bridge. No HealthKit recording or live Android sync is claimed. See [Apple workflow](docs/APPLE_WATCH.md), [feature research](docs/COMPETITOR_FEATURE_RESEARCH.md) and [muscle catalog](docs/EXERCISE_MUSCLES.md).

Current checks and installable APK paths are in [validation](docs/VALIDATION.md). Public adaptive coaching and Apple distribution remain pending their documented gates.
