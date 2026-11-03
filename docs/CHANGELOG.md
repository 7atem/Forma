# Changelog

## 0.2.0-alpha02 — 3 November 2026

Adds editable saved training programs and a rest timer; correction/date entry for sessions, meals and measurements; searchable history and recorded load/weight charts; a sourced personal food library and immutable recipe portions; password-encrypted portable backup with a preview and transactional replacement; weekly check-ins and confirm/cancel scheduling changes; and a limited, optional Gemini Nano fact-highlighting adapter.

Room v1 upgrades to v2 without destructive fallback. Program/library changes do not rewrite old training or meal records. Device timezone changes are observed without recreating repositories, and midnight invalidates old AI highlights. Fractional loads are displayed with up to two decimal places.

AI highlights are known fact IDs only; model prose cannot become a claim or prescription. Model availability/download is user initiated, inference is foreground-bound and time/token-limited, and the application has no INTERNET permission. Deterministic logging and review remain available without a supported model.

This is a development alpha. Actual Nano inference on supported physical hardware, reviewed adaptive training/nutrition rules, the usability study and public-release gates remain open. No catalog data, model weights, clinical validation, subscriptions, cloud sync or store publication are included.

See `VALIDATION.md` for exact final checks and artifacts. The debug APK is for installation/testing; the minified release APK is unsigned. Nemory remains an independent preserved product.

## 0.1.0-alpha01

Initial independent bilingual encrypted logging foundation. Preserved in commit `645b030` and checkpoint `checkpoint/forma-alpha01`.
