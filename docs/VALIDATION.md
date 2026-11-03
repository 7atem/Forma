# Validation record

Date: 3 November 2026. This document is updated after the checks finish; unchecked release gates are not completed features.

## Scope

Local debug alpha. Validation includes deterministic JVM cases, English/Arabic resource parity, an Android debug build, lint, isolated encrypted-storage tests and a fresh-data UI journey on a disposable read-only emulator. Generated logs and screenshots remain ignored local artifacts.

## Results

| Check | Result |
|---|---|
| Deterministic JVM cases | 8/8 passed: empty history, seven-day boundaries, drafts/future records, logged-day calorie denominator, evidence, comparison coverage, timezone, Arabic/Persian input, invalid values |
| English/Arabic resources | 103/103 keys and format placeholders match |
| Debug APK | Built successfully, `0.1.0-alpha01`, ID `com.formacoach.app`; 34,551,660 bytes |
| Android lint | 0 errors, 1 warning: obsolete **empty** `mipmap-anydpi-v26` directory left after moving the icon to `mipmap-anydpi` |
| Stable emulator | 4/4 passed on Android 15 / API 35, x86_64, Medium_Tablet, disposable read-only session |
| Encrypted persistence | Save/reopen profile, meal and measurements; non-plaintext DB header; reject empty-session completion, duplicate completion and appending to completed sessions |
| Key failure behavior | Missing key with existing DB and corrupt existing key both fail closed without replacement |
| UI journey | Profile → Arabic-digit set entry → session completion → meal entry → Arabic-digit measurements → Activity recreation → profile and weekly-review UI |
| Visual QA | English and Arabic onboarding inspected on the tablet emulator; RTL text and layout render correctly. Full multi-device/large-font QA remains open |
| Nemory preservation | Original HEAD unchanged and working tree clean; bundle checksum matches manifest; archive verifies from the independent Forma repository |

JVM command: `.\gradlew.bat :core:coach:test --max-workers=1`.

Final Android command used after a Windows resource-cache issue: `.\gradlew.bat :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest --rerun-tasks --no-build-cache --no-daemon --max-workers=1`, with `ANDROID_SERIAL=emulator-5582`.

Earlier attempts exposed AGP/Kotlin DSL compatibility requirements, Windows lint-jar locks and a stale resource-move cache. The final source compiled and all checks above completed successfully after serial regeneration without the build cache. Avoid cleaning a checkout while a Gradle/lint process still owns its files. VFS watching is disabled for this Windows baseline.

An Android 17 / API 37 preview emulator passed the 3 storage tests but could not run the Compose UI test because the selected Espresso version calls a removed InputManager method. That preview is **not** a validated target; the 4/4 UI/storage result is from stable API 35. Emulator-console authentication messages did not prevent the stable test run.

Automatic approval review rejected removal of the empty obsolete resource directory with only `blocked by policy` and no detailed reason. It remains in place, so the one harmless lint warning is reported rather than suppressed. No permission or deletion is needed to build or run the app.

Local deliverables: `artifacts/forma-0.1.0-alpha01-debug.apk`, `artifacts/build-manifest.json`, and `artifacts/screenshots/en.png` / `ar.png`. These generated files are ignored by Git. APK SHA-256: `cacf01133ccc90795d70cf185a4033d3382f283ee1599f80908c18c66aebc46e`.

AGP legacy-DSL / external-Kotlin deprecation messages remain, as documented in architecture. This build is a development alpha without AI chat, automatic coaching prescriptions, a nutrition catalog or portable backups.

## Release gates still required

- Physical Android device testing, including minimum-supported API/device capabilities and 16 KB page-size compatibility where applicable.
- Reviewed exercise content and program/progression rules before prescribing or adapting training.
- Reviewed nutrition sources, quantities, data provenance and proposed coaching behavior before adaptive nutrition.
- Portable export/restore, migrations from v1, data recovery and rollback testing.
- TalkBack, large text, contrast and touch targets; portrait/landscape and phone/tablet visual QA.
- Background/process-death, reboot, midnight/timezone changes and long-history performance.
- Usability/retention study with consenting users and an agreed feedback/privacy approach.
- Release identity, original signing key, dependency/license audit, privacy policy and store disclosures.
- Optional LLM runtime/model licensing, device memory/battery/cancellation and evidence evaluation before enabling AI.

An emulator result is not physical-device or clinical validation. CI configuration is supplied but has not run on GitHub; no repository or app has been published.
