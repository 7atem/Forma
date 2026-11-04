# Alpha06 workflow implementation

4 November 2026. This checkpoint applies selected patterns from the [18-product
review](PRODUCT_WORKFLOW_REVIEW_ALPHA06.md). It is an Android development candidate,
with matching Apple source changes awaiting a Mac build and paired-device tests.

## Plan → execute → review

A saved program can carry optional working-set counts (1–20), a complete rep range
(1–1000), per-exercise rest (15–600 seconds) and personal program notes. Blank values
remain blank; the application does not choose new loads or prescribe a routine.
Adjacent exercises can be linked into supersets or circuits. Reordering/removing
members normalizes or splits groups and retains their individual targets/notes.

Starting a session snapshots its order and details. Later program edits do not alter
an existing session or history. Logging commits a set and reads its actual session
counts in one database transaction before advancing the UI. Warm-ups remain on the
current exercise and do not satisfy working-set counts. Linked exercises move forward
without starting rest, then use the configured timer when the round wraps; completed
members are skipped. A blank working-set target allows continued manual logging.
Exercise changes clear reps/load/RPE and warm-up selection. Ending the order is not a
claim that every earlier target was met; users review and explicitly finish.

History offers a locale-aware month calendar over recorded completed sessions,
including date filtering and a clear action. Missing logs imply no adherence score.
Repeat-as-new uses historical details and no copied completed sets.

## Food → portion → meal diary

Select breakfast, lunch, dinner, snacks or unassigned, then log a reviewed portion or
manual values. Entries retain that category, source and quantity snapshot. Editing and
copying days preserve explicit categories. Legacy records stay unassigned. Group totals
use displayed entries; progress uses only logged values against user-entered calorie
and protein targets. Unknown nutrients and expenditure are not invented.

## Compatibility and ownership

Encrypted Room 5 adds program/session plan JSON and the meal category through an
explicit 4→5 migration, retaining earlier migrations. Authenticated backups include
the appended fields and accept legacy defaults. Exercise search indexes both canonical
English and Arabic names regardless of UI language, removing Arabic marks/tatweel.
The 40 original exercise illustrations/guides and simplified muscle maps remain.

Workout transfer v2 carries immutable plan details on programs and workouts. Current
Android/Apple readers accept v1 and v2. Encoding v1 refuses nonempty plan details;
unknown nested fields are rejected. Use the matching companion for v2 exports. The
file remains readable JSON with an explicit UI disclosure and platform destination
picker. Personal library notes, meals, profile and Nemory data are not transferred.

See [validation](VALIDATION.md) for executed Android/JVM checks and open release gates.
