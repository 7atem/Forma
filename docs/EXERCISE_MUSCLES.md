# Exercise anatomy and schematic provenance

The canonical catalog is `catalog/exercise-muscles.json`. `scripts/generate_exercise_catalog.py` generates Kotlin exercise IDs/domain mappings, equipment metadata, Android labels/resources and Swift mappings from that file. `--check` detects drift. All 50 exercise enum values are covered; tests require primary groups, disjoint supporting groups and visible regions. The table below describes the original eight generic IDs; the 32 named additions and their roles are in the canonical catalog and [alpha05 contract](ALPHA05_EXPANSION.md).

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

Primary regions use the warm primary-muscle colour; supporting regions use the blue supporting colour with a dashed white edge. Text labels repeat both roles, so color is not the only distinction. Front/back panels and textual accessibility descriptions accompany the Android diagram. Arabic/English labels are generated from the same catalog. Apple diagrams include the same text and geometry; VoiceOver still needs physical validation.

Weekly counts include completed **working** sets over seven local dates. A compound set can be associated with multiple muscles. Primary/supporting counts stay separate: they are not additive effective-set estimates, recovery scores, percentages of activation, or evidence that a muscle has recovered. Warm-up sets, drafts, future dates and records outside that window are excluded.

## Detailed regions (catalog layer 2)

Each broad group is split into one or more **detailed regions** (`details` in the catalog, `MuscleDetail` in Kotlin/Swift), 20 in total:

| Broad group | Detailed regions |
|---|---|
| Chest | Upper chest · Mid and lower chest |
| Front and side shoulders | Front shoulder · Side shoulder |
| Rear shoulders | Rear shoulder |
| Upper and mid back | Upper trapezius · Mid trapezius and rhomboids |
| Core | Front abdominals · Obliques |
| Glutes | Gluteus maximus · Gluteus medius |
| Calves | Gastrocnemius (upper calf) · Soleus (lower calf) |
| Lats, biceps, triceps, spinal erectors, quadriceps, hamstrings, adductors | One region each |

Every exercise lists `primaryDetail` and `secondaryDetail`. The generator enforces an exact **roll-up**: the groups of the primary details equal the exercise's primary groups, and the groups of the supporting details (minus primary groups) equal its supporting groups. A detail may support within a primary group (for example, flat bench press: primary *mid and lower chest*, supporting *upper chest*; incline press: the reverse). Because the broad groups are unchanged, weekly counts, the library muscle filter, discomfort filters, the 3D motion body-part map and watch transfers (which pin catalog version 1) behave exactly as before.

These detailed assignments are general strength-training anatomy chosen for orientation (`detailReview` in the catalog): they are not EMG measurements, ignore individual technique and grip, and remain subject to the same qualified-review release gate as poses and cues.

### Drawing

Regions are stored once per side with a `fiber` angle (degrees, screen coordinates) and redrawn as smooth original cubic curves. Android (`MuscleRendering.kt`) and iPhone (`MuscleBodyCanvas` in `MuscleDiagram.swift`) render the same layers: a soft drop shadow, a lit body, each muscle with a highlight toward the upper left and darker edges, fibre striations along `fiber` (omitted in the small demo overlay), dark region contours and a dashed edge for supporting regions. All shading is illustrative and never encodes activation, effort or recovery. Muscle colours follow the app's own light/dark setting.
