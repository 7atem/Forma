# Validation record — current branch (post-alpha06)

Updated 24 November 2026. Application `com.formacoach.app`, encrypted Room schema 6 (explicit `1→2→3→4→5→6` migrations). Development candidate; public release, physical paired-device Apple testing, visual Apple UI acceptance, and qualified exercise/video approval remain pending.

> **Note on UI preferences:** Display preferences (`kg`/`lb` weight unit toggle, Light/Dark theme, and in-app language `System`/`en`/`ar`) are stored in Android `SharedPreferences` (`forma_ui_prefs`) and iOS `@AppStorage` outside the encrypted Room database and portable backups. Canonical persisted training loads, body weights, backups (`FORMABK1`), and watch transfers remain in `kg`.

## Latest checkpoint (24 November 2026 — Room v6, 12 3D motion clips & iOS parity)

| Check | Result |
|---|---|
| Canonical/generated catalog (`generate_exercise_catalog.py --check`) | **50 stable exercises**, equipment & 14 simplified / 20 detailed muscle regions; no drift |
| Shared 3D motion (`generate_exercise_motion.py --check`) | **12 synchronized clips** (5 original + 7 new); reach, floor, plate clearance & loop continuity verified across Kotlin and Swift |
| Shared movement guides & art (`generate_exercise_guides.py --check`, `check_exercise_art.py`) | **50 bilingual guides** and **50 compressed 960×640 two-phase PNGs**; hashes, XML escaping & packaging match |
| Bundled exercise video (`check_exercise_video.py`) | **31 of 50** bundled; valid files/manifest, **qualified review pending** |
| Android localization (`check_localization.py`) | **829 EN/AR keys** match |
| Apple source checks & localization (`check_apple_sources.py`) | **213 EN/AR keys** match, plus Home/Gym filter, Freestyle workout, expanded tape measurements, read-only HealthKit, discomfort filter & Family-Pot removal assertions. **Swift/Xcode NOT built on Windows; push to trigger `macos-15` CI.** |
| Android JVM tests, debug/release builds, lint (`--max-workers=1`) | Verified on Windows (`:core:coach:test`, `:core:wearables:test`, `:core:backup:test`, `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleRelease`, `:app:compileDebugAndroidTestKotlin`) |
| Room v6 migration (`EncryptedStorageTest` on API 35 `Medium_Tablet`) | **15/15 passed** including `5→6` migration retaining history with hydration & expanded measurements (`b04f131`) |
| Physical iPhone/Watch, visual 3D/video/photo acceptance and qualified pose/cue/video review | **Not performed** |

## Previous committed Apple CI verification (23 November 2026)

| Check | Result |
|---|---|
| Canonical/generated catalog (`generate_exercise_catalog.py --check`) | **50 stable exercises**, equipment & 14 simplified muscle groups; no drift |
| Shared 3D motion (`generate_exercise_motion.py --check`) | **5 synchronized pilot clips**; generated Kotlin/Swift and mathematical constraints match |
| Shared movement guides & art (`generate_exercise_guides.py --check`, `check_exercise_art.py`) | **50 bilingual guides** and **50 compressed 960×640 two-phase PNGs (~18.09 MB)**; hashes, XML escaping & packaging match |
| Bundled exercise video (`check_exercise_video.py`) | **1 of 50** (`SQUAT`); valid file/manifest, **qualified review pending** |
| Apple source checks and localization (`check_apple_sources.py`) | **191 EN/AR keys match**, plus source/fixture/media/discomfort-filter assertions |
| Portable verification chain | All six commands above and `git diff --check` passed on Windows for committed Apple source `ff9cd48` |
| GitHub Actions `macos-15` run [`45191884998`](https://github.com/7atem/Forma/actions/runs/45191884998), job `137752084876` | **Passed** on `ff9cd48`: Swift tests, XcodeGen, iPhone Simulator build, watchOS Simulator build, unsigned ARM64 IPA packaging and artifact upload |
| Physical iPhone/Watch, Apple Health permission, visual 3D/video/photo acceptance and qualified pose/cue/video review | **Not performed** |

The IPA is ad-hoc/unsigned for distribution purposes and requires owner-controlled provisioning. Its embedded HealthKit declaration does not by itself authorize Apple Health on a device. See `APPLE_WATCH.md`.

## Android UX/accessibility pass (23 November 2026, commit 7a6b918 plus follow-up)

Emulator review on API 35 `Medium_Tablet` (Arabic/RTL, light) and a `Pixel_9_Pro` phone image (English, dark). Changes:
quick-log button hidden during an active workout on Train (it covered set steppers and *Discard*); bodyweight-only
exercises save with a blank external load (recorded as 0, shown as bodyweight) and a disabled *Save set* now states
what is missing; save failures show as a snackbar instead of an off-screen banner; one-tap meal repeats confirm with
*Undo*; untouched meal forms no longer show a name error; destructive confirmations name the action and use 48 dp
buttons; exercise-picker rows are 48 dp with selection semantics and a no-results message; Today quick-start cards
show two-line names; `secondaryContainer` is now an on-brand tonal green (tonal buttons, selected chips) with amber
kept as an explicit `accentContainer` for records/warm-ups/protein; remaining hard-coded unit labels (s, kcal, RPE/RIR,
macro shares, set numbers) moved to EN/AR resources.

| Check | Result |
|---|---|
| Static checks (localization, catalog, guides, art, Apple sources) | Passed; **812 EN/AR keys** match |
| JVM unit tests, debug/release builds, lint (`--max-workers=1`) | **60 passed, 0 failed**; builds pass; lint 0 errors |
| Muscle detail upgrade (same working tree) | 20 detailed regions over the unchanged 14 groups, exact roll-up enforced by the generator and by new Kotlin/Swift tests; redrawn original geometry with shaded rendering on Android and iPhone. Android checked visually on the phone emulator (dark and light). **iPhone rendering is source-only until macOS CI/simulator runs.** |
| Full `:app:connectedDebugAndroidTest` on `Medium_Tablet` API 35 | **37 passed, 0 failed** (14 encrypted storage + 23 UI, including 3 new tests for these changes) |
| Follow-up: appearance and Eat toolbar | Appearance offers **System / Light / Dark**; new installs follow the device setting, while an existing saved light/dark choice is kept. *Copy a day of meals* and *Family pot* moved into a 48 dp "More food actions" menu so the Eat toolbar is two rows. Re-run: **834 EN/AR keys**, builds and lint pass (0 errors), **38 passed, 0 failed** on `Medium_Tablet` API 35 (new test covers the menu, theme choice and recreation). Phone emulator checked: legacy Light kept, System follows device dark mode, menu layout, and the 7-day heat map with recorded squat sets. **iPhone still offers only Light/Dark.** |
| `Pixel_9_Pro` image (Android 17 preview) | Instrumented tests cannot run: Espresso's `InputManager.getInstance` reflection is absent on that image; used for manual review only |

## Previous Android verification (22 November 2026)

These results cover the Android checkpoint tested that day; they are not rerun results for every later uncommitted Android edit.

| Check | Result |
|---|---|
| JVM unit tests (`:core:coach:test`, `:core:backup:test`, `:core:wearables:test`) | **54 passed, 0 failed** at that checkpoint |
| Android EN/AR localization (`check_localization.py`) | **765 EN/AR keys & placeholders match** at that checkpoint |
| Android debug, lint & R8 minified release (`--max-workers=1`) | Passed at that checkpoint (0 lint errors; `INTERNET` scoped to free, keyless Open Food Facts lookup) |
| Emulator instrumented tests (`:app:connectedDebugAndroidTest`) | **26 passed, 0 failed** on disposable API 35 `Medium_Tablet` emulator (14 encrypted storage + 12 UI journeys) |

---

## Historical alpha06 baseline (4 November 2026)

4 November 2026. Application `com.formacoach.app`, `0.6.0-alpha06`, encrypted Room
schema 5. Development candidate; public-release and qualified exercise approval remain pending.

| Check | Result |
|---|---|
| JVM coach/history/search/calendar/plan tools | 31 passed |
| JVM authenticated portable backup/default compatibility | 9 passed |
| JVM workout transfer v1/v2 and closed nested fields | 5 passed |
| Stable API 35 x86_64 emulator | 26 passed: 14 encrypted storage cases and 12 UI journeys |
| Unique automated cases | **71 passed, 0 failed** |
| Android EN/AR resources | **575 keys and placeholders match** |
| Apple source localization | **56 EN/AR keys match**; Swift/Xcode not executed |
| Canonical/generated catalog | 40 stable exercises, equipment and 14 simplified muscle groups; no drift |
| Shared movement guides/art | 40 bilingual guides and 40 original 1536×1024 two-phase PNGs; hashes, prompts and packaging references match |
| Debug and R8 minified release | Passed |
| Android lint | **0 errors, 1 existing ObsoleteSdkInt empty-directory warning** |
| Installable minified QA package | Development signature verifies; application INTERNET absent in all three APKs |
| Qualified pose/cue review / Apple native build | **Not performed** |

JDK 21, sequential Gradle `--max-workers=1 --no-build-cache`. Executed tasks:
`:core:coach:test :core:backup:test :core:wearables:test`,
`:app:assembleDebug :app:assembleDebugAndroidTest`,
`:app:connectedDebugAndroidTest`, `:app:lintDebug :app:assembleRelease`.
Final core/debug build, full emulator suite and lint/release build passed. Apple and
release-documentation edits afterwards do not change the Android package. Previous
[alpha05 validation](VALIDATION_ALPHA05.md) and [alpha04 record](VALIDATION_ALPHA04.md)
remain separate, and are not added to this case total.

## Automated coverage

- Optional plan validation, group adjacency/edit normalization, asymmetric targets,
  warm-up exclusion, session isolation and linked-exercise round navigation.
- Actual transaction-returned set counts drive advancement. Exercise change clears
  the load field (checked as empty editable text), never copies another exercise's kg.
- Program targets/notes/grouping survive Activity recreation and are snapshotted into
  sessions. History repeat carries details and no completed sets.
- Encrypted schema 4→5 migration retains older records and uses empty plan details /
  unassigned meals as defaults. Earlier 1→2→3→4 contracts remain covered.
- New plan/category values survive close/reopen, authenticated backup/restore and
  copy-day operations. Legacy portable backups use compatible defaults.
- Breakfast saved-portion category and source/macros survive recreation; exercise
  search in an English interface finds Arabic names with diacritics.
- Locale calendar filters recorded workouts; pure month logic covers leap February.
- v2 fixtures carry plans, v1 still reads, legacy encoding keeps its old shape and
  refuses to downgrade nonempty details; unknown nested fields fail closed.
- Existing 40-art guide coverage, search/filter/favorites, numeric input, plate solver,
  CSV escaping, backup tamper/rollback, local fallback and merge/conflict cases pass.

Static checks: `check_localization.py`, `generate_exercise_catalog.py --check`,
`generate_exercise_guides.py --check`, `check_exercise_art.py`,
`check_apple_sources.py`. Six Swift tests are written but **not run**, excluded from
71. The Apple checker verifies source/fixture consistency, not compilation or delivery.

## Actual minified Android review

The final development-signed R8 candidate was installed on disposable API 35
`emulator-5582`. Synthetic profile and records only, separate from Nemory data.

- Created a saved chest-press/row program with rep range, exercise rest, notes and a
  linked group. Logged 8×20 kg, verified automatic Row selection and empty new
  reps/load fields, then logged 8×15 kg and explicitly finished before all optional
  targets. No false all-targets-complete claim was shown.
- Calendar showed one recorded completed workout; selecting its date opened the
  filtered history containing the two-set session.
- Manual breakfast entry retained 300 kcal, 20 g protein, 30 g carbohydrate and 10 g
  fat, grouped under breakfast after locale/viewport recreation.
- Native SAF program export saved readable JSON. Pulled and parsed that actual
  minified export: version 2 retained the linked group, rep range, rest and notes.
- English tablet 2560×1600/density 320 and Arabic RTL phone viewport
  1080×2340/density 420/font scale 1.5 were inspected. Calendar controls/counts and
  the meal group/card remained readable. These screenshots are not extra test cases.
- Device locale, font and viewport overrides were restored, then emulator shut down.

Screenshots: `artifacts/alpha06/screenshots/tablet-targets-en.png`,
`tablet-linked-next-en.png`, `tablet-calendar-en.png`, `phone-breakfast-ar-large.png`,
`phone-calendar-ar-large.png`. Actual synthetic program export and verification are
in `artifacts/alpha06/native`; automated XML/lint evidence is in `test-evidence`.

## Artifacts and checkpoint

- `artifacts/forma-0.6.0-alpha06-minified-qa.apk`: installable **development-signed**
  test build; 68,964,150 bytes. SHA-256:
  `f70395b6be3e76798c39106453f2f86108ddd2f0f1467d80d315485f56a29ece`.
- `artifacts/forma-0.6.0-alpha06-debug.apk`: debugging build.
- `artifacts/forma-0.6.0-alpha06-release-unsigned.apk`: owner production signing required.

Exact sizes/hashes, certificate, source checkpoint and INTERNET assertions are in
ignored `artifacts/build-manifest-alpha06.json`. Test XML, lint output and build logs
are under `artifacts/alpha06`. Original PNGs remain 69,717,035 bytes with committed
provenance; no new art was generated or sourced from competitors for this checkpoint.

Branch `codex/guided-fitness-workflows`; tag `checkpoint/forma-alpha06`.
Source bundle, APKs, checksums and evidence are retained in
`D:\Archives\Forma\alpha06-2026-11-04`. Independent Nemory and earlier Forma archives
remain preserved. This work does not alter the original Nemory checkout.

## Remaining gates

Qualified review of every generated pose, translated cue and exercise video; actual beginner usability
study; physical Android/minimum API/long-history/accessibility/battery/interruption
acceptance; supported-hardware local-model inference; owner signing/privacy/store
choices. Swift tests and Xcode builds now run in macOS CI. Visual simulator/device acceptance,
HealthKit permission/import with owner-controlled HealthKit-enabled provisioning, and physical paired
WatchConnectivity transfer remain open. No physical-device behavior is established by Android emulator
results, portable static checks, remote compilation or generated-video inspection alone.
