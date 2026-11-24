# Forma implementation guide

Independent Android project, working name Forma / فورمة. Nemory is a separate product and remains at `D:\Vault Brain`. Never rename, rewrite, migrate or delete that checkout as part of Forma work. See `docs/NEMORY_PRESERVATION.md` before proposing shared-source changes.

- Kotlin, Jetpack Compose, Hilt, Room v6 with SQLCipher and explicit 1→2→3→4→5→6 migrations. Native iPhone/watchOS source under apple/ is unbuilt on this Windows host. Never claim Apple runtime validation without Mac/paired-device evidence.
- Separate ID `com.formacoach.app`, database `forma.db`, key filename and Keystore alias. Never reuse Nemory signing credentials or read its database.
- New typed fitness entities; do not use VaultItem, lens IDs or arbitrary metadata as the fitness schema.
- All Android strings in matching `values/*.xml` and `values-ar/*.xml`; Apple strings in `apple/Resources/en.lproj` and `ar.lproj`. Support RTL and localized numeric input.
- Core logging and deterministic reports work without an LLM or network. Missing logs are unknown, not evidence of nonadherence.
- Calculations use typed records, local calendar dates and an injected clock. Foods/recipes and logged portions retain source snapshots; edits never silently rewrite history.
- Optional Gemini Nano only selects known computed fact IDs, capped at 64 tokens / 15 seconds, foreground-only. Never render raw model prose. Application INTERNET permission is scoped strictly to free, public, keyless food lookup APIs (Open Food Facts, ODbL); no paid cloud provider or user-data telemetry.
- No automated diet or exercise prescription until reviewed rules, boundaries and regression cases exist. User-entered goals are explicit user inputs.
- No health diagnosis, injury treatment, automatic calorie changes or silent plan writes. Future coach proposals need confirmation and source evidence.
- All external APIs must be free, public and keyless. Review data licensing, attribution, coverage and rate limits before integration.
- Never copy secrets, release signing keys, private data, large datasets or downloaded model files from Nemory into this repository.
- Preserve Apache-2.0 notices and record the source commit, original hash and adaptation for copied code in `docs/NEMORY_REUSE_AUDIT.md`.
- Prefer narrow extraction when a feature needs it; no generic platform rewrite or forced cross-repository dependency.
- Build and checks: `.\gradlew.bat :core:coach:test :core:wearables:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease --max-workers=1`. Android tests: `:app:connectedDebugAndroidTest` on a disposable emulator. Do not run Gradle/clean concurrently.
- Backup: FORMABK1 authenticated, bounded typed-record archive. Preview before explicit replace; replacement must be transactional and retain the device key. Never export keys or write plaintext staging files.
- Keep README, architecture, roadmap and validation status honest and aligned with source.

- Muscle mappings and original geometry have one canonical source: `catalog/exercise-muscles.json`. Run `python scripts/generate_exercise_catalog.py --check`; never hand-edit generated Kotlin/Swift/resources. Colors do not represent measured activation or recovery.
- Warm-ups stay out of working-load charts/records and weekly muscle associations. Primary/supporting counts remain separate. RPE is subjective input, not an automatic prescription.
- Apple transfer files are bounded readable workout-only JSON, separate from encrypted backup. Preview/confirm on Android; merge transactionally, deduplicate matches and reject ID conflicts without partial writes. Never silently overwrite edited history.
- WatchConnectivity requires a paired iPhone. Android uses a manual file bridge. Preserve watch outbox until phone disk-success ACK. The iPhone app may read (never write or share) Apple Health workouts and body mass after explicit user permission; imported copies stay labelled, separate from Forma logs and stats, deduplicated by sample UUID, removable, and never overwrite user entries. No sensor recording, Health writes or guaranteed background timers.
- Apple build commands and physical gates: `docs/APPLE_WATCH.md`. Portable checks are not substitutes for Swift/Xcode/device tests.

- Exercise demonstration PNGs are original two-phase AI-generated stills, not videos or validated personal coaching. Canonical files live once in `assets/exercises/res/drawable-nodpi`. Do not mirror their left/right phase order in RTL.
- 3D motion library (12 exercises): keyframes, skeleton and muscle→body-part map live only in `catalog/exercise-motion.json`; run `generate_exercise_motion.py --check` and never hand-edit generated motion. Kotlin and Swift engines must stay identical. The mannequin is illustrative, colours are catalogued muscles (not measured activation), reduced motion holds a still pose, and nothing is mirrored in RTL. Qualified pose review remains a release gate. See `docs/EXERCISE_MOTION.md`.
- AI exercise videos (optional, catalog exercises only): generated outside the app from the existing start/end stills, bundled in `assets/exercises/res/raw` and listed in `catalog/exercise-video-manifest.json` via `finalize_exercise_video.py`. Never bake muscle highlights into video; never claim review until `reviewed` is true; `check_exercise_video.py --release` must pass before release. See `docs/EXERCISE_VIDEO.md`.
- Guide variants/cues/references come from `catalog/exercise-guides.json`; run `generate_exercise_guides.py --check` and `check_exercise_art.py`. Never rename historical exercise IDs to match an illustration. Qualified pose/cue review remains a release gate.
- Keep library thumbnails sampled/cached off the UI thread; reserve full resolution for the selected guide. Preserve draft input when switching training sections and keep explicit save/confirm semantics.
- Program targets/rest/linked groups/notes are user-entered, snapshotted into sessions and retained in backup/repeat. Warm-ups never satisfy working-set targets. Log-and-read counts transactionally before advancing; switching exercises clears stale load input.
- Transfer writers use protocol v2 for plan snapshots; readers retain v1 support. Never silently downgrade/drop targets or groups. Keep Kotlin/Swift fixtures and closed nested-field checks aligned; Apple source checks are not native tests.
- Meal categories default UNSPECIFIED for legacy records. Never infer meal times or retag history. Calendar days count recorded completed workouts only and do not establish adherence.
