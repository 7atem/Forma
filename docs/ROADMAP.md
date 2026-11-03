# Delivery plan

The first implementation is a foundation alpha, not the complete adaptive coach. Status must follow source and verification.

## Foundation alpha — current scope

- [x] Preserve Nemory with recoverable history and local configuration.
- [x] Independent Android repository, ID, data model and encrypted database.
- [x] Bilingual profile and optional manual targets.
- [x] Two simple logging templates, exercise/set logging, session completion/history.
- [x] Whole-portion meal entry and repetition from recent history.
- [x] Daily weight/waist records and deterministic seven-day review.
- [x] Evidence references, coverage gates, injected review clock, Arabic numeric parsing.
- [x] Foundation validation recorded in `VALIDATION.md`: 8 JVM and 4 emulator cases pass; bilingual resources match; debug APK built; lint has 0 errors and one empty-directory warning.

## M1 — make the daily workflow dependable

1. Run 10–15-person usability study; simplify set and food entry based on observed friction.
2. Add reviewed exercise descriptions, editable saved programs, progression history and timer.
3. Add date selection/backfill, correction of completed workouts, history/search and charts.
4. Add typed food/recipe/portion records with units, source attribution, uncertainty and a curated Arabic/Egyptian-food pilot. Validate coverage and licenses before choosing a keyless catalog. Never seed invented nutrient values.
5. Port portable encrypted backup/restore with a new format namespace, independent credentials, rollback and device tests. Do not reuse the Nemory archive/database schema.
6. Complete accessibility, screen-size, RTL, process-death and physical-device tests.

## M2 — reviewed adaptive-coaching MVP

1. Define program and nutrition rules with qualified review; make exclusions and data requirements explicit.
2. Separate observation, interpretation and proposed change. Require enough history and an explicit weekly check-in; allow corrections and rejection.
3. Add deterministic regression scenarios for missing/inconsistent data, interrupted training, weight fluctuations, timezone changes and misleading correlations.
4. Add structured weekly proposals and confirm/cancel UI. Never write through an answer stream.
5. Measure usefulness and weekly return with 30–50 consenting beta users. No monetization claim before evidence.

## M3 — optional local intelligence

1. Extract LLM contracts/runtime only after dependency, license, cancellation, memory, battery and device-capability audit.
2. Add grounded chat over typed fitness history, with evidence and clear missing-data answers. Keep all numeric calculations in deterministic tools.
3. Reuse optional OCR for food labels with review. Latin OCR does not establish Arabic OCR support.
4. Evaluate text/voice input, reminders and explicit scheduled permissions when needed. Health Connect requires new product and privacy review; Nemory's disabled integration is not a shipped feature.
5. Keep full CPU vision out of normal production paths. Validate a suitable encoder and model licenses before any photo-based feature.

## Release and payment experiment

Usability/retention results, reviewed rule behavior, data export/restore, migrations, privacy disclosures, accessibility, device/battery testing and release-signing setup precede public release. A paid tier and price are later experiments. Trainer dashboards and iOS are separate roadmap decisions. No paid/key-required external API dependency.
