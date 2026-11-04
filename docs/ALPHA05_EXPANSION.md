# Alpha05 implementation contract

Independent Forma checkout; original Nemory is preserved. Alpha04 remains tagged
and archived. This milestone expands the existing product without importing Nemory
data, keys or signing identity.

## Catalog and illustrations

Canonical muscles/equipment/roles: `catalog/exercise-muscles.json`. Variants, cues
and links: `catalog/exercise-guides.json`. Generated Kotlin and Swift retain the
original first eight string IDs; 32 additive IDs cover lower body, chest/back,
shoulders/arms and core. Existing records never change identity or units.

Equipment is a list of required categories (a dumbbell bench press lists both
dumbbells and bench). The filter means "uses this equipment", not "can be done
with this item alone". Primary-muscle filtering does not count supporting roles.
The previously logged filter uses completed local or imported workouts, across all
dates, and is not a suggested recency/recovery score.

Core-only and isolation movements may have no supporting role in this simplified
map; the UI omits an empty supporting legend. Regions are illustrative and exclude
many anatomical subdivisions. No measured activation percentages are inferred.

One original PNG per exercise, two phases, shared by Android and Apple. New assets
are generated using built-in image_gen, copied unchanged into
`assets/exercises/res/drawable-nodpi`; prompt text, SHA-256 and byte counts accompany
them in the art manifests. Start/movement order stays physically left/right in RTL.
Generated poses/cues require qualified review before public release. Most new links
point to ACE's general library for further reading; they are not endorsements or
claims that the generated pose was independently validated. Specific references
include NASM's goblet squat and Queensland Health's dead bug guidance.

Repetition-based records only. Unilateral cues specify reps per side. New dumbbell
variant guidance uses one dumbbell's kg; barbell is bar plus plates, machines use
displayed load and bodyweight uses added external load only. Historical generic
records are not converted. Compare like equipment and recording conventions.

## Storage and compatibility

Room v4 adds `exercise_preferences(exercise PRIMARY KEY, favorite, notes)` through
explicit 3→4 migration after existing 1→2→3. Notes capped at 2,000 characters; reads
and writes remain SQLCipher encrypted. Backup defaults missing preferences to an
empty list and rejects duplicate exercise preferences. Favorites/notes are private
Android data, not included in readable Watch workout packets or training CSV.

Workout transfer stays protocol/catalog v1 with additive exercise names. New apps
read old packets. Old builds cannot decode unknown new exercise names and fail
closed; update both sides before transferring expanded routines. No silent ID
remapping or ordinal serialization. Apple static consistency is not compilation.

Repeat-history and source-day copy preserve old records. One draft guard and copied
meal source comparison execute transactionally. Repeating history uses its stored
order plus actually logged exercises (distinct, maximum 20), rest and label; sets
start empty. Repeat is disabled while a draft exists or the combined order exceeds
20; no exercises are silently dropped.

## Verification

Evidence and executable results are recorded in `docs/VALIDATION.md` after the
candidate passes core tests, encrypted migrations/restore/UI on the disposable
Android emulator, debug build, lint and minified release. No Swift/Xcode execution
is available on this Windows host. Do not count source checks as Apple tests.
