# Release gates

This is a development alpha. A built APK or a passing emulator suite is not a public-release decision.

- [ ] Review exercise content and any proposed adaptive rules with qualified reviewers.
- [ ] Verify all food catalog/source licensing and coverage before adding external data. Current library is user-entered only.
- [ ] Complete the 10–15-person usability study; record actual results.
- [ ] Test on physical devices, minimum supported Android, low-memory devices and 16 KB page-size devices where applicable.
- [ ] Test restore on a second physical device, export-provider failures, process termination during restore, reboot and forgotten-password messaging.
- [ ] Validate TalkBack, large text, contrast, phone/tablet and both orientations in Arabic/English.
- [ ] Measure database/history performance, startup, battery use and timer behavior over long histories.
- [ ] Verify local-model download, generation, foreground cancellation, quota/error recovery and thermal/memory behavior on supported hardware.
- [ ] Keep Qwen CPU vision disabled and avoid bundling weights without license/compatibility review.
- [ ] Review dependency notices, SDK/service metadata, privacy policy and store disclosures; no cloud AI or paid/key-required API.
- [ ] Select the final brand, store identity and owner-controlled release signing credentials. Nemory credentials/listing remain independent.
- [x] Verify minified build/startup, encrypted 1→2 migration and a native system-picker backup round trip on the minified QA app.
- [ ] Review the release candidate and decide distribution after the remaining gates.
- [ ] Run a 30–50-person beta and examine actual repeat usage before payment experiments.

No store upload, public repository push, recruitment, billing setup or external messages are performed by the development checks.

## Apple companion and anatomy

- [ ] Run native Swift package tests and compile both Xcode simulator targets on macOS. CI configuration alone is not a result.
- [ ] Validate actual paired iPhone/watch delivery, durable ACK/retry, delayed/stale libraries, duplicates/conflicts, capacity, relaunch and file-provider failures.
- [ ] Review OS-protected Apple storage, backup/recovery, export/deletion, VoiceOver/Arabic layout and foreground timer limitations.
- [ ] Physical-iPhone check of the read-only Apple Health import: permission prompt (EN/AR), allow/deny/partial access, first one-year sync, re-sync, removal, and that nothing is written to Health. Requires a paid Developer team (HealthKit entitlement) and a privacy policy for TestFlight/App Store.
- [ ] Define and review a HealthKit lifecycle/privacy implementation before recording sensors or writing Apple Health. The watch is still a manual logger.
- [ ] Obtain qualified review of all 40 simplified muscle mappings, representative variants and generated poses/cues. No measured activation/recovery claim.
- [ ] Prepare original Apple icons/store assets, choose owner signing credentials and review final platform disclosures before distribution.

- [ ] Review every generated demonstration pose, named equipment variant and translated cue with qualified reviewers; verify beginners understand the two still phases and can obtain individual form feedback. These are not motion capture or personal technique assessments.
