# Validation record — alpha03

3 November 2026. App `com.formacoach.app`, version `0.3.0-alpha03`, encrypted Room schema 3. Development alpha; not a clinical or public-release approval.

| Check | Result |
|---|---|
| JVM review/history/muscle/input/model contracts | 21/21 passed |
| JVM portable backup and alpha02 default compatibility | 7/7 passed |
| JVM watch wire fixture/bounds/invalid-value contracts | 3/3 passed |
| Android 15 / API 35 x86_64 emulator | 16/16 passed: 10 encrypted storage/migration/merge cases, 6 UI cases |
| Unique executed automated cases | **47 passed, 0 failed** |
| Android localization | **274 EN/AR keys and placeholders match** |
| Shared exercise generation | 8 exercises / 14 muscle groups; Kotlin, Swift and resources synchronized; path subset/coordinate checks pass |
| Apple portable source consistency | Identical cross-platform fixture, companion IDs and **44 EN/AR keys** match |
| Apple Swift tests | 4 cases written; **NOT RUN** on this Windows host; excluded from the 47-case count |
| iPhone/watchOS compilation and paired delivery | **NOT VERIFIED**; Swift/Xcode/macOS unavailable here |
| Android debug and minified release/R8/resource shrinking | Passed |
| Android lint | **0 errors, 1 existing empty-directory warning** (`mipmap-anydpi-v26`) |
| Actual minified APK | Cold startup and native SAF workout import/export checks passed, details below |
| Nemory preservation | Original `main` at `c4e9f15` remains clean; independent preservation archive unchanged |

Commands were run sequentially with JDK 21 and `--max-workers=1 --no-build-cache`: JVM suites for `core:coach`, `core:backup`, `core:wearables`; `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:connectedDebugAndroidTest`, `:app:lintDebug` and `:app:assembleRelease`. Emulator target: `emulator-5582`, stable Android 15. No preview-API Espresso assumptions. Final packaging after the checkpoint commit only refreshes build metadata; it does not change the tested application source.

The first alpha03 UI run caught mojibake in the test's Arabic numeric literals and an ambiguous map selector; both test issues were corrected and the full 16-case suite passed. The 47-case total counts the passing final cases once, not retries or native smoke steps.

## Coverage added

Encrypted migration chains 1→2→3 and 2→3 preserve original profile/session/set records, program order/rest preferences and working/local defaults. Watch merge is idempotent; a later conflict rolls back an earlier insert in the same transaction. Sensor metadata, warm-up and RPE survive encrypted backup/restore. Alpha02 serialized records lacking the new fields retain their defaults.

Muscle tests require every exercise's mapping, visible regions and disjoint roles. Seven-day counts exclude warm-ups, drafts, future dates and out-of-window sessions. Load history and highest recorded working loads exclude warm-ups. Wire tests cover the shared fixture, round trip, unsupported exercise/version/unit/field, duplicates, nonfinite values, RPE bounds, maximum bytes and nesting.

UI coverage includes all eight muscle maps, Arabic numeric warm-up/RPE entry, the watch entry point and disabled empty export selection, plus the existing daily logging, saved program, sourced food, check-in confirmation and unsupported-model fallback journeys. The emulator does **not** verify actual Gemini Nano inference.

## Native minified / system picker QA

The production build variant was signed separately with the ordinary local Android **development** certificate for testing. Source release output remains unsigned; no Nemory or production signing credential was used.

Using only `contracts/apple-watch/transfer-v1.json` and a deliberately conflicting synthetic copy:

- Cold-launched the minified app, created an explicitly labelled synthetic profile and opened the watch dialog.
- Opened a completed-workout file through Android DocumentsUI/Downloads, reviewed the preview and explicitly imported it.
- Reopened the identical file and confirmed successful deduplicated handling; conflicting set content was explicitly rejected.
- Exported a selected starter program through ACTION_CREATE_DOCUMENT and independently parsed the output: v1/KG, expected exercise order, one selected program, no workouts/profile/food/key fields.
- This provider appended `.json` to the proposed `.formaworkouts` filename. Both iPhone and Android importers support JSON/content parsing; the extension does not determine trust or validation.
- Reviewed the real minified muscle diagram and labels in English and Arabic RTL at a simulated 1080×2340 phone viewport, density 420, default font scale. Row primary/supporting colors and text, front/back order, recorded load and Arabic numbers rendered correctly. All eight map changes are also covered by the UI suite.

Evidence: ignored `artifacts/qa-native/alpha03-result.json`, screenshots in `artifacts/alpha03-screenshots`. These are smoke/visual checks, not additional JUnit cases. No actual watch data, physical pairing or sensor reading was used. Display and locale overrides are restored after QA.

## Artifacts and recovery

Install `artifacts/forma-0.3.0-alpha03-minified-qa.apk` for the compact development-signed test app, or `forma-0.3.0-alpha03-debug.apk` for debugging. `forma-0.3.0-alpha03-release-unsigned.apk` needs owner signing before installation. Final byte counts, SHA-256, certificate and checkpoint commit are in ignored `artifacts/build-manifest-alpha03.json`.

The final source is committed on `codex/muscles-apple-watch`, tagged `checkpoint/forma-alpha03`. A verified all-refs source bundle, APK copies and checksums are retained separately under `D:\Archives\Forma\alpha03-2026-11-03`. The alpha02 checkpoint/archive remains available. Historical baseline evidence: [alpha02 validation](VALIDATION_ALPHA02.md).

## Remaining gates

Apple build/paired-device acceptance is explicitly pending: [workflow and limitations](APPLE_WATCH.md). Current Apple source is an offline manual workout logger; no HealthKit sensors, Apple Health writes, background timer guarantee or automatic Android live sync. Source consistency is not native runtime validation.

Physical Android/minimum-API/long-history/16 KB-page and interruption/provider-failure checks, actual Nano hardware inference, TalkBack/VoiceOver, broader font/orientation coverage, qualified exercise/nutrition review, the user study, beta outcomes and owner signing/store review remain open. There is no public catalog or automated calorie/load prescription. CI is configured but has not run remotely; nothing was published or sent to external users.
