# Daily workflow implementation — alpha02

Historical alpha02 scope; schema is now v3. See `ARCHITECTURE.md`, `EXERCISE_MUSCLES.md` and `APPLE_WATCH.md` for alpha03 additions.

Date: 3 November 2026. Base: alpha01 commit `645b030`; branch `codex/forma-daily-workflow`. Nemory remains separate and unchanged. The alpha01 source/history checkpoint is `checkpoint/forma-alpha01`, with a verified local bundle in `D:\Archives\Forma\before-daily-workflow-2026-11-03`.

## Implemented behavior

- Saved programs have ordered exercise selections and a user-selected 15–600 second rest timer. Starting a session snapshots the program name, exercise order and timer. Renaming/deleting a program never changes recorded sessions.
- A single active session is enforced transactionally. The timer uses monotonic elapsed time, survives screen recreation, and does not schedule background alarms. A saved deadline outside the chosen duration is treated as expired after reboot.
- Users can select a past local calendar date, correct a recorded set, move a session, delete a session, correct/delete a meal, and edit measurements. The last set of a completed session cannot be removed independently; deleting the session is explicit and confirmed.
- Session/meal search, paged display of history, recorded load charts and 90-day body-weight charts are available. Chart spacing uses actual dates and does not invent observations. External-load comparisons are descriptive, not strength estimates or prescriptions.
- Foods have a reference amount, grams/servings unit, known entered nutrient totals and required source description. The user can scale a portion. Recipes retain flat, immutable ingredient snapshots and a user-entered serving yield. No unverified nutrition catalog is seeded, and no food API is connected.
- Logged meals retain their nutrient totals and source snapshot when a library food changes or is deleted. Manual correction detaches the old quantity/food link instead of keeping misleading provenance.
- Weekly check-ins capture availability, user-reported food-log completeness and optional notes. A changed availability creates one pending schedule proposal. Confirmation checks the current target, applies only the weekly session count, and leaves nutrition and exercise prescriptions alone. Profile changes supersede conflicting pending proposals.
- Coach questions route to local training/food/weight/week tools with dated evidence. The feature does not explain weight-change causes, diagnose conditions or infer total intake from incomplete logs.

## Persistence

Room schema is now v2, with an explicit encrypted 1→2 migration. New tables are `programs`, `foods`, `check_ins` and `plan_proposals`. Sessions gain the immutable program snapshot and timer; meals gain optional portion/source fields. V1 records retain IDs and numeric values, and starter exercise order is recovered from its known template ID.

Repository observation reads a transactionally consistent snapshot after Room invalidations. The production clock follows device timezone changes dynamically; injected test clocks remain deterministic. Midnight clears model highlights. Backups and UI reviews use the same typed domain records. There is no destructive migration fallback.

## Portable backup format

The application exports a Forma-only `.formabackup` record archive through Android's system document picker. It does not copy a live SQLCipher database, device Keystore credentials, signing keys, Nemory archives or captured media. There is no media feature in this version.

The archive is `FORMABK1` (8 bytes), a fixed big-endian KDF iteration count (4 bytes), random salt (16 bytes), random GCM nonce (12 bytes), then AES-256-GCM ciphertext/tag. The entire 40-byte header is authenticated as additional data. A versioned JSON payload contains an export timestamp and typed records. PBKDF2-HMAC-SHA256 uses 600,000 iterations. The supported format accepts exactly that cost, preventing an attacker-selected excessive KDF workload. Inputs are bounded to 16 MiB of plaintext and 100,000 records, with bounded JSON nesting and domain/reference validation.

The parameter choice is informed by [OWASP's PBKDF2 guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html); authenticated encryption follows the [cryptographic storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html). This is not a claim of FIPS certification.

Export is encrypted in memory before opening a destination. A provider failure can leave an incomplete external file, which cannot authenticate as a valid backup; the UI says to export again to a new file. Passwords are not persisted. Owned byte/character buffers are cleared where practical; JVM/Compose immutable strings cannot promise forensic memory erasure.

Import authenticates and validates before showing a preview. Confirming replaces all Forma records in a single Room transaction. A failed constraint rolls back the entire replacement. The existing device key remains untouched. Canceling preview performs no write. Portable restore is independent of the originating Android Keystore. See [Android's Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) for the system-picker boundary.

## Optional local model

The text/status/download subset of Nemory's ML Kit runtime is adapted for Gemini Nano on compatible AICore devices. It does not include Qwen/JNI, vision, model weights, a paid API, a cloud LLM or vault-specific code. Availability is checked on demand; downloads require an explicit user action. Hardware support and system initialization remain device dependent.

The model receives a bounded, code-computed weekly statistics list, without profile names, raw notes or meal descriptions. It may return only 1–3 distinct known fact IDs. Unknown IDs, free prose, invented values, malformed output and background results are rejected. Displayed numbers and explanations are rendered from the typed report, never model text. Output is capped at 64 tokens; selection has a 15-second total timeout and foreground cancellation. Changed data invalidates highlights.

Application-UID INTERNET permission contributed by the SDK is explicitly removed. AICore and document providers run under their own system/provider boundaries. Model-download success and actual inference still need verification on supported physical hardware. SDK/service metadata behavior and Google terms must be included in the pre-release privacy/dependency review. [Official Prompt API guidance](https://developers.google.com/ml-kit/genai/prompt/android/get-started) documents the beta API and device availability checks.

## Still requires external validation

Qualified review of exercise descriptions and adaptive prescriptions, a verified food-data pilot, real-device testing, a 10–15-person usability study, beta retention/payment experiments, and release/store decisions are not software checks that this implementation can manufacture. The app deliberately does not claim those outcomes. See the roadmap and validation record.
