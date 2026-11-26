# Third-Party Licenses, Data Sources & Media Provenance

Forma is licensed under the [Apache License, Version 2.0](LICENSE). Required upstream attribution notices are maintained in [NOTICE](NOTICE). This document summarizes the open-source libraries, public datasets, and media provenance used across the Android and Apple targets.

---

## 1. Adapted Upstream Code

| Source | License | Usage & Provenance |
| :--- | :--- | :--- |
| **Nemory** (`c4e9f15c85bc49f4f9dc6dee76be4bcf6204718b`) | Apache-2.0 | Adapted `KeystoreManager.kt` and `MlKitNanoRuntime.kt` status/download pattern. Full hashes and adaptation notes in [NOTICE](NOTICE) and [docs/NEMORY_REUSE_AUDIT.md](docs/NEMORY_REUSE_AUDIT.md). |
| **Gradle Wrapper** | Apache-2.0 | Build wrapper scripts copied unchanged (`gradlew`, `gradlew.bat`, `gradle/wrapper/`). |

---

## 2. Android & JVM Dependencies

| Library / Artifact | License | Purpose |
| :--- | :--- | :--- |
| **Kotlin Standard Library, Coroutines, Serialization** (`org.jetbrains.kotlin*`, `org.jetbrains.kotlinx*`) | Apache-2.0 | Language runtime, structured concurrency, JSON serialization |
| **AndroidX & Jetpack** (`core-ktx`, `activity-compose`, `lifecycle-*`, `compose-*`, `room-*`, `sqlite`, `security-crypto`, `health.connect:connect-client`, `test-*`) | Apache-2.0 | UI (Jetpack Compose Material 3), encrypted Room persistence, read-only Health Connect client, Android tests |
| **Dagger / Hilt** (`com.google.dagger:hilt-*`) | Apache-2.0 | Compile-time dependency injection |
| **KSP** (`com.google.devtools.ksp`) | Apache-2.0 | Kotlin symbol processing for Room & Hilt |
| **SQLCipher for Android** (`net.zetetic:sqlcipher-android`) | BSD-style (Zetetic Community Edition) | Full-database AES-256 encryption for `forma.db` |
| **Google ML Kit GenAI Prompt** (`com.google.mlkit:genai-prompt`) | Google APIs / ML Kit Terms of Service | Optional on-device Gemini Nano (AICore) fact-ID highlighting; no model weights bundled |
| **Google Play Services Code Scanner** (`com.google.android.gms:play-services-code-scanner`) | Google APIs Terms of Service | Optional barcode scanning for Open Food Facts food lookup |
| **JUnit 4** (`junit:junit`) | Eclipse Public License 1.0 (EPL-1.0) | JVM unit testing |

---

## 3. Apple Dependencies & Tooling

- **Apple system frameworks** (SwiftUI, Foundation, HealthKit, WatchConnectivity, AVKit, VisionKit, CryptoKit) are used directly with zero third-party Swift Package dependencies in `apple/Package.swift`.
- **XcodeGen** (MIT License) is used only at build/CI time to generate `apple/FormaApple.xcodeproj` from [apple/project.yml](apple/project.yml).

---

## 4. External Food & Nutrition Data Sources

| Dataset / Service | License | How Forma Uses It |
| :--- | :--- | :--- |
| **USDA FoodData Central (FDC)** | CC0 1.0 Universal (Public Domain) | Reference nutrition values for the optional offline starter whole-food catalog (explicit user import into editable local library; logged portions keep immutable snapshots). |
| **FAO Near East Food Composition Tables** | Public reference data (FAO) | Reference values for traditional Arabic / Middle Eastern staples included in the optional offline starter catalog. |
| **Open Food Facts** (`world.openfoodfacts.org`) | Open Database License (ODbL 1.0) | Optional, free, keyless name and barcode nutrition lookup when the user explicitly searches or scans a barcode. Data © Open Food Facts contributors. |

---

## 5. Exercise Media Provenance

- **Two-phase exercise illustrations (`assets/exercises/res/drawable-nodpi/*.webp`)**: Original AI-generated two-phase studio illustrations created specifically for Forma and tracked with SHA-256 hashes and prompt provenance in [catalog/exercise-art-manifest.json](catalog/exercise-art-manifest.json) and [catalog/exercise-art-prompts.json](catalog/exercise-art-prompts.json).
- **Optional exercise videos (`assets/exercises/res/raw/exercise_video_*.mp4`)**: Original AI-generated clips derived from Forma's own start/end stills and tracked in [catalog/exercise-video-manifest.json](catalog/exercise-video-manifest.json). None are marked `reviewed: true` yet; qualified technique review remains a release gate.
- **No third-party copyrighted stock photography, commercial anatomy atlases, or scraped exercise videos are bundled.**
