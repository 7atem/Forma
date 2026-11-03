# Validation record — alpha02

Date: 3 November 2026. Application `com.formacoach.app`, version `0.2.0-alpha02`, Room schema 2. This is a development alpha, not a clinical or public-release approval.

## Final results

| Check | Result |
|---|---|
| JVM review/history/input/clock/local-model contracts | 19/19 passed |
| JVM portable archive/validation/tamper contracts | 6/6 passed |
| Android 15 / API 35 x86_64 emulator suite | 13/13 passed (8 storage/migration/backup/proposal/history cases + 5 UI cases) |
| Unique automated cases | **38 passed, 0 failed** |
| English/Arabic resources | **227 keys and format placeholders match** |
| Android debug build | Passed |
| Android lint | **0 errors, 1 warning**: pre-existing obsolete empty `mipmap-anydpi-v26` directory |
| Minified release / R8 / resource shrinking | Passed; unsigned production-variant artifact generated |
| Packaged manifest checks | Application INTERNET permission absent from debug and release; emulator package-manager assertion also passed |
| Nemory preservation | Original commit `c4e9f15` on `main` remains clean; independent source/history backup retained |

Final full regression command: `.\gradlew.bat :core:coach:test :core:backup:test :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest :app:assembleRelease --max-workers=1 --no-build-cache`, with `ANDROID_SERIAL=emulator-5582`. After the password fields were marked as password keyboard inputs, debug/lint/minified release packaging was rerun and the actual minified SAF flow was checked as described below.

Coverage includes: missing logs versus zero, window boundaries, drafts/future records, logged-day denominators, source evidence, sufficient weight coverage, Arabic/Persian numeric input, invalid/nonfinite values, immutable nutrition/recipe calculations, sparse history, timer expiry/reboot, live device timezone changes, strict fact-ID verification, unsupported/background model behavior and cancellation.

The Android suite verifies: encrypted persistence/reopen, corrupt or missing key fail-closed behavior, encrypted schema 1→2 migration with retained records/starter order, backup preview without writes, wrong-password rejection, restore without key replacement, rollback after a failing replacement, stale proposal rejection, explicit proposal confirmation, unchanged historical meals/program snapshots, and the daily/program/food UI journeys. The model check on this emulator tested graceful availability/error handling, **not actual Nano inference**.

## Actual minified application / system file-picker QA

The release variant was additionally signed with the ordinary local **development certificate**, matching the debug APK, for emulator-only QA. No production release-signing identity or Nemory release credential was used. The source release build remains unsigned.

Using clearly labelled synthetic records in a disposable read-only emulator:

- Installed and cold-launched the minified APK successfully.
- Selected a synthetic password-encrypted archive through Android DocumentsUI / Downloads.
- Confirmed an incorrect password is rejected without replacing records.
- Unlocked a valid archive, canceled the preview and verified the empty onboarding log remained.
- Confirmed restore and rendered the restored profile, sessions, sourced foods, recipe ingredients, measurements and pending proposal.
- Exported again through ACTION_CREATE_DOCUMENT to Downloads.
- Independently decrypted the result using Python/OpenSSL-backed AES-GCM/PBKDF2 and compared every typed record against the input. All records matched, including immutable recipe sources and proposal state.
- Verified a fractional 20.25 kg recorded load renders accurately and a scheduling change applies only after confirmation.

Results: `artifacts/qa-native/native-roundtrip-result.json`. The scripts and encrypted fixtures in that directory are QA-only and never seed the application automatically. Their records/nutrition values are synthetic, not a food catalog or real personal data. These checks are additional native smoke/round-trip checks, not extra unique JUnit cases added to the 38-case count.

## Visual QA

Reviewed English Today/Coach on the tablet viewport and Arabic RTL Progress on a simulated phone viewport (`1080×2340`, density 420) with 1.8 font scale, including scrolling to the chart. Display overrides were restored. Screenshots are under `artifacts/alpha02-screenshots`. This is limited visual QA; full TalkBack, physical-device, orientation and large-font coverage remains open.

## Artifacts

- `forma-0.2.0-alpha02-debug.apk`: 77,300,627 bytes; SHA-256 `5f80313210d1210f31bab0ceac0a84b71634e418c87062cb2d8fc5d32030b2cd`.
- `forma-0.2.0-alpha02-release-unsigned.apk`: 11,651,570 bytes; SHA-256 `ae1edd8867fd7fef3c171ec4eb843a7c393bc4aa727669cd673f89d5017c3390`.
- `forma-0.2.0-alpha02-minified-qa.apk`: 11,683,445 bytes; SHA-256 `4fdc1628f618dab8855738fbe3b13266f9b734b89cf7605ede9593f3728142be`.

`-minified-qa.apk` is a development-signed installation/testing build, not a Play release. The `-release-unsigned.apk` cannot be installed until signed. The debug APK remains available for debugging. Generated files, logs, encrypted QA fixtures and screenshots are ignored by Git.

## Limits and remaining gates

- Actual local-model download/inference on supported physical hardware, including memory, thermal, quota and foreground-preemption checks. No Qwen CPU vision or model weights are included.
- Physical Android devices, minimum API, 16 KB page-size cases, long histories, reboot and process termination during restore.
- More document-provider failures, canceled/interrupted exports, cross-physical-device restore and forgotten-password usability.
- Qualified review of exercise descriptions/adaptive prescriptions and nutrition sources before automated coaching rules or a curated catalog.
- TalkBack, broader large-text/screen-size testing, the 10–15-person study, beta retention/payment validation and store/privacy/signing review.

AGP legacy-DSL/external-Kotlin deprecation messages remain. The one empty-directory lint warning was already present; automatic review rejected its removal in the earlier alpha01 session, so no new deletion attempt or blanket lint suppression was made. CI is configured but has not run on GitHub. Nothing was published, and no external messages or recruitment were performed.
