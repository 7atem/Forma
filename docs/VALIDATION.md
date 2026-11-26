# Validation record — current branch (0.7.0-alpha07)

Updated 24 November 2026. Application `com.formacoach.app`, encrypted Room schema 10 (explicit `1→2→3→4→5→6→7→8→9→10` migrations). Development candidate; public release, physical paired-device Apple testing, visual Apple UI acceptance, and qualified exercise/video approval remain pending.

> **Note on UI preferences:** Display preferences (`kg`/`lb` weight unit toggle, Light/Dark theme, and in-app language `System`/`en`/`ar`) are stored in Android `SharedPreferences` (`forma_ui_prefs`) and iOS `@AppStorage` outside the encrypted Room database and portable backups. Canonical persisted training loads, body weights, backups (`FORMABK1`), and watch transfers remain in `kg`.

## Review of commit d85bce5 (26 November 2026)

Five stills (barbell shrug, cable crossover, close-grip bench press, good morning, hip abduction) and two unreviewed videos (dip, incline barbell press) were added by another session. Catalog, guide, art and video checks pass with 74 exercises and 37 videos, all `reviewed: false`. Corrections made in the catalog and regenerated: hip abduction no longer lists core/obliques, barbell shrug no longer lists forearms (which showed as biceps; matches dumbbell shrug and the deadlifts), cable crossover no longer lists core, and its Arabic name no longer says "upper" (the guide is the high-to-low variant). Shrug and good-morning stills are flagged for regeneration in `EXERCISE_DEMOS_AND_UX.md`.

## Video, discomfort and focus, reminder offer, Progress (26 November 2026)

- **Video player:** tap anywhere on the clip to play or pause, a large play button while paused, a control bar with play/pause, a progress line and a 1×/0.5× slow-motion switch (viewing speed only). Under Reduce Motion it starts paused and a tap starts it (Android and iPhone).
- **Discomfort and focus:** neck (upper traps) and upper back (mid back) are separate areas; matches are marked by default or hidden by choice; "Muscles I want to train more" lists those exercises first. The legacy combined value keeps its old effect. Tests: `ExerciseDiscomfortTest` (JVM), `ExerciseDiscomfortTests` (Swift, CI).
- **Reminder offer:** a one-time Today card (top of Train on iPhone) after the first logged meal, workout or cardio entry while all reminders are off. One tap turns on meal and workout reminders only after the system permission; "No thanks" hides it for good. Nothing turns on by itself.
- **Progress:** opens on "Your last 7 days" (workouts vs plan, cardio time, average kcal on logged days with the user's target, 30-day weight change from logs, new bests this week), then records and charts; measurement entry moved to a sheet. Test: `ProgressHighlightsTest`. iPhone shows the same card without the plan and records lines.
- **Not verified:** iPhone layouts and playback are compiled by CI only.

## Cardio and iPhone parity (26 November 2026)

- **Cardio/timed activity:** `CardioEntry` (1 s–24 h, optional distance up to 1 000 km, note ≤ 60 characters) in table `cardio_entries`, added by `MIGRATION_9_10` whose SQL matches the v10 schema export. Included in FORMABK1 backup validation (unique IDs, date range) and restore; `{}` archives decode with no entries. Counts as training for the reminder skip rule. Tests: `cardioEntriesRoundTripAndInvalidValuesFail` (JVM), `schemaNineUpgradeAddsCardioThatRoundTripsThroughBackupAndCountsAsTraining` and `cardioEntryAcceptsArabicDigitsPersistsAndRemovalNeedsConfirmation` (emulator).
- **Emulator walkthrough (phone 1080×2400, EN and AR):** onboarding, Today, Train, Eat, Progress and the cardio dialog checked. Fixed: duplicate count in the Today ring, oversized activity chips, cardio placement above starters, Arabic `+250 مل` sign order, Arabic pace wording and a mixed-digit 7-day label.
- **iPhone source:** `PhoneCardio.swift`, `PhoneCopyMealDay.swift` (copies exactly the reviewed meals, rejects a changed source day without partial writes) and a CSV share from memory using FormaCore `TrainingCsv` (`TrainingCsvTests`, columns match Android). **Compiled only by CI; not run on an iPhone.**

## Reminders and info bubbles (25 November 2026)

- **Info bubbles:** a tap on (i) now shows a small bubble next to it with the section title and what it shows (Android M3 rich tooltip, iPhone popover kept as a popover on compact width). "More" opens the full three-part dialog on Android and expands the bubble in place on iPhone. Test: `sectionInfoBubbleShowsPurposeThenMoreOpensFullDetails`.
- **Reminders (Android + iPhone):** four opt-in kinds (meals, water, workout, weekly look-back), each with its own time and weekdays (at least one stays chosen; the weekly one defaults to the last day of the local week). Messages are encouraging, rotate (Android by date, iPhone by weekday) and never include logged data; titles and bodies are EN/AR. "Skip if already logged that day" is on by default for meals, water and workouts and reads only whether a record exists for today on this device; an unreadable log still shows the reminder. Tapping opens Eat, Train or Progress. Buttons: "+250 ml" on water (adds water only after a tap, capped at 20 000 ml, one database transaction) and "In 1 hour". Settings show an example message and "Send a test now", plus a link to system notification settings when blocked.
- **Android details:** daily inexact alarm, weekdays filtered when it fires; alarms and buttons go to a non-exported `ReminderAlarmReceiver`, so other apps cannot add water; the exported receiver only re-arms after boot/time changes and retires the old alarm. One channel per kind with a monochrome Forma status-bar icon. Tests: `ReminderTimeTest` (days, skip rule, rotation), `reminderChecksSeeOnlyThatDayAndWaterButtonAddsWithinLimit`, `remindersAreOffByDefaultAndSwitchOnAndOff` (days, skip switch).
- **iPhone details:** chosen weekdays repeat weekly; when today is already logged, today's weekday becomes single reminders for the next four weeks, so later weeks still arrive if the app is not opened. Re-armed when the app comes forward and when today's log changes. **Not compiled or run on an iPhone yet; delivery, buttons and tab opening are unverified.**

## Section info buttons (25 November 2026)

- **What changed:** every main section has an (i) button that opens a short explanation in three parts: what it shows, how it works and good to know. There are 35 Android sections (14 old one-line notes replaced, 21 new) and 20 iPhone sections. The text was checked against the code (review rules, Epley estimate, how saving replaces a record, quick re-log ranking, online lookup saving to My foods, Apple Health merge) and lives only in `catalog/section-info.json`. `scripts/generate_section_info.py` writes the Android EN/AR resources, a Kotlin enum and a Swift file; CI runs it with `--check`. The iPhone weight cards have their own text because they differ from Android (30 recorded days, 7-day trend line, a weight is required).
- **Accessibility:** the button reads "About <section>", is at least 48 dp (Android) or 44 pt (iPhone), each part has a heading, the text scrolls at large font sizes, and Close is a 48 dp button.
- **Tests:** `sectionInfoButtonExplainsWhatHowAndCaveatsAndCloses` and `everySectionExplanationHasTextInEachPart` (Android), `SectionInfoTests` (Swift, needs a Mac). **iPhone layout has not been viewed on a device.**

## App size and database unlock (25 November 2026)

- **Exercise stills:** the 69 two-phase images ship as 960×640 WebP at quality 92 (`scripts/convert_exercise_art.py`), 2.5 MB instead of 30 MB of PNG; worst sampled PSNR against the PNG originals about 44 dB. `check_exercise_art.py` parses WebP headers without Pillow and rejects unlisted files. Android loads them by resource name; the iPhone/watch loader tries WebP then PNG (native decoding on iOS 17 / watchOS 10). **Not yet viewed on an iPhone or watch.**
- **Database unlock (Android):** measured on the API 35 emulator, opening with the stored key as a passphrase took 1.0–1.35 s (SQLCipher PBKDF2); the same key as a raw key took about 20 ms. New databases now use raw mode (marker `forma_db_key_mode`, written before creation); existing databases keep passphrase mode and are never re-keyed. `EncryptedStorageTest`: 21/21, including `newDatabaseUsesTheStoredKeyAsRawKeyAndStaysEncrypted` (file is not plaintext, passphrase-mode key is rejected, reopen works) and `existingPassphraseDatabaseIsNeverRekeyedOrMarkedRaw`. First-run Keystore key creation (about 2.3 s on the emulator) is unchanged. **Not measured on a physical phone.**

## UI/UX pass 4 (25 November 2026)

Audit on the API 35 emulator at phone size (1080×2400, 420 dpi), release build, fresh install, English. Fixes (Android):
- **12-week grid:** today was filled with the "1 recorded workout" colour even with no workout; it is now outlined only.
- **Eat:** two one-tap repeat rows did the same job (one unlabelled, no kcal unit). One labelled "Quick re-log" row remains under the meal-slot picker; it uses the chosen slot, ranks by frequency, keeps newest-first ties (no longer by random ID) and has 48 dp chips.
- **Sparse data:** a single weigh-in shows its value and "record another day to draw a line" instead of a lone dot; an empty 7-day heat map is one sentence instead of two grey figures and a legend.
- **Profile/onboarding:** height moved to the "about you" card; target labels share one short format ("Calories · kcal"); weekly review counts read "recorded days: 1" instead of "1 observations".
- **Tests:** `frequentMealTiesKeepNewestFirstOrderAndTemplate` (unit) and `sparseDataExplainsWhatIsMissingInsteadOfDrawingEmptyGraphics` (component).
- **Not changed:** the encrypted-database unlock showed for several seconds on this overloaded emulator (host RAM nearly exhausted, other apps also hit ANRs); not measured on a real device. iPhone needed none of these fixes except where noted earlier.

## Large-text and Arabic sweep (25 November 2026)

All four tabs captured at **200% system text** on the phone image, plus the new cards in Arabic RTL. Fixes:
- New shared `FitText` label: navigation items and segmented tabs step down in size only as needed instead of truncating ("Progre…") or breaking mid-word ("Program/s").
- 12-week grid title no longer collapses beside its streak badge; macro rows give the label the free width so it never collides with its value; the water buttons move under the title with large text.
- Arabic: numeric fractions ("2 / 7", "20 / 150 g") are wrapped in left-to-right isolates (the app's existing convention) so RTL no longer reverses them; new Arabic strings use the app's Western digits, and the training-trends title uses parentheses because a middle dot beside the number read like an Arabic zero ("120").

## Feature set: trends, reminders, Health Connect (25 November 2026)

- **Nutrition trends (Android + iPhone):** 7/30-day logged calories with the user's own calorie-target line, average macros per logged day, days within 10% of the calorie target and at/above the protein target. Unlogged days are dashed outlines, excluded from averages. Shared logic tested in `core:coach` (`TrendsTest`) and `FormaCore` (`TrendsTests`).
- **Training trends (Android + iPhone):** 12 rolling weeks of working sets, sessions and external-load volume; warm-ups and unfinished sessions excluded.
- **Logging reminders (Android + iPhone):** meals, water, workout; off by default; per-kind time; Android uses inexact daily alarms re-armed after reboot/time-zone change (`ReminderTime` unit-tested; UI test toggles one on/off); iPhone uses repeating local notifications after permission.
- **Health Connect (Android):** read-only weight and exercise sessions for the last 90 days after permission; copies stored in separate encrypted tables (schema 8), de-duplicated by record ID, excluded from logs/stats/backups, removable; rationale screen wired. Storage/migration covered by `EncryptedStorageTest`. **Not tested against a device with real Health Connect data.**
- **BMI (Android + iPhone):** optional profile height (50–272 cm, schema 9); BMI shown as a number from the latest logged weight, no category labels. `BodyAndSleepTest`, `SleepTests`, UI test `heightInProfileShowsBmiFromLoggedWeight`.
- **Sleep (Android + iPhone):** read-only sleep sessions from Health Connect / Apple Health, summarized per wake date keeping one source per night; 7-night chart with unknown nights dashed. Stored with other health copies (excluded from logs/stats/backups, removable). Apple's Sleep Score is not available to apps and Forma computes none. Migration covered by `schemaEightUpgradeAddsHeightAndSleepWithoutTouchingData`. **Not tested with real sleep data on either platform; iPhone UI not viewed on a device.**
- **CI:** trends commit passed GitHub iOS (`swift test`, simulator builds) and Android workflows.

## UI/UX pass 3 (24 November 2026)

Audit on the API 35 tablet (fresh install, onboarding, tablet rail layout, Arabic RTL) and the phone at 150% text. Fixes:
- **Localization (Android + iPhone):** macro values, targets, the widget and iPhone diary/search/food tools no longer show a hard-coded Latin "g", "kcal" or English P/C/F letters; Arabic shows جم/سعرة and macro names.
- **Water logging (Android):** buttons read "+250 ml"/"+500 ml" with spoken names, and the remove button grew from 38 dp to 48 dp.
- **History (Android):** muscle rows put the name and "N main · N supporting sets" on separate lines (a long name previously collapsed to one letter per line under large text, drawing a tall empty box) and speak one sentence; the load-history note no longer squeezes beside the guide link.
- **Tests:** new accessibility test for the muscle row. Tablet onboarding, Coach and Arabic Eat screens were checked and needed no change.

## Android UI/UX pass 2 (24 November 2026, uncommitted)

Audit on the `Pixel_9_Pro` image at 150% and 100% text, light and dark. Changes:
- **Eat:** the day's logged entries now follow the summary card instead of sitting below every entry tool (about half the previous scroll length). Entry tools are grouped under "Add food"; the 7-field manual form opens on demand; the separate "recent portion" list that duplicated the one-tap repeat chips (which confirm with Undo) is removed; the food library opens directly under the toolbar; meal cards no longer repeat calories twice; the per-day search appears only for long lists.
- **Progress:** the weekly-review title no longer wraps one syllable per line (date range moved below it); review tiles no longer print each number twice; bodyweight records read "bodyweight × 10" instead of "0 kg × 10"; streak badge reads "Active weeks in a row: N".
- **Train:** tab labels stay centred when large text wraps them, "My programs" shortened to "Programs"; only the first starter program keeps a solid Start button (and only until the user saves a program), the rest use tonal buttons.
- **Workout set form:** when a number field has the full width (large text, or narrow screens), its −/+ buttons now sit beside it (56 dp) instead of on a separate full-width row, so *Save set* is much closer to the exercise; side-by-side fields keep the buttons below. The "Previously logged" sentence stacks above *Copy previous values* with large text instead of breaking mid-word.
- **Exercise library:** removed a second chip row that duplicated the muscle dropdown filter.
- **Follow-up (same day):** Android Progress now lists this week's working sets per muscle with main and supporting counts kept separate under the heat map (matching iPhone); the workout form guide card and *History* stack with large text. iPhone: the Eat diary moved directly under the summary, meal deletion asks for confirmation, diary icon buttons have 44 pt targets, the streak reads "Active weeks in a row: N" and bodyweight records read "bodyweight × reps". iPhone changes are verified only by GitHub `macos-15` CI builds, not on a device.
- **Tests:** 918 EN/AR keys match; builds pass, lint 0 errors; **42/42** `connectedDebugAndroidTest` on `Medium_Tablet` API 35 (meal tests now open the manual form first).
- **Performance:** not changed. Frame timing on this emulator is dominated by software GPU rendering (≈5 s 90th-percentile GPU frames), so it is not a valid measure; real-device profiling is still needed.

## Android custom exercises & supplements storage fix (24 November 2026, uncommitted)

Android custom exercises, their sets, supplements and daily supplement check-offs moved from plain-JSON `SharedPreferences` into typed tables in the encrypted Room database (schema **7**, explicit `6→7` migration) and into `FORMABK1` backup/restore with reference and duplicate validation. The previous silent trimming (newest 2,000 sets, 90 days of check-offs) is removed; adding stops at 100 custom exercises / 40 supplements instead. Deleting an exercise or supplement now asks for confirmation and states that its history goes too. Existing preference data is copied once after the database loads; the plain copies are deleted only after the database commit, and unreadable data is left in place rather than partly imported. Supplement shortcuts no longer pre-fill doses. The app module now applies the Kotlin serialization plugin; without it the earlier preference code could not serialize its classes.

| Check | Result |
|---|---|
| Android localization | **910 EN/AR keys** match |
| JVM tests, debug/release builds, lint (`--max-workers=1`) | coach 52, wearables 5, backup **10** (new round-trip/reference test) — 0 failures; builds pass; lint 0 errors |
| Full `:app:connectedDebugAndroidTest` on `Medium_Tablet` API 35 | **42 passed, 0 failed**, including `6→7` upgrade + backup round trip, one-time preference import, and create/log/recreate/confirm-delete UI flow |
| iPhone | Unchanged (already stores these records in `forma.json`); not rebuilt |

Follow-up the same day: the canonical catalog gained `singleDumbbell` (goblet squat, overhead dumbbell extension). Android and iPhone now show "log the one dumbbell you hold" for those instead of a doubled "pair total"; generator check, a new coach test (53 total) and `check_apple_sources.py` (266 EN/AR keys, run against the committed 3D scene file) pass. `docs/RIGGED_3D_MODELS.md` now states the `.usdz` loader is iPhone-only, untested on device, with no models bundled and no Android loader.

## Latest checkpoint (24 November 2026 — 61 exercises, 21 detailed muscles, Supplements, CSV exports & iOS parity)

| Check | Result |
|---|---|
| Canonical/generated catalog (`generate_exercise_catalog.py --check`) | **61 stable exercises**, equipment & 14 simplified / **21 detailed muscle regions** (including `FOREARMS`); no drift |
| Shared 3D motion (`generate_exercise_motion.py --check`) | **12 synchronized clips** (5 original + 7 new) + native iOS `.usdz` rigged character loader; reach, floor, plate clearance & loop continuity verified across Kotlin and Swift |
| Shared movement guides & art (`generate_exercise_guides.py --check`, `check_exercise_art.py`) | **61 bilingual guides** and **61 compressed 960×640 two-phase PNGs**; hashes, XML escaping & packaging match |
| Bundled exercise video (`check_exercise_video.py`) | **31** bundled clips; valid files/manifest, **qualified review pending** |
| Android localization (`check_localization.py`) | **906 EN/AR keys** match |
| Apple source checks & localization (`check_apple_sources.py`) | **249 EN/AR keys** match, plus Custom Exercises, native barcode scanner, Food-Swap, 42 starter foods/Arabic staples, Saved Meal Combos, Supplements & Vitamins, Home/Gym filter, Freestyle workout, expanded tape measurements, weekly primary/supporting muscle split, read-only HealthKit, discomfort filter & Family-Pot removal assertions. **Swift/Xcode NOT built on Windows; verified via `macos-15` GitHub Actions CI.** |
| Android JVM tests, debug/release builds, lint (`--max-workers=1`) | Verified on Windows (`:core:coach:test` including Training/Nutrition/Measurements CSV & 21-region muscle roll-up, `:core:wearables:test`, `:core:backup:test`, `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleRelease`, `:app:compileDebugAndroidTestKotlin`) |
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
