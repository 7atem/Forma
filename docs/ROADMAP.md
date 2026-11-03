# Delivery plan

Current development version: **0.4.0-alpha04**. Nemory remains an independent preserved project. Checked items describe implemented software and verified checks, not clinical review, market validation or a public-release approval.

## Completed foundation and daily workflow

- [x] Independent repository/application ID, encrypted database, Nemory preservation and alpha01 checkpoint.
- [x] English/Arabic profile, manual goals and Today dashboard.
- [x] Editable ordered saved programs, a user-selected rest timer, set logging and corrections, safe completion and history.
- [x] Past-date entry/correction, session/meal search, body-weight and exercise-load charts.
- [x] Typed foods, source attribution, grams/servings portions and immutable recipe/meal snapshots.
- [x] Portable encrypted backup with a preview, explicit restore and transactional rollback; password/size/tamper validation.
- [x] Encrypted Room 1→2 migration preserving alpha01 records.
- [x] Deterministic weekly reports with coverage, evidence and local record questions.
- [x] Weekly check-ins and explicit confirm/cancel scheduling proposals with stale-target protection.
- [x] Optional Gemini Nano adapter for bounded selection of existing facts, manual availability/download controls and independent deterministic fallback.
- [x] Alpha02 baseline: 25 JVM and 13 emulator cases passed. Current results are in `VALIDATION.md`.
- [x] English tablet and Arabic phone-viewport / large-text visual review; detailed scope in validation.

## M1 external validation and remaining usability work

- [ ] Run the 10–15-person study in `USABILITY_STUDY.md`; apply observed fixes. Nobody has been recruited by this implementation.
- [ ] Review exercise descriptions with qualified reviewers. Current exercise names/logging notes are not technique coaching.
- [ ] Build a verified Arabic/Egyptian food pilot after data licensing and coverage review. Current foods are user supplied; no invented catalog.
- [ ] Test backup/provider interruptions and recovery across physical devices, process death and reboot.
- [ ] Complete TalkBack, minimum-supported Android, physical phone/tablet and long-history/battery testing.

## M2 adaptive-coaching release

- [x] Separate observation, user context and an explicitly requested scheduling proposal.
- [x] Confirm/cancel, stale-state guards, source windows and deterministic regression tests.
- [ ] Define and approve actual training/nutrition adaptation rules, exclusion criteria and sufficient-history requirements with qualified review.
- [ ] Add reviewed proposals for those rules only after their boundaries and regression cases exist. No automatic calorie/load prescriptions are currently shipped.
- [ ] Run a consenting 30–50-person beta and examine actual repeat usage and review comprehension.

## M3 optional intelligence and integrations

- [x] Audit/adapt the narrow ML Kit local text/status/download path and verify fail-closed model output/cancellation behavior.
- [x] Keep the record tools usable without a model and remove application INTERNET permission.
- [ ] Verify actual Nano download/inference on supported physical hardware; inspect SDK/service metadata and privacy disclosures before advertising support.
- [ ] Evaluate a licensed downloadable text fallback for non-AICore devices with measured memory, latency and battery limits.
- [ ] Add reviewed OCR/barcode or voice entry only when it reduces validated logging friction. Latin OCR does not imply Arabic OCR support.
- [ ] Add user-chosen reminders and Health Connect through explicit permission/privacy flows when product scope calls for them.
- [ ] Keep full CPU vision out of normal production paths. A compatible licensed encoder needs its own audit and validation.

## Release and payment

A minified unsigned release build is part of local engineering verification. Final brand/store identity, owner-controlled signing credentials, privacy/dependency review, physical validation and beta outcomes are still required. No store publication, cloud sync, payment setup or trainer dashboard is claimed. Native phone/watch source is not yet built or device-validated. Prices and revenue remain hypotheses. See `RELEASE_CHECKLIST.md`.

## Alpha03 anatomy and Apple companion

- [x] Primary/supporting muscle highlights for all eight current exercises, original front/back geometry, shared generated Kotlin/Swift catalog and bilingual labels.
- [x] Working/warm-up sets, optional RPE, previous-value copy, best recorded working load and seven-day muscle associations.
- [x] Encrypted schema 2→3 migration and bounded transactional Android workout merge with preview/deduplication.
- [x] Primary-source feature research in `COMPETITOR_FEATURE_RESEARCH.md`.
- [x] Native phone/watch source: offline logging/editing, muscles, rest countdown, persisted drafts/outbox and explicit Android file bridge.
- [ ] Compile/test native apps on macOS and validate paired WatchConnectivity on physical devices.
- [ ] Verify VoiceOver/RTL, interruptions/recovery, capacity, stale program delivery, background timers and file errors on Apple hardware.
- [ ] Add reviewed HealthKit lifecycle/sensors as a separate permission/product change. Current watch app is a manual logger.
- [ ] Add equipment-specific exercise variants and qualified review; evaluate supersets/custom exercises and fast-entry improvements against the study.

## Alpha04 demonstrations and usability

- [x] Original two-phase 3D assets for every current exercise, named variants and bilingual source-linked cues.
- [x] Searchable exercise library, separate Movement/Muscles details and Android enlarged phase selection.
- [x] Sticky toolbar, adaptive tablet navigation, Today metrics, retained-input training sections and responsive logging fields.
- [x] Sampled cached thumbnails and one shared asset source for Android/Apple.
- [ ] Qualified review of every generated pose/cue before public release; run the actual beginner/form-comprehension study.
- [ ] Complete physical-device, TalkBack/VoiceOver, larger histories, minimum API and Apple native build/device acceptance.
