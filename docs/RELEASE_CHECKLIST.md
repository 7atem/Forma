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
