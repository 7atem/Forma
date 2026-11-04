# Exercise anatomy and schematic provenance

The canonical catalog is `catalog/exercise-muscles.json`. `scripts/generate_exercise_catalog.py` generates Kotlin exercise IDs/domain mappings, equipment metadata, Android labels/resources and Swift mappings from that file. `--check` detects drift. All 40 exercise enum values are covered; tests require primary groups, disjoint supporting groups and visible regions. The table below describes the original eight generic IDs; the 32 named additions and their roles are in the canonical catalog and [alpha05 contract](ALPHA05_EXPANSION.md).

The body outline and muscle-region path coordinates were drawn originally for Forma in a 100 × 220 coordinate space, licensed under this repository's Apache-2.0 license. No competitor graphic, logo, photograph or anatomical illustration was copied. The same original M/L/C/Z paths are rendered natively in Compose and SwiftUI. The drawing is schematic, not anatomically precise artwork.

| Current exercise | Primary groups | Supporting groups | Reference |
|---|---|---|---|
| Squat | Quadriceps, glutes | Adductors, core | [NASM guidance](https://www.nasm.org/workout-exercise-guidance) |
| Leg press | Quadriceps | Glutes, adductors | [NASM leg press](https://www.nasm.org/resource-center/exercise-library/leg-press) |
| Chest press | Chest | Front/side shoulders, triceps | [ACE seated chest press](https://www.acefitness.org/resources/everyone/exercise-library/188/seated-chest-press/) |
| Row | Lats, upper back | Biceps, rear shoulders | [ACE seated row](https://www.acefitness.org/resources/everyone/exercise-library/48/seated-row/) |
| Lat pulldown | Lats | Biceps, upper back | [ACE back training](https://www.acefitness.org/resources/pros/expert-articles/8944/a-back-to-school-program-an-evidence-based-approach-to-training-the-back/) |
| Hip hinge, representative Romanian-deadlift style | Glutes, hamstrings | Spinal erectors, core | [NASM guidance](https://www.nasm.org/workout-exercise-guidance) |
| Shoulder press | Front/side shoulders | Triceps, upper back | [NASM guidance](https://www.nasm.org/workout-exercise-guidance) |
| Split squat | Quadriceps, glutes | Adductors, core | [NASM split squat](https://www.nasm.org/resource-center/exercise-library/bulgarian-split-squat) |

These are editorial simplifications of major groups based on general exercise references, not measured EMG results or an exhaustive list of every stabilizer. Some references describe specific variants; the app's broad exercise names do not specify equipment, stance, grip, range or technique. For example, a hip hinge can describe several movements; this mapping represents an RDL-style movement. Qualified review and equipment-specific exercise variants remain release work. Primary/supporting classification does not prescribe exercise choice, effort or volume.

Primary regions use a solid green fill; supporting regions use orange with an outline. Text labels repeat both roles, so color is not the only distinction. Front/back panels and textual accessibility descriptions accompany the Android diagram. Arabic/English labels are generated from the same catalog. Apple diagrams include the same text and geometry; VoiceOver still needs physical validation.

Weekly counts include completed **working** sets over seven local dates. A compound set can be associated with multiple muscles. Primary/supporting counts stay separate: they are not additive effective-set estimates, recovery scores, percentages of activation, or evidence that a muscle has recovered. Warm-up sets, drafts, future dates and records outside that window are excluded.
