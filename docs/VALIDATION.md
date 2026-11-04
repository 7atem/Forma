# Validation record — alpha06

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

Qualified review of every generated pose and translated cue; actual beginner usability
study; physical Android/minimum API/long-history/accessibility/battery/interruption
acceptance; supported-hardware local-model inference; owner signing/privacy/store
choices. Swift tests, Xcode builds, HealthKit lifecycle and physical paired
WatchConnectivity transfer require macOS and Apple devices. No physical device or
native Apple success is claimed by the Android emulator or static checks.
