# Validation record — alpha05

4 November 2026. Application `com.formacoach.app`, `0.5.0-alpha05`, encrypted Room
schema 4. Development candidate; public-release and qualified exercise approval are pending.

| Check | Result |
|---|---|
| JVM coach/history/model/tools contracts | 25 passed |
| JVM authenticated portable backup/default compatibility | 8 passed |
| JVM workout transfer, including new exercise names and legacy packets | 4 passed |
| Stable API 35 x86_64 emulator | 22 passed: 13 encrypted storage cases and 9 UI journeys |
| Unique automated cases | **59 passed, 0 failed** |
| Android EN/AR resources | **539 keys and placeholders match** |
| Apple source localization | **53 EN/AR keys match**; Swift/Xcode not executed |
| Canonical/generated catalog | 40 stable exercises, equipment and 14 simplified muscle groups; no drift |
| Shared movement guides/art | 40 bilingual guides, 40 original 1536×1024 two-phase PNGs; hashes, dimensions, prompts and native resource references match |
| Debug and R8 minified release | Passed |
| Android lint | **0 errors, 1 existing obsolete empty-directory warning** |
| Installable minified QA package | Development signature verifies; application INTERNET absent |
| Qualified pose/cue review / Apple native build | **Not performed** |

JDK 21, sequential Gradle `--max-workers=1 --no-build-cache`. Tasks:
`:core:coach:test :core:backup:test :core:wearables:test`,
`:app:assembleDebug :app:assembleDebugAndroidTest`,
`:app:connectedDebugAndroidTest`, `:app:lintDebug :app:assembleRelease`.
The full 22-case Android functional suite passed on the disposable emulator.
Subsequent presentation adjustments (compact headings, three-column tablet cards,
resetting guide scroll when browsing another exercise) were rebuilt, linted and
reviewed on the actual minified package below. The default source date for meal-day
copy is also clamped to the supported 1970 lower bound. These are not extra test cases.
Unused duplicate legacy exercise-label resources were removed; both locales remain
in parity. Existing alpha04 evidence is retained in [its record](VALIDATION_ALPHA04.md).

## Automated coverage

- Every one of the 40 cards opens its corresponding actual illustration and muscle
  map. Enlarged phase/search state survives Activity recreation.
- Combined equipment/muscle/favorite filtering and clear; encrypted favorite/notes
  retention and display after recreation; Arabic-digit plate input.
- Exact balanced plate solving, inventory limits, empty bar, fractional plates and
  cases where a greedy strategy fails; no pinned choice between equally optimal loads.
- Canonical CSV values, working/warm-up inclusion, draft exclusion, multiline Arabic
  and spreadsheet-formula-sensitive names.
- Encrypted 1→2→3→4 upgrades retain old records; new preferences survive close/reopen
  and authenticated export/preview/restore. Favorite edits preserve notes.
- Repeat-history creates one fresh draft with preserved order/rest and zero copied
  completed sets; an existing draft rejects another start.
- Copy-day retains meal portions/source snapshots with fresh IDs and atomically
  rejects a source edited after preview.
- Existing backup tamper/size/rollback, stored program/food snapshots, explicit
  check-in proposals, local-model fallback and Watch merge/conflict contracts remain.

Static commands: `check_localization.py`, `generate_exercise_catalog.py --check`,
`generate_exercise_guides.py --check`, `check_exercise_art.py`,
`check_apple_sources.py`. Four Swift tests exist but were **not run** and are
excluded from the 59-case total. The Apple checker is source consistency only.

## Actual minified Android review

Final QA package installed on disposable API 35 `emulator-5582`. Synthetic profile
and records only; no physical device or Nemory data was used.

- Startup and retained encrypted data across package updates; native tablet rail.
- One completed DEAD_BUG set: 8 repetitions, 0 kg added external load.
- Native SAF CSV create/save, pulled and parsed as CSV with the expected ID/reps/kg.
- English tablet 2560×1600 at density 320: compact heading, three columns, pictures,
  names, muscle/equipment labels and favorite actions visible.
- Arabic phone viewport 1080×2340 at density 420, font scale 1.5: RTL library,
  previously logged filter, new dead-bug guide, movement-only full-screen image,
  readable controls and the core map with no invented supporting-muscle legend.
- Viewport, locale and font overrides restored after inspection.

Evidence: `artifacts/alpha05/screenshots/tablet-library-en.png`,
`phone-library-ar-large.png`, `phone-dead-bug-phase-ar-large.png`,
`phone-core-map-ar-large.png`; XML/log evidence is under
`artifacts/alpha05/test-evidence`. Development/intermediate screenshots in that
folder are not additional passing test cases.

## Artifacts and checkpoint

- `artifacts/forma-0.5.0-alpha05-minified-qa.apk`: installable **development-signed**
  test build; 68,894,518 bytes.
- `artifacts/forma-0.5.0-alpha05-debug.apk`: debugging build.
- `artifacts/forma-0.5.0-alpha05-release-unsigned.apk`: owner signing required.

Exact hashes, sizes, certificate, final source checkpoint and INTERNET assertions:
ignored `artifacts/build-manifest-alpha05.json`. Original 40 PNGs total 69,717,035
bytes; their source/prompt provenance is committed. Alpha04's original eight PNGs
remain unchanged.

Branch `codex/expanded-training-library`; tag `checkpoint/forma-alpha05`.
Verified all-refs source bundle, APK copies, checksums and validation evidence:
`D:\Archives\Forma\alpha05-2026-11-04`. Earlier Forma checkpoints and independently
preserved Nemory remain available.

## Remaining gates

Qualified review of every pose and translated cue; actual beginner usability study;
physical Android/minimum API/long-history/accessibility/battery/interruption checks;
supported-hardware Nano inference; owner signing/privacy/store decisions.
Swift package tests, native iPhone/watch builds and paired delivery still require
Mac/Xcode and actual hardware. No HealthKit recording, live Android↔Watch sync,
automatic calorie/load prescription, public release, recruitment or external messages
are claimed.

See [expansion/compatibility](ALPHA05_EXPANSION.md),
[official-source feature review](COMPETITOR_REVIEW_2026_11_04.md) and
[remaining delivery work](ROADMAP.md).
