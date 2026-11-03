# Forma / فورمة

Independent Android fitness and nutrition project. Working name; no public brand or store listing has been selected.

Built from the engineering lessons of Nemory, with a new application ID and domain model. Nemory remains at `D:\Vault Brain`; see [the preservation record](docs/NEMORY_PRESERVATION.md).

## First milestone

An offline, bilingual personal log: profile and manually chosen goals, training templates and set logging, manually entered meal totals, body measurements, and a deterministic weekly review. AI explanations, food databases, OCR, reminders, portable backups and subscriptions are subsequent milestones. No network service or model download is required for this first slice.

## Build

Use JDK 21 and an Android SDK with platform 36. Set `sdk.dir` in ignored `local.properties` or configure `ANDROID_HOME`.

```powershell
.\gradlew.bat :core:coach:test :app:assembleDebug :app:lintDebug --max-workers=1
```

Run Gradle sequentially on Windows. See [product scope](docs/PRODUCT_BRIEF.md), [architecture](docs/ARCHITECTURE.md), [reuse audit](docs/NEMORY_REUSE_AUDIT.md), and [delivery plan](docs/ROADMAP.md).

This is an early development build. Status and validation limitations are recorded in `docs/VALIDATION.md`.

The local tested APK is `artifacts/forma-0.1.0-alpha01-debug.apk` (generated, ignored by Git). The foundation passed 8 JVM cases and 4 Android 15 emulator tests; 103 bilingual strings match. Lint reports 0 errors and one obsolete empty-directory warning. AI coaching and portable backups are not implemented yet.
