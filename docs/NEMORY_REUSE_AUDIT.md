# Nemory reuse audit

Audited checkpoint: `c4e9f15c85bc49f4f9dc6dee76be4bcf6204718b` on `main`, 3 November 2026. This is a targeted source/dependency audit, not a new full security or performance certification. Original tests/build claims have not been rerun as part of Forma's preservation.

## Actually transferred

| Component | Treatment and validation |
|---|---|
| Gradle launcher and wrapper | Copied unchanged. Same pinned Gradle distribution. Apache/upstream notices retained. |
| LICENSE | Original Apache-2.0 text retained; NOTICE records adaptations. |
| KeystoreManager database-key path | Adapted into `core:data`. New package, key basename, Keystore alias and DB name. Removed Hilt annotations, media-key/restore methods and Nemory-only legacy `.tmp` recovery. Retained CSPRNG, size validation, stable staging basename, verified round trip, AtomicFile recovery/install, missing-existing-key fail-closed guard. Forma's encrypted-storage device tests are separate from Nemory's tests. |

Alpha02 additionally adapts the ML Kit text/status/download pattern from `MlKitNanoRuntime.kt` into `app/LocalNanoModel.kt`. It omits images, streaming, Hilt runtime coupling, cloud, native code and vault dependencies; it adds a 64-token request cap and strict app-level known-fact-ID verification, explicit download and foreground cancellation. SDK beta4 is pinned. Actual supported-device inference remains unverified.

The fitness data model, repository, deterministic reports and Compose screens are newly implemented. They are not represented as direct ports of the vault app. No release signing material, private vault data, model downloads, `.git` directory or local secrets were copied into Forma. Existing credentials are preserved only in the private local Nemory backup.

## Next extraction map

| Nemory area | Decision | Conditions before transfer |
|---|---|---|
| Compose/UI patterns and bilingual resources | Reuse patterns; new fitness UI | Check RTL, accessibility, phone/tablet layouts; no vault taxonomy in UI |
| Room + SQLCipher | Reuse approach, new schema v1 | Own keys/DB; schema export and migration tests; no destructive migration |
| Hilt/WorkManager/reminders | Hilt used; scheduling deferred | Product-specific payloads, private notifications, timezone/permission/idempotency tests |
| Portable encrypted backup | High-value follow-up; not copied yet | New archive namespace; adapt FK/schema/media rules; round-trip, rollback, corruption and wrong-passphrase tests |
| LlmClient, ReasoningBudget, Qwen/native model selection | Deferred optional extraction | Audit runtime/JNI/device capability, providers/model licenses, cancellation and foreground preemption; keep logging independent |
| RAG, LocalAgent and ClaimVerifier | Adapt principles; domain-bound code not copied | Replace VaultItem/ExternalRecord tools with typed fitness tools; verified record IDs and explicit proposal confirmation; no LLM arithmetic |
| Embeddings/ObjectBox | Deferred until justified | Separate fitness index; disclose any unencrypted embeddings; do not claim SQLCipher protects the vector store |
| OCR/barcodes | Deferred capture aids | Separate label parser, units/serving validation and user confirmation; Latin ML Kit cannot establish Arabic OCR support |
| CPU Qwen vision | Excluded from normal production path | Prior Nemory benchmark found unacceptable CPU latency; new device/model validation required |
| VaultItem, document classification, lenses, keyword library | Not transferred | Fitness entities are typed exercises, sets, meals, measurements and future proposals |
| Decoy mode and app lock | Product decision later | No fake lock or privacy claim; current isolation is app-private encrypted storage |
| Calendar/Gmail/Health Connect | Not transferred | Fitness-specific needs, explicit permissions/consent; Gmail foundation and disabled Health Connect are not production integrations |
| Billing, trainer accounts, cloud sync | Deferred | Validate product first; separate credentials/store listing and no paid/key-required API dependency |
| Nemory KMP/iOS | Not transferred | Android-first fitness product; independent portability and macOS verification needed later |

## Source/document discrepancies found

The checkout has evolved beyond the brief AGENTS module summary: `shared` now contains VaultItem, entities/DAOs and intelligence interfaces, not just a small portable keyword layer. It cannot be copied as a generic fitness core. `core/ai/llm/ReasoningBudget.kt` currently sets FAST/NORMAL/DEEP rounds to 1/2/3, while historical prose mentions 1/2/5. These are recorded for a later Nemory documentation audit; its source was intentionally left intact.

## Original file hashes

Hashes identify the exact upstream bytes before adaptation. The source checkpoint preserves history; Forma has independent history and no Nemory remote.

| Original path | SHA-256 |
|---|---|
| `core/security/src/main/java/com/vaultbrain/core/security/KeystoreManager.kt` | `687a868e6a46be0b3038c21c062d6b4737d53b9b2a191803846c10aeac124d81` |
| `gradlew` | `4c09b420894378ea5bbc5f4e32edf7b84f482475f41fe4a327a8c83ee512f133` |
| `gradlew.bat` | `c46a27c79007746de5922b17abb6230d64ad8b1ba3ad1585ee5c6543c2a9b129` |
| `gradle/wrapper/gradle-wrapper.jar` | `498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17` |
| `gradle/wrapper/gradle-wrapper.properties` | `257d14aa05eba1c0eb260ccb99a0b40d97960529e5f5841b63a4b6c87dab55d1` |
| `LICENSE` | `40932c5e8b9127b30823e800a5bdfa3dc0eec2c96effd38e1ec0f120fbefa107` |

## Alpha02 additional upstream source

`core/ai/llm/src/main/java/com/vaultbrain/core/ai/llm/MlKitNanoRuntime.kt` at the same checkpoint: SHA-256 `2eed630b6da9f64d315c05d724ea9ef04f22e51cd0a33f9863b9d05671168a30`. The new local adapter is a narrow adaptation, not a wholesale import of Nemory runtime behavior. SDK/model terms and device suitability need pre-release review; no model weights are redistributed.
