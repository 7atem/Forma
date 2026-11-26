# Changelog

## 0.7.0-alpha07 — 26 November 2026

Adds cardio and timed-activity logging: pick an activity, enter minutes and seconds, and optionally a distance (km or miles to match the weight unit). The Train tab shows recent entries, a 7-day total of what was logged and pace for walks and runs. Forma estimates no calories, heart rate or intensity. Entries are typed records in a new encrypted Room v10 table (explicit 9→10 migration), included in FORMABK1 backups; older archives still restore with no cardio.

Each section's (i) button now opens a small bubble with what the section shows, with "More" for full details. Reminders are opt-in per kind (meals, water, workout, weekly look-back) with their own days and time, rotating encouraging messages that never include logged data, a "skip if already logged" option (cardio counts), a +250 ml water action and a one-hour snooze.

iPhone source gains the cardio card, a preview-then-confirm "Copy a day of meals" sheet and a training CSV share in Settings. CI builds and Swift-tests it; nothing has been run on a physical iPhone.

The exercise video player gains tap-to-pause, a progress line and slow motion. Discomfort filters become "Discomfort & focus": neck is separate from upper back, matching exercises are marked instead of hidden by default, and you can pick muscles to train more so they list first. After your first log, Today offers reminders once (nothing turns on without your tap and permission). Progress opens on a 7-day highlights card; measurement entry moved to a sheet.

UX fixes: the Today ring no longer repeats the session count shown beside it, cardio sits below the starter programs, activity chips use the standard size with numeric keyboards, and Arabic water buttons and pace labels read correctly right to left.

## 0.4.0-alpha04 — 4 November 2026

Adds eight original offline 3D two-phase illustrations, named exercise variants, bilingual source-linked movement cues, a searchable visual library and enlarged phase viewer. Sticky chrome, adaptive tablet navigation, Today progress, compact set entry, retained user values, expandable history and responsive food/measurement fields improve the daily flows. Active drafts survive section switches. Room stays at v3; historical IDs, goals and saved records are unchanged.

Shared SwiftUI source consumes the same art/guides but is not yet compiled on a Mac. Generated demonstrations are visually checked examples; expert technique review remains a release gate. See `EXERCISE_DEMOS_AND_UX.md` and `VALIDATION.md`.

## 0.3.0-alpha03 — 3 November 2026

Adds original front/back highlights for all eight current exercises, bilingual primary/supporting muscle labels and seven-day working-set associations. Sets support warm-up and optional subjective RPE; warm-ups are excluded from load history and recorded best loads. Previous set values can be copied before explicit save.

Room upgrades through 1→2→3, retaining old working/local defaults. The watch dialog exports selected programs and previews completed-workout files before a transactional, idempotent merge. Conflicting IDs reject the batch; file contents and the manual iPhone bridge are explicitly disclosed.

Native SwiftUI iPhone/watchOS source includes offline set logging/editing, the same muscle catalog, rest countdowns, persisted drafts, a watch outbox and durable phone ACK. Apple builds and physical delivery are **not verified** on this Windows host. No HealthKit recording, live Android sync or Apple distribution is claimed. Primary-source competitor research, anatomy provenance and Mac build/device gates are documented.

Nemory remains unchanged. See `VALIDATION.md` for alpha03 results and `APPLE_WATCH.md` for native limitations.

## 0.2.0-alpha02 — 3 November 2026

Adds editable saved training programs and a rest timer; correction/date entry for sessions, meals and measurements; searchable history and recorded load/weight charts; a sourced personal food library and immutable recipe portions; password-encrypted portable backup with a preview and transactional replacement; weekly check-ins and confirm/cancel scheduling changes; and a limited, optional Gemini Nano fact-highlighting adapter.

Room v1 upgrades to v2 without destructive fallback. Program/library changes do not rewrite old training or meal records. Device timezone changes are observed without recreating repositories, and midnight invalidates old AI highlights. Fractional loads are displayed with up to two decimal places.

AI highlights are known fact IDs only; model prose cannot become a claim or prescription. Model availability/download is user initiated, inference is foreground-bound and time/token-limited, and the application has no INTERNET permission. Deterministic logging and review remain available without a supported model.

This is a development alpha. Actual Nano inference on supported physical hardware, reviewed adaptive training/nutrition rules, the usability study and public-release gates remain open. No catalog data, model weights, clinical validation, subscriptions, cloud sync or store publication are included.

See `VALIDATION_ALPHA02.md` for the historical alpha02 checks and artifacts. The debug APK is for installation/testing; the minified release APK is unsigned. Nemory remains an independent preserved product.

## 0.1.0-alpha01

Initial independent bilingual encrypted logging foundation. Preserved in commit `645b030` and checkpoint `checkpoint/forma-alpha01`.
