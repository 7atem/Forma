# Contributing to Forma / فورمة

Thank you for your interest in contributing to Forma! Before opening a pull request, please review the project boundaries and verification steps below.

---

## Core Project Rules

1. **Single-source catalogs — never hand-edit generated files.**
   - Muscles & 2D geometry: edit [catalog/exercise-muscles.json](catalog/exercise-muscles.json), then run `python scripts/generate_exercise_catalog.py`.
   - Exercise guides & cues: edit [catalog/exercise-guides.json](catalog/exercise-guides.json), then run `python scripts/generate_exercise_guides.py`.
   - Section info bubbles: edit [catalog/section-info.json](catalog/section-info.json), then run `python scripts/generate_section_info.py`.
   - Procedural 3D motion: edit [catalog/exercise-motion.json](catalog/exercise-motion.json), then run `python scripts/generate_exercise_motion.py`.
2. **Exercise art & videos:**
   - Add or replace stills only via `python scripts/convert_exercise_art.py <id> <original.png>` (`960×640` WebP, quality 92), then run `python scripts/check_exercise_art.py`.
   - Finalize optional videos only via `python scripts/finalize_exercise_video.py`, then run `python scripts/check_exercise_video.py`. Never mark `reviewed: true` without qualified review.
3. **Bilingual parity (English & Arabic RTL):**
   - Every Android string must exist in both `app/src/main/res/values/*.xml` and `values-ar/*.xml`.
   - Every Apple string must exist in both `apple/Resources/en.lproj/Localizable.strings` and `ar.lproj/Localizable.strings`.
   - Support RTL layouts and localized numeric input; never mirror exercise demonstration stills or motion direction in RTL.
4. **Offline-first & non-prescriptive boundaries:**
   - Core logging and deterministic summaries must work without an LLM or network.
   - No paid cloud APIs, user telemetry, health diagnoses, injury treatment, or automated diet/training prescriptions.
   - All external APIs must be free, public, and keyless, with license attribution documented.
5. **Database & backup safety:**
   - Every Room schema change requires an explicit migration and exported schema JSON (`core/data/schemas/`). Never use destructive fallback.
   - Preserve `FORMABK1` backward compatibility and transactional restore semantics.
6. **Honest validation:**
   - Never claim iOS/watchOS runtime validation without Mac/paired-device evidence, and never claim physical Gemini Nano verification without supported hardware.
7. **License & Nemory preservation:**
   - Preserve Apache-2.0 notices in [LICENSE](LICENSE) and [NOTICE](NOTICE). Never copy secrets, signing keys, or private data into this repository.

---

## Local Verification Checklist

Run these checks before submitting a PR:

```powershell
# 1. Localization & single-source catalog checks
python scripts/check_localization.py
python scripts/generate_exercise_catalog.py --check
python scripts/generate_exercise_guides.py --check
python scripts/generate_section_info.py --check
python scripts/check_exercise_art.py
python scripts/generate_exercise_motion.py --check
python scripts/check_exercise_video.py
python scripts/check_apple_sources.py

# 2. Android JVM tests, builds, and lint (run sequentially on Windows)
.\gradlew.bat :core:coach:test :core:wearables:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease --max-workers=1
```

If you are on macOS and touch `apple/`, also run:
```bash
swift test --package-path apple
```

---

## Licensing of Contributions

By submitting a pull request to this repository, you agree that your contributions will be licensed under the [Apache License, Version 2.0](LICENSE) (Section 5, *Submission of Contributions*).
