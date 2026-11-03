# Forma / فورمة

Independent Android fitness and nutrition project. Working name; no public brand or store listing has been selected. Nemory remains unchanged at `D:\Vault Brain`; see [its preservation record](docs/NEMORY_PRESERVATION.md).

## Current version: 0.2.0-alpha02

Offline bilingual logging with encrypted Room storage, editable saved training programs, set corrections and date backfill, a rest timer, session search/load history, sourced food/recipe portions, meal corrections, measurements and charts, evidence-based weekly reviews, explicit check-ins/schedule proposals, and portable password-encrypted backup/restore.

Optional Gemini Nano on compatible AICore devices can select existing report highlights. No raw model claims, automatic diet/exercise prescriptions, cloud LLM or application INTERNET permission. Logging and reviews work without a model. Actual inference still needs supported physical-device verification.

The current library contains user-entered foods and sources; no nutrition catalog is preloaded. Public release still requires the reviews, device tests and user study in [the roadmap](docs/ROADMAP.md).

## Build

JDK 21 and Android SDK platform 36. Configure ignored `local.properties` or `ANDROID_HOME`.

```powershell
.\gradlew.bat :core:coach:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease --max-workers=1
```

Run Gradle sequentially on Windows. Use `:app:connectedDebugAndroidTest` on a disposable stable emulator. Do not clean while a Gradle/lint process still owns files. The release APK is unsigned; no Nemory signing material is used.

See [implementation and backup format](docs/DAILY_WORKFLOW_IMPLEMENTATION.md), [architecture](docs/ARCHITECTURE.md), [reuse provenance](docs/NEMORY_REUSE_AUDIT.md), [validation](docs/VALIDATION.md), [study protocol](docs/USABILITY_STUDY.md) and [release gates](docs/RELEASE_CHECKLIST.md).

Generated APKs, manifests and screenshots are under ignored `artifacts/`. This is a development alpha, not a complete adaptive-coaching release.

Validated locally: 38 automated cases pass, 227 bilingual strings match, debug and minified release builds pass. Lint has 0 errors and one pre-existing empty-directory warning. The system-picker backup round trip also passed on the minified QA app with synthetic data. Install `artifacts/forma-0.2.0-alpha02-minified-qa.apk` for the compact development-signed test build, or use the debug APK.
