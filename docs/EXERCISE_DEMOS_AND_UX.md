# Exercise demonstrations and experience

50 original, generated 3D **still illustrations** show two distinct phases per current exercise. They are not interactive 3D models, motion capture, videos or a validated assessment of an individual user's form. They ship offline with the app. No exercise imagery or videos were copied from another product.

## Assets and editorial references

Canonical assets: `assets/exercises/res/drawable-nodpi/exercise_*.webp`. Each is a 960×640 WebP at quality 92 made from the PNG original by `scripts/convert_exercise_art.py` (about 2.5 MB for all 69, down from 30 MB of PNGs; worst sample PSNR about 44 dB) with two side-by-side panels. Start is physically left and movement physically right; the image and its phase labels are intentionally not mirrored in Arabic. The generated subjects are adult mannequins in neutral gym clothing. Equipment is representative and simplified.

| Original stable exercise ID | Illustrated variation | Technique reference |
|---|---|---|
| SQUAT | Bodyweight squat | [ACE](https://www.acefitness.org/resources/everyone/exercise-library/135/bodyweight-squat/) |
| LEG_PRESS | Inclined sled leg press | [NASM](https://www.nasm.org/resource-center/exercise-library/leg-press) |
| CHEST_PRESS | Seated machine chest press | [ACE](https://www.acefitness.org/resources/everyone/exercise-library/188/seated-chest-press/) |
| ROW | Seated low cable row | [ACE](https://www.acefitness.org/resources/everyone/exercise-library/48/seated-row/) |
| LAT_PULLDOWN | Front lat pulldown | [NASM](https://www.nasm.org/resource-center/blog/training/the-biomechanics-of-the-lat-pulldown-muscles-grip-and-form) |
| HIP_HINGE | Dumbbell Romanian deadlift | [NASM](https://www.nasm.org/resource-center/exercise-library/dumbbell-romanian-deadlift) |
| SHOULDER_PRESS | Seated machine shoulder press | [ACE](https://www.acefitness.org/resources/everyone/exercise-library/186/seated-shoulder-press/) |
| SPLIT_SQUAT | Bodyweight Bulgarian split squat | [NASM](https://www.nasm.org/resource-center/exercise-library/bulgarian-split-squat) |

Broad historical exercise IDs and saved sets are unchanged. Each guide explicitly names its illustrated variation; it does not establish what equipment was used in an older generic log. The Bulgarian example uses a rear-foot bench and is not presented as a suitable beginner prescription for everybody. No target loads, repetitions or progression are generated from the illustrations.

`catalog/exercise-guides.json` holds original concise EN/AR editorial cues and their primary references. `generate_exercise_guides.py` creates Android resources and Swift values. These are short paraphrases, not copied exercise articles. The guide's source button opens the public page in the system browser only when the user chooses it.

The built-in `image_gen` tool produced the assets. [Prompt set](../catalog/exercise-art-prompts.json) records the generation specifications; [manifest](../catalog/exercise-art-manifest.json) records exact byte counts and SHA-256. Files are included once and referenced by Android and XcodeGen. Source originals remain in the Codex generated-images directory as well. The 40 assets total 69,717,035 bytes. Alpha04's original eight remain byte-identical; alpha05 adds 32.

## Review status and limits

Visual sanity review checked distinct phases, full-body framing, basic limb/contact/equipment plausibility and agreement with the named variation. That is not qualified exercise or biomechanics validation. No expert review, clinical validation or beginner study is claimed. Generated equipment and body geometry can be simplified. The in-app guide identifies generated examples, asks users to match their equipment and comfortable range, and recommends a qualified form check. Qualified review of **every pose and cue** remains a public-release gate.

The precise primary/supporting muscle diagram remains the separate original schematic generated from `exercise-muscles.json`; the 3D images do not claim measured activation or silently change the anatomy mapping.

## UX implementation

- Sticky brand/date/profile/watch/backup header. Bottom navigation on phones; navigation rail at widths of 840 dp or more.
- A clearer Today hierarchy: seven-day session progress, resume/start action and separate logged-calorie/protein tiles. Targets remain user inputs; missing logs remain unknown.
- Training sections for programs, searchable visual exercise library and history. Active drafts remain available while visiting the library/history; section state is retained and section changes return to the top.
- Exercise details separate Movement and Muscles. Movement shows the named variation, both phases and three concise cues; Android's full-screen viewer can isolate either original half without modifying the source asset.
- Compact active-session preview opens the guide. Reps/load sit together when space permits, and stack with large fonts. RPE is an optional disclosure. Explicit save keeps the user's entered reps/load for another set without prescribing an increase.
- Recorded sets and expandable session details keep editing/removal available while reducing repeated controls. Food and measurement fields share the responsive paired-field pattern; daily food totals are explicitly labelled as logged.
- Thumbnail previews decode at half the original dimensions on an IO dispatcher and use an eight-entry cache (roughly 12 MiB of pixel data). Full-resolution assets are reserved for the selected guide/viewer. No new image/network SDK or app INTERNET permission.

SwiftUI source consumes the same image files, cues and variant names for iPhone/watch exercise details. Native Apple compilation and physical testing remain pending macOS/Xcode; see [Apple status](APPLE_WATCH.md). Do not infer native runtime validation from shared asset checks.

## Checks

```powershell
python scripts/check_localization.py
python scripts/generate_exercise_catalog.py --check
python scripts/generate_exercise_guides.py --check
python scripts/check_exercise_art.py
python scripts/check_apple_sources.py
```

These verify keys, catalog coverage, exact WebP integrity/dimensions, generated outputs and packaging references. They do not validate the movement scientifically. Current executed UI/build results and visual evidence are in [validation](VALIDATION.md).

Alpha05 extends this foundation with 32 named variations, combined equipment/muscle/favorites/previously logged filters, personal exercise notes and a compact three-column tablet library (two columns on phones). See [expansion contract](ALPHA05_EXPANSION.md).

## Stills that need regenerating

Found in the 26 November 2026 visual check of the five stills added in `d85bce5`. Replace each only through `python scripts/convert_exercise_art.py <id> <original.png>`, then run `check_exercise_art.py`.

- `barbell_shrug`: the two phases look almost identical, so the shoulder elevation is not visible. The end phase should show the shoulders clearly raised toward the ears with straight arms.
- `good_morning`: the end phase is a shallow hinge with deep knee bend that reads like a partial squat. It should show a clear hip hinge with soft knees and a near-horizontal, neutral back.

The other three (`cable_crossover`, `close_grip_bench_press`, `hip_abduction`) show distinct, plausible phases. None of this is qualified pose review.
