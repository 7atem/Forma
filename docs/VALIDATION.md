# Validation record — alpha04

4 November 2026. Application `com.formacoach.app`, `0.4.0-alpha04`, encrypted Room schema 3. Development/testing build, not public-release or qualified exercise approval.

| Check | Result |
|---|---|
| Existing JVM coach/history/model contracts | 21 passed |
| JVM encrypted portable backup/default compatibility | 7 passed |
| JVM workout-transfer contract | 3 passed |
| Stable API 35 x86_64 emulator | 17/17 passed: 10 storage cases and 7 UI journeys |
| Unique automated cases | **48 passed, 0 failed** |
| Android EN/AR resources | **333 keys/placeholders match** |
| Apple source localization | **49 EN/AR keys match**; native apps unbuilt here |
| Shared exercise/anatomy generation | 8 exercises, 14 muscle groups; no generated-file drift |
| Shared movement guides/art | 8 bilingual guides and 8 original 1536×1024 two-phase PNGs; hashes, sizes, prompt coverage and packaging references match |
| Debug and R8 minified release | Passed |
| Android lint | **0 errors, 1 existing obsolete empty-directory warning** |
| Actual minified app | Startup, tablet rail, phone navigation, visual library, guide and individual-phase viewer reviewed |
| Qualified technique review / Apple native build | **Not performed**; explicit remaining release gates |

JDK 21, sequential Gradle with `--max-workers=1 --no-build-cache`. Tasks: `:core:coach:test :core:backup:test :core:wearables:test`, `:app:assembleDebug`, `:app:connectedDebugAndroidTest`, `:app:lintDebug`, `:app:assembleRelease`. Unchanged JVM inputs reuse their passing results; the final full Android suite passed on fresh disposable Forma data. Native smoke profiles are synthetic and are kept separate from the fresh-data UI-test precondition. Retries/smoke steps are not extra unique cases.

## Added UI and image coverage

The daily journey opens the 3D guide and muscle tab, accepts Arabic numeric warm-up/RPE input, switches away from an active workout and back without losing entered reps/load, saves and completes, then logs food/measurements and verifies review after recreation. All eight exercise cards open their actual image and muscle map. Search finds the Romanian-deadlift variation. The enlarged viewer's selected phase and search state survive Activity recreation. Existing saved-program, sourced-food, explicit scheduling proposal, optional model fallback and encrypted storage/migration/import/rollback coverage remains passing.

Static checks: `check_localization.py`, `generate_exercise_catalog.py --check`, `generate_exercise_guides.py --check`, `check_exercise_art.py`, `check_apple_sources.py`. These establish consistency/integrity, **not biomechanical or clinical validity**. Original generated PNGs were visually sanity-reviewed for distinct poses, full-body framing and basic contacts/equipment plausibility. Each guide names the representative variation and links its editorial reference. No personal form assessment or motion capture is claimed.

## Native visual QA

The release variant was signed separately with the ordinary local **development** certificate, matching debug; the source release APK remains unsigned. No Nemory or production release-signing key was used.

Reviewed the actual minified application with an explicitly synthetic profile:

- English tablet library with persistent navigation rail and toolbar.
- English phone Today hierarchy and visual library at 1080×2340 / density 420.
- Arabic RTL exercise library, movement guide and phase selection.
- Arabic enlarged viewer at font scale 1.5; full body, phase controls, close action and shortened footer remain visible. The footer was adjusted after the first large-text review.
- Thumbnails use sampled IO decoding/cache; full-resolution phase display remains crisp. No network/image SDK was added.

Evidence: ignored `artifacts/alpha04-screenshots/` and `artifacts/alpha04/native-result.json`. Emulator launcher/System UI stalled during one display reconfiguration; rebooting the disposable emulator restored it. No Forma crash was observed in that recovery. Display/font/locale overrides are restored afterward. This remains limited emulator visual QA; full TalkBack, minimum API, physical devices and broader font/orientation coverage are pending.

## Artifacts and checkpoint

- `artifacts/forma-0.4.0-alpha04-minified-qa.apk`: compact, installable **development-signed** test build.
- `artifacts/forma-0.4.0-alpha04-debug.apk`: debugging build.
- `artifacts/forma-0.4.0-alpha04-release-unsigned.apk`: needs owner signing before installation/distribution.

Exact final byte counts, SHA-256, certificate, checkpoint commit and manifest-permission assertions are in ignored `artifacts/build-manifest-alpha04.json`. Final metadata packaging after committing does not change the tested application source.

Branch `codex/3d-exercises-ux`, tag `checkpoint/forma-alpha04`. A verified all-refs source bundle, APK copies, checksums and validation evidence are retained at `D:\Archives\Forma\alpha04-2026-11-04`. Earlier checkpoints are retained. Nemory at `D:\Vault Brain` remains independently preserved and untouched.

## Remaining gates

Qualified review of **every generated pose and translated cue** before public release; actual beginner/form-comprehension usability study; physical Android, minimum API, long histories, accessibility, battery and interruption/provider checks; supported-hardware Nano inference; owner signing/privacy/store review. Apple Swift package tests and iPhone/watchOS compilation/delivery still require Mac/Xcode and paired devices. Four Swift cases exist but are **not run** and are excluded from the 48-case count.

No public release, user recruitment, external messages, remote CI run, cloud logging or adaptive calorie/load prescriptions were performed. See [demonstration/UX boundaries](EXERCISE_DEMOS_AND_UX.md), [Apple status](APPLE_WATCH.md) and [historical alpha03 validation](VALIDATION_ALPHA03.md).
