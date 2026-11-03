# Architecture and decisions

## Separation from Nemory

Forma has its own repository at `D:\Projects\Forma`, ID `com.formacoach.app`, database `forma.db`, key file `forma_passphrase.bin` and Keystore master-key alias `forma_master_key`. Nemory remains at `D:\Vault Brain` on `main`. There are no Gradle project links, source symlinks, database imports or release-signing links between them. Both applications can coexist by application ID. Do not migrate existing Nemory users or overwrite its Play listing.

Android first: Kotlin + Compose + Hilt, using lessons from the existing project. Native SwiftUI iPhone/watch source is in `apple/`, independent of Nemory KMP. It is not yet compiled/device-validated; see `APPLE_WATCH.md`.

## Current modules

| Module | Responsibility |
|---|---|
| `app` | Compose onboarding, Today, Train, Eat, Progress, Coach; saved programs, foods/recipes, SAF backup UI; Hilt wiring |
| `core:model` | Typed fitness records, bounded numeric validation, Arabic/Persian digit parsing |
| `core:data` | Room schema v3 with encrypted 1→2→3 migrations, transactional snapshots, SQLCipher/Keystore, repository |
| `core:coach` | JVM-only review/history/timer/coach tools, strict local-model fact-ID verification |

| `core:wearables` | Bounded v1 workout transfer; validated wire records, no networking |
| `apple/` | Native phone/watch source, outbox/ACK and muscle diagrams; pending Mac/device checks |
| `core:backup` | JVM authenticated and bounded portable typed-record archive, independent of device keys |

Feature modules can be split when actual size and independent ownership justify them. Do not recreate all of Nemory's modules before they are needed.

## Storage model

- `profile`: single local profile, selected goal, planned sessions, optional user-entered nutrition targets.
- `sessions`: local calendar date, selected template, optional completion timestamp. Drafts do not count as completed training. Schema 3 adds local/Apple-file source and nullable future-compatible sensor metadata; current Apple source does not collect it.
- `training_sets`: parent session FK with cascade, exercise, repetitions, external load in kg, working/warm-up kind and optional RPE. Zero external load is explicitly supported. Completed sessions reject new sets; explicit corrections of existing sets are allowed.
- `meals`: local calendar date, description, explicit whole-portion kcal/protein/carbs/fat totals. Repeating creates a new ID; it is a user-triggered write.
- `programs`: ordered exercise choices and user-selected rest timer; session snapshots survive edits/deletion.
- `foods`: typed nutrition/reference unit/source plus immutable recipe ingredient snapshots; old meals never recalculate from library edits.
- `check_ins` / `plan_proposals`: explicit availability and confirmation state, with stale-target guards.
- `measurements`: one record per local calendar date, optional weight/waist, at least one present. Saving replaces that day's record as stated in the form.

The first alpha has one local user; it does not pretend to support coach accounts. Add trainer/client ownership and consent through an explicit future migration. Do not add unused multi-tenancy now.

Room schema JSON is committed. No destructive fallback migration. No repository path reads a Nemory database. No sensitive record values in logs, notification previews or a vector index. There is no vector store in this alpha.

## Encryption and failure behavior

Room uses SQLCipher. A random 32-byte passphrase is encrypted with Android Keystore-backed AES-GCM through the adapted Nemory key-storage code. Staging keeps the final basename, verifies the ciphertext round trip, and uses AtomicFile installation. Existing unreadable/missing keys fail closed; they are never regenerated over an existing database. Hardware backing depends on the device; no universal hardware-backed claim is made.

Initialization and observation occur on the IO dispatcher. Cancellation is propagated. Failed writes preserve form input. Loading failures do not reset data. Automatic OS backup and device transfer are excluded because Keystore-bound material is not a portable backup. Portable password-encrypted export/restore is implemented through SAF, with preview and a transactional record replacement that preserves the device key. Uninstalling still removes local data; users must export first. Physical cross-device/provider-failure and process-termination validation remains a release gate.

## Reports and future AI

The current review uses seven local calendar dates through today and the seven preceding dates. It counts completed sessions and food-logging days, calculates mean **logged** kcal per logged day, and computes available measurement averages. Weight differences require three weight-recording days in each window as a product data-coverage gate. This is descriptive, not a medical trend model. No causal explanation, diagnosis, calorie adjustment or load prescription is generated.

Review results carry session/meal IDs and measurement dates. UI shows date windows, coverage and underlying recent records. Future chat must retrieve typed structured summaries for arithmetic and source-bound notes for narrative; RAG over raw text is not the arithmetic engine. LLM output may explain a reviewed proposal but cannot manufacture facts or commit writes. An optional ML Kit Gemini Nano adapter selects only known report fact IDs; no model text becomes a claim or write. Status/download are user initiated and inference is foreground-bound and capped. There is no cloud AI and application INTERNET permission contributed by the SDK is removed. AICore/provider processes manage their own downloads. Supported-device inference is not yet physically verified.

## Build baseline

Pinned Gradle 9.5.0 wrapper copied from Nemory; AGP 9.3.2; Kotlin 2.2.10; JDK 21; SDK 36; min SDK 29; Room 2.8.4; SQLCipher 4.17.0. Compose dependencies are explicitly pinned. AGP uses the temporary legacy DSL / external Kotlin compatibility flags required by the selected Kotlin/Hilt toolchain. Deprecation warnings are recorded, not hidden. Modernization is a follow-up before an AGP 10 upgrade.

See [alpha02 details](DAILY_WORKFLOW_IMPLEMENTATION.md) for the archive format, migration and model boundaries.

## Muscle catalog and watch transfer

Original front/back paths, bilingual labels and role mappings generate Android/Swift outputs from `catalog/exercise-muscles.json`. Both roles have text labels; no EMG percentage or recovery estimate is inferred. Working-load history excludes warm-ups. Weekly counts use completed sessions in seven local dates, separating primary/supporting associations.

Android's watch dialog uses SAF and a readable workout-only contract capped at 1 MiB. Imports require preview/confirmation and a single Room transaction; identical records deduplicate, changed records or set ID collisions roll back the batch. Profile/food/program records are not imported. Sensor energy never modifies food targets. Apple storage uses atomic OS-protected files, not SQLCipher. Watch results stay queued until a phone disk-success ACK. Android↔phone transport is an explicit manual file bridge. Details and unverified native gates are in `APPLE_WATCH.md`.
