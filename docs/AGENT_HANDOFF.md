# Forma — Agent Handoff & Current State

**Last updated:** 22 November 2026  
**Repository:** `d:\Projects\Forma` (`https://github.com/7atem/Forma`)  
**Branches:** `main` and `codex/guided-fitness-workflows` (kept in sync)  
**Product isolation:** Never touch Nemory (`D:\Vault Brain` or `https://github.com/7atem/NEMORY`). Follow all rules in `AGENTS.md`.

---

## 1. Completed & Verified Work

### Commit `7052f22` (CI ✅ Green on Android + `macos-15`)
- **50 Exercises & Compressed 3D Illustrations:** Expanded from 40 → 50 exercises (`BARBELL_BENCH_PRESS`, `INCLINE_BARBELL_PRESS`, `DIP`, `CONVENTIONAL_DEADLIFT`, `BARBELL_ROW`, `BARBELL_OVERHEAD_PRESS`, `CABLE_LATERAL_RAISE`, `BARBELL_CURL`, `SKULL_CRUSHER`, `HANGING_LEG_RAISE`), compressed all 50 two-phase PNGs to `960×640` quantized PNGs (`69.90 MB → 18.09 MB`; `FormaPhone-unsigned.ipa` shrunk from ~153 MB → ~40 MB).
- **XML Escaping Fix:** Fixed `scripts/generate_exercise_guides.py` and `scripts/generate_exercise_catalog.py` to escape Android XML special characters (`'`, `"`, `\`, leading `@`/`?`).
- **UI/UX Overhaul (Round 1) on Android & iOS:** Deep Forest + Lime Dark & Light themes, tabular numbers, scrubbable charts, workout victory summary with session muscle heat map, ghost previous-set values, undo snackbar, quick-log sheet, haptics, PR detection (Weight + Epley e1RM, excluding first-ever set), 12-week consistency grid, radial rest timer, Olympic barbell plate visualizer + warm-up ramp, 3-segment macro donut, 30 USDA CC0 starter whole foods, `Clock` injected via Hilt, `Mutex`-queued writes in `FitnessViewModel`.
- **iOS 3D Image Fix:** `FormaExerciseImage` + `ExerciseImageCache` loads PNGs from `drawable-nodpi/` subdirectory in the `.app` bundle.

### Commit `a64ba35` + `c3118c9` (CI ✅ Green on Android + `macos-15`)
- **Full `kg / lb` Weight Unit Toggle (Android & iOS):**
  - Canonical persistence, backups (`FORMABK1`), and watch transfers remain in `kg`.
  - Display/input unit preference stored in `SharedPreferences("forma_ui_prefs")` (Android) and `@AppStorage("forma_use_lb")` (iOS), documented in `docs/VALIDATION.md`.
  - **All `kg/lb` reassessment bugs fixed in `c3118c9`:**
    - Centralized `formatWeight`, `formatWeightInput`, `formatInputNumber` (`MainActivity.kt`) and `WeightUnits.convert(value, from, to)` (`WeightUnits.kt`).
    - Fixed "Use last set" in `TrainingScreen.kt` to convert stored `kg → unit` before pre-filling.
    - Fixed "Edit set" (`SetDialog`) to pre-fill and save in the active unit.
    - Added `LaunchedEffect(unit)` mid-entry conversion in active workout, set editor, body-weight input, and quick-record sheet so toggling `kg ↔ lb` mid-entry preserves the physical load.
    - Updated all weight displays (previous-set hint, tonnage, PR wall, load/e1RM charts, set cards, measurement history, weekly review, coach evidence, unit-aware `PlateDialog` with `20 kg`/`45 lb` bars & plate sets, and all iPhone views in `FormaPhoneApp.swift`).
    - Added round-trip `lb` repeat & mid-entry switch unit test in `DailyWorkflowTest.kt`.
- **Correctness & Clock Hygiene (`c3118c9`):**
  - Added transactional `FitnessDao.restoreSet(row, orderedSessionSetIds)` so Undo restores a deleted set with its original ID at its exact original position and safely no-ops if the session was finished/deleted.
  - Added synchronous `if (vm.state.value.busy) return@Button` double-tap guards on submit buttons.
  - Replaced remaining `LocalDate.now()` calls in `FitnessViewModel.selectHighlights` and `BackupDialog` with injected `clock`.
- **Adaptive Light & Dark Mode on iPhone (`FormaTheme.swift`, `FormaPhoneApp.swift`).**
- **Docs Updated:** `docs/VALIDATION.md` and `docs/EXERCISE_DEMOS_AND_UX.md`.

### Round 2 Commit (Exercises Main Tab, Direct Recording & Moving Demos with Marked Muscles — Local Gradle ✅ Passed, CI Triggered)
- **4-Tab Navigation with `Exercises` as Tab 0 (Main / Default) on Android & iOS:**
  - Android (`NavigationChrome.kt`, `MainActivity.kt`): **`Exercises` (Tab 0) · `Train` (Tab 1) · `Eat` (Tab 2) · `You` (Tab 3)** (`You` contains a clean segmented hub for `Today + Milestones | Progress | Coach`).
  - iPhone (`apple/iPhone/FormaPhoneApp.swift`): **`Exercises` (Tab 0) · `Train` (Tab 1) · `Eat` (Tab 2) · `Progress` (Tab 3)**.
  - Persistent **Mini Active-Workout Bar** on non-Train tabs showing active session name, last logged exercise, and set count with 1-tap jump to `Train`.
- **Record Exercises Directly From Inside the `Exercises` Tab (Android & iOS):**
  - Added transactional `FitnessDao.quickLogExerciseSet`, `FitnessRepository.quickLogExerciseSet`, `FitnessViewModel.quickLogExerciseSet`, and iOS `LocalStore.quickLog(_:label:)` — appends to an active session if open, or starts a quick session for today in one transaction.
  - Every exercise card and Exercise Detail sheet now has a 1-tap **"Record" / "سجّل"** button opening a unit-aware (`kg/lb`) **`QuickRecordExerciseSheet`** (`ModalBottomSheet` on Android / `PhoneQuickRecordSheet` on iOS).
- **Moving Exercise Demos with Live Marked Affected Muscles Overlay (Android & iOS):**
  - Added `CompactMuscleOverlay` (`MuscleMap.kt`) and `CompactMuscleOverlayView` (`apple/AppShared/MuscleDiagram.swift`) — translucent floating front+back body schematic with Primary muscles pulsing Lime and Supporting muscles in Amber on the Movement/Peak phase.
  - Upgraded `ExerciseGuideSheet` / `AnimatedExerciseIllustration` (`ExerciseGuide.kt`) and `ExerciseDetail` (`ExerciseDetail.swift`) to **auto-animate by default** (smooth cross-fade + `1.0 → 1.035` subtle scale motion + phase progress bar, respecting system Reduce Motion), with the live `CompactMuscleOverlay` on the illustration and Primary/Supporting muscle pills directly beneath.
- **Per-Exercise Rep-Max Ladder & Recorded Milestones:**
  - Added `TrainingHistory.repMaxLadder(snapshot, exercise)` (`core/coach/History.kt`, warm-ups excluded) + unit test in `DailyWorkflowTest.kt`, rendered inside `ExerciseGuideSheet`.
  - Added `MilestonesCard` (`MainActivity.kt`) under `You → Today` celebrating recorded milestones without punishing gaps.
- **Localization:** `668` Android EN/AR keys matched; `112` Apple EN/AR keys matched.

---

## 2. Files Modified in the Latest Round

| Area | Files |
|---|---|
| Core model & coach | `core/model/.../WeightUnits.kt`, `core/coach/.../History.kt`, `core/coach/.../DailyWorkflowTest.kt` |
| Room & Repository | `core/data/.../FitnessDatabase.kt`, `core/data/.../FitnessRepository.kt` |
| Android UI | `app/.../FitnessViewModel.kt`, `MainActivity.kt`, `NavigationChrome.kt`, `ExerciseGuide.kt`, `MuscleMap.kt`, `TrainingScreen.kt`, `ProgressScreen.kt`, `CoachScreen.kt`, `TrainingTools.kt`, `DesignComponents.kt`, `BackupDialog.kt`, `NutritionScreen.kt` |
| Android Strings | `app/src/main/res/values/strings.xml`, `values-ar/strings.xml`, `values/training_tools.xml`, `values-ar/training_tools.xml`, `values/training_watch.xml`, `values-ar/training_watch.xml` |
| Apple iPhone/Watch | `apple/AppShared/LocalStore.swift`, `apple/AppShared/MuscleDiagram.swift`, `apple/AppShared/ExerciseDetail.swift`, `apple/iPhone/FormaPhoneApp.swift`, `apple/Resources/en.lproj/Localizable.strings`, `apple/Resources/ar.lproj/Localizable.strings` |
| Docs | `docs/VALIDATION.md`, `docs/EXERCISE_DEMOS_AND_UX.md`, `docs/AGENT_HANDOFF.md` |

---

## 3. Coordination & Guardrails for the Next Agent

1. **Wait for clean git status / no active Gradle process** before starting edits or Gradle builds (never run Gradle concurrently; always use `--max-workers=1`).
2. **Always run all 5 static checks + full Gradle verification before pushing:**
   ```powershell
   python scripts/check_localization.py
   python scripts/generate_exercise_catalog.py --check
   python scripts/generate_exercise_guides.py --check
   python scripts/check_exercise_art.py
   python scripts/check_apple_sources.py
   .\gradlew.bat :core:coach:test :core:wearables:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease --max-workers=1
   ```
3. **Swift 5 (`macos-15` Xcode 16) gotchas:**
   - Never leave trailing commas before `)` in Swift calls/initializers.
   - Never use tuple keypaths in `ForEach`.
   - Explicitly cast `Double` to `CGFloat` when mixing with `GeometryReader`.
   - Always load 3D PNGs via `FormaExerciseImage` (`drawable-nodpi/` subdirectory), never bare `Image(asset)`.
   - Verify CI via `Invoke-RestMethod "https://api.github.com/repos/7atem/Forma/actions/runs?per_page=6"`.
4. **Hard Product Rules (`AGENTS.md`):**
   - No `INTERNET` permission, no cloud provider. Apple Health is **read-only** on iPhone (`HealthImporter.swift` only); no share/write permission, `HKWorkoutSession` or sensor recording — `check_apple_sources.py` enforces this.
   - No automated diet/exercise prescription; missing logs are unknown, never nonadherence.
   - Never mirror exercise demonstration left/right phase order in RTL.
   - Never hand-edit generated Kotlin/Swift/XML catalog or guide files.

---

## 4. Recommended Next Priorities (Remaining Roadmap)

1. **Per-feature Room `Flow`s (`FitnessRepository.kt`):** Replace the whole-database `FitnessSnapshot` reload on every write with scoped `Flow`s (active session + sets, today's meals, paged history), keeping full snapshot for backup and weekly review only.
2. **Split large UI files & Gradle catalog:** Break `TrainingScreen.kt`, `MainActivity.kt`, and `FormaPhoneApp.swift` into feature sub-files; introduce `gradle/libs.versions.toml` + Compose BOM.
3. **Replace remaining dialogs with Bottom Sheets / full-screen editors:** Program editor, food/recipe editor, and plate calculator.
4. **Active Workout Mode enhancements (Round 2 Tier 2):** Plate-aware barbell steppers, "Same as last time" full-workout prefill, mid-workout exercise reorder/swap, and Android rest timer notification.
5. **Room Schema v6 Migration:** Normalized columns replacing JSON text payloads + custom user exercises + replacing deprecated `androidx.security:security-crypto` with direct Keystore AES-GCM + backup schema versioning.
6. **Emulator instrumented tests (`:app:connectedDebugAndroidTest`):** Re-run full UI test suite against the new 4-tab layout (`Exercises · Train · Eat · You`) and update any outdated tab-index assertions if needed.
