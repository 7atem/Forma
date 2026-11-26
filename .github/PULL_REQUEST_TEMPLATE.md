## Summary
<!-- Briefly describe what this PR changes and why. -->

## Verification
- [ ] `python scripts/check_localization.py`
- [ ] `python scripts/generate_exercise_catalog.py --check`
- [ ] `python scripts/generate_exercise_guides.py --check`
- [ ] `python scripts/generate_section_info.py --check`
- [ ] `python scripts/check_exercise_art.py`
- [ ] `python scripts/generate_exercise_motion.py --check`
- [ ] `python scripts/check_exercise_video.py`
- [ ] `python scripts/check_apple_sources.py`
- [ ] Android tests/build/lint passed (`:core:coach:test :core:wearables:test :core:backup:test :app:assembleDebug :app:lintDebug :app:assembleRelease`)
- [ ] Apple Swift tests / Xcode build verified on macOS or via GitHub Actions `macos-15` CI (do not claim physical device tests unless run on hardware)

## Boundary Checklist
- [ ] No hand-edited generated Kotlin/Swift/XML files
- [ ] Matching English and Arabic (`values` / `values-ar`, `en.lproj` / `ar.lproj`) strings added for any UI changes
- [ ] No paid APIs, user telemetry, medical/prescription claims, or secrets added
- [ ] Room schema changes include explicit migrations and exported schema JSON (if applicable)
