# Delivery plan

Current development version: **0.2.0-alpha02**. Nemory remains an independent preserved project. Checked items describe implemented software and verified checks, not clinical review, market validation or a public-release approval.

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
- [x] 25 JVM cases and 13 stable-emulator cases passed; 227 bilingual resource keys match.
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

A minified unsigned release build is part of local engineering verification. Final brand/store identity, owner-controlled signing credentials, privacy/dependency review, physical validation and beta outcomes are still required. No store publication, cloud sync, payment setup, trainer dashboard or iOS implementation is claimed. Prices and revenue remain hypotheses. See `RELEASE_CHECKLIST.md`.
