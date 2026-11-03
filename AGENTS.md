# Forma implementation guide

Independent Android project, working name Forma / فورمة. Nemory is a separate product and remains at `D:\Vault Brain`. Never rename, rewrite, migrate or delete that checkout as part of Forma work. See `docs/NEMORY_PRESERVATION.md` before proposing shared-source changes.

- Kotlin, Jetpack Compose, Hilt, Room with SQLCipher. Android first. No iOS implementation claim.
- Separate ID `com.formacoach.app`, database `forma.db`, key filename and Keystore alias. Never reuse Nemory signing credentials or read its database.
- New typed fitness entities; do not use VaultItem, lens IDs or arbitrary metadata as the fitness schema.
- All visible strings in `values/strings.xml` and `values-ar/strings.xml`. Support RTL and localized numeric input.
- Core logging and deterministic reports work without an LLM or network. Missing logs are unknown, not evidence of nonadherence.
- Calculations use typed records, local calendar dates and an injected clock. AI must not invent nutrition values, medical facts or recorded history.
- No automated diet or exercise prescription until reviewed rules, boundaries and regression cases exist. User-entered goals are explicit user inputs.
- No health diagnosis, injury treatment, automatic calorie changes or silent plan writes. Future coach proposals need confirmation and source evidence.
- All external APIs must be free, public and keyless. Review data licensing, attribution, coverage and rate limits before integration.
- Never copy secrets, release signing keys, private data, large datasets or downloaded model files from Nemory into this repository.
- Preserve Apache-2.0 notices and record the source commit, original hash and adaptation for copied code in `docs/NEMORY_REUSE_AUDIT.md`.
- Prefer narrow extraction when a feature needs it; no generic platform rewrite or forced cross-repository dependency.
- Build and checks: `.\gradlew.bat :core:coach:test :app:assembleDebug :app:lintDebug --max-workers=1`. Do not run Gradle/clean concurrently.
- Keep README, architecture, roadmap and validation status honest and aligned with source.
