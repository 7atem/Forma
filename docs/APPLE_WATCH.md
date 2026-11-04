# Apple Watch companion — alpha06 source

Native SwiftUI iPhone and watchOS application source now lives in `apple/`. It is **not yet a compiled, installed or device-validated Apple release**. This Windows machine has neither Swift nor Xcode; portable catalog/fixture/localization checks do not establish that the native applications build. A macOS workflow is prepared but has not run remotely.

## User workflow

Apple Watch uses a paired iPhone; it does not pair with Forma Android. See [Apple's setup guidance](https://support.apple.com/en-ca/guide/watch/-apdde4d6f98e/watchos).

1. Android: tap the watch icon, select programs, export a `.formaworkouts` file.
2. Move that file using a user-chosen private method to iPhone. Open it from Forma's importer, review the program count and confirm replacing the program library. A running watch workout keeps its program snapshot.
3. iPhone: explicitly send the library to the paired watch through WatchConnectivity. No Android server, cloud account or application INTERNET permission is needed.
4. Watch: start a saved or starter program. Log/edit/delete sets, select warm-up/RPE, view the selected exercise's front/back muscles and use a rest countdown. Logging and draft persistence work independently of phone availability in the source implementation.
5. Finish explicitly. Results remain saved on the watch and queued until iPhone saves the exact workout and sends a durable-receipt ACK. Retry is available. WatchConnectivity background delivery has no immediate-delivery guarantee.
6. iPhone: export an individual completed workout. Move its file to Android, preview it from the watch dialog and confirm import. This is a manual file bridge, not automatic cross-platform/live session sync.

## Implemented boundaries

- iOS 17 / watchOS 10 minimum targets; local Foundation Swift package; XcodeGen target configuration, EN/AR strings and shared original schematic geometry.
- All 40 catalog exercises with shared equipment metadata and 3D guides, kg external load, reps, optional subjective RPE 0–10 and warm-up/working types. A watch workout has at most 300 sets. The kg stepper increments by 0.5 kg, preserving imported/copied fractional values. iPhone library source adds name search and equipment filtering.
- Atomic native state file, OS file protection until first unlock, fail-closed unreadable state and explicit deletion. This is **not** Android SQLCipher encryption and is not a password-encrypted Apple backup. Device/OS backup policy and Apple recovery/export UX still need review.
- Persist-before-ACK; matching workout IDs deduplicate, conflicting IDs reject. Up to 500 completed workouts are retained; capacity errors preserve the draft/outbox instead of silently deleting history. Phone exports one workout at a time.
- Rest countdown uses a persisted deadline. Foreground haptic on expiry only; background notifications, workout-process runtime, reboot/clock-change timer guarantees and active HealthKit session recovery are not implemented.
- No HealthKit entitlement, health permission request, sensor recording or Apple Health write. Optional heart-rate/energy fields in the transfer contract are accepted/displayed on Android for future compatible exporters. The current watch app leaves them unknown. There is no calorie-target credit from exercise energy.
- No automatic diet/training prescription, live phone/watch co-editing, custom exercise authoring or App Store submission.

## Transfer and privacy contract

`core:wearables` and `apple/Sources/FormaCore` now write `forma.workouts` v2 and read v1/v2, catalog v1, canonical `KG`, Gregorian `yyyy-MM-dd` and UTC completion milliseconds. Stable exercise enum IDs come from `catalog/exercise-muscles.json`. Fixtures: `contracts/apple-watch/transfer-v1.json` and `transfer-v2.json`. V2 carries optional user-entered working sets, rep ranges, exercise rest, adjacent linked groups and program notes. V1 cannot carry these details; attempts to downgrade a nonempty plan are rejected. Updated readers retain old files, while old companion builds reject v2 instead of dropping plan data. Update both sides before transferring new programs.

Some Android providers append `.json` to the proposed `.formaworkouts` filename; both importers accept JSON and validate its contents.

Files contain program names, program exercise notes/targets and workout results in readable JSON. The UI discloses this and asks for a destination through the platform file picker. They exclude profile, food, body measurements, Nemory content, passwords and device keys. Android personal exercise-library notes remain separate and are not exported. These are not encrypted backups and their origin is not cryptographically authenticated. Use a private transfer destination; actual private transfer is the user's choice.

Limits: 1 MiB, 20 programs, 100 workouts, 300 sets/workout, 3,000 total sets/packet, unique IDs, supported enums, finite bounded values and version/unit validation. Android parsing also bounds nesting. Unknown JSON fields are rejected by both readers. Android dates cannot be future local days and completion timestamps cannot exceed the injected clock by five minutes. Conflicting imports roll back the entire Room transaction; identical re-import is a no-op. Correcting/deleting the Android copy does not mutate the watch/phone original; a later conflicting re-import is rejected for review.

Program transfer uses `WCSession.transferFile`; completed workouts use bounded `transferUserInfo` packets and a separate ACK after phone disk success. No assumption of reachability or successful delivery is made from a send call. Consult [Apple WatchConnectivity](https://developer.apple.com/documentation/WatchConnectivity/transferring-data-with-watch-connectivity). Apple explicitly requires paired physical devices to test [`transferUserInfo`](https://developer.apple.com/documentation/watchconnectivity/wcsession/transferuserinfo(_:)); simulator builds alone cannot verify this path.

## macOS validation and installation

```sh
python3 scripts/generate_exercise_catalog.py --check
python3 scripts/check_apple_sources.py
swift test --package-path apple
cd apple
brew install xcodegen
xcodegen generate
xcodebuild -project FormaApple.xcodeproj -scheme FormaPhone -destination 'generic/platform=iOS Simulator' -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO build
xcodebuild -project FormaApple.xcodeproj -scheme FormaWatch -destination 'generic/platform=watchOS Simulator' -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO build
```

Open the generated project on a Mac, select an owner-controlled development team and paired devices. No team ID, signing key, certificate or provisioning profile is included. App icons, distribution metadata and signing/store review are pending. No paid service was introduced; App Store distribution itself is an owner release decision.

Physical acceptance gates: first pairing/install, EN/AR layout and VoiceOver, all muscle maps, offline log/relaunch, explicit finish/discard, edits and fractional loads, denied/unavailable transfer, delayed/repeated/reordered delivery, phone storage failures, missing/corrupt files, durable ACK/outbox retry, background/foreground timers, capacity handling, program update during a draft, conflict rollback and a full watch→iPhone→Android round trip. Add sensor recording only through a separately reviewed HealthKit lifecycle/privacy implementation.


Alpha04 adds shared original 3D still assets, generated named variations/cues and an exercise detail view to the native source. Android and XcodeGen reference the same PNG directory; no duplicated image files or cloud image fetch. These Apple UI additions remain uncompiled/device-unverified on this Windows host. Full-screen individual-phase viewing is currently Android only. See `EXERCISE_DEMOS_AND_UX.md` for generation provenance and qualified-review boundaries.


Alpha06 adds optional manual working-set/rep targets, per-exercise rest and notes,
linked groups and actual-count navigation to the shared native source. A program
snapshot stays attached to its completed workout. Warm-ups do not consume working-set
targets; automatic exercise changes clear entered reps/load. Both readers accept v1
and v2; current exports use v2, and v1 encoding refuses nonempty plan details. Install
the matching companion before transferring new packets. Six Swift tests are written,
but none were executed on this Windows host. Static source checks are not a native
build or WatchConnectivity/device validation.
