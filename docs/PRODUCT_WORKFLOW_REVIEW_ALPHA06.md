# Fitness workflows: 18-product review — 4 November 2026

Official-site/help desk research, scoped to strength logging, guided training,
activity history and nutrition diaries. This is a broad representative review,
not an exhaustive census of every app, a hands-on usability study or an independent
popularity/rating benchmark. Platform, region and subscription availability differ.
Marketing claims about AI, recovery or health outcomes do not validate Forma.

No competitor art, videos, coach programs, food databases, algorithms or extended
written instructions were copied. These are product patterns implemented with
Forma's own typed models and original existing 40-exercise assets.

| Product / official source | Pattern worth examining | Decision for Forma |
| --- | --- | --- |
| [Hevy logging](https://www.hevyapp.com/features/track-workouts/) and [superset navigation](https://help.hevyapp.com/hc/en-us/articles/35650286563095-The-Complete-Guide-to-Supersets-and-Smart-Superset-Scrolling) | Reusable routines, set types, per-exercise rest and easier linked-exercise navigation | Add explicit user-entered targets and linked groups; after saving, select the next exercise without copying its load. |
| [Strong](https://www.strong.app/) | Focused log, charts, data portability and Watch access | Keep fast entry, explicit save, existing charts/export and manual bridge. Avoid cluttering the current exercise with unrelated controls. |
| [Fitbod FAQ](https://fitbod.me/faqs/) | Equipment/goal context, demos and adaptable workouts | Existing equipment filters and original guides; bilingual search improves discovery. Automatic personalized prescription remains review-gated. |
| [JEFIT](https://www.jefit.com/) | Plans with chosen exercises/rest/supersets, notes and logged progress | Snapshot the user's detailed plan into each workout; later program edits cannot rewrite history. |
| [FitNotes workflow](https://www.fitnotesapp.com/workout_tracking/) | Simple exercise search, routine or flexible logging, supersets and corrections | Preserve unstructured logging alongside optional targets; warm-ups do not satisfy working-set targets. |
| [Boostcamp features](https://www.boostcamp.app/features) | Program builder with sets/reps and linked exercises, per-exercise timers, swap and weekly reports | Add manual plan detail editing. Do not copy coach programs, strength scores or automated weight recommendations. |
| [StrengthLog](https://www.strengthlog.com/) | Workout log, routines and progress review | Maintain record ownership and editable history; calendar provides another route into the same completed records. |
| [Alpha Progression](https://alphaprogression.com/en) | Editable exercise/set/rep plans, instruction discovery, progress and export | Allow optional rep ranges without inventing defaults or load increases. Keep actual set entry independent of planned targets. |
| [Nike Training Club](https://www.nike.com/ntc-app) | Accessible workout discovery, goal context and guided content | Reduce planning-to-logging steps and keep original guides nearby. Licensed trainer video production is separate future work. |
| [Freeletics](https://www.freeletics.com/en/) | Time/equipment/location-aware workflow and adjustable training days | Favor explicit user choices and reusable plans. Adaptive exercise/calorie rules require qualified boundaries and validation. |
| [Sweat](https://sweat.com/) | Organized programs, home/gym options, progress and recipes | Reusable plans, equipment browsing and existing sourced recipe snapshots fit Forma; no copied specialty or population-specific prescriptions. |
| [Peloton app](https://www.onepeloton.com/app) | Guided sessions, varied durations, progress and consistent return paths | Make a saved session easier to execute and resume. Streaming classes, social features and third-party AI services are outside this offline milestone. |
| [Strava training log](https://support.strava.com/en-us/articles/15402077-training-log) | Calendar/activity overview and filtering into past activities | Add locale-aware monthly browsing of actual completed workouts and date-filtered history, without a recovery/fitness score or GPS collection. |
| [MyFitnessPal logging](https://support.myfitnesspal.com/hc/en-us/articles/360032274592-How-to-log-food-to-your-diary) | Choose a meal, choose a food, review serving size and save | Add breakfast/lunch/dinner/snack/unassigned context and grouped diary; retain explicit portions and sources. |
| [MacroFactor](https://macrofactor.com/macrofactor/) | Quick logging, reusable foods/recipes, copy-day and review | Existing source snapshots/copy-day remain; show logged progress against user-entered targets. No inferred energy expenditure or automatic calorie adjustment. |
| [Cronometer](https://cronometer.com/features/) | Meal/recipe tools, nutrition detail and traceable diary | Keep known macro totals; do not populate unknown micronutrients from estimates or unlicensed data. |
| [Lifesum](https://lifesum.com/) | Quick diary actions, nutrient breakdown and simple habit trackers | Streamline the diary and keep targets explicit. Water/habit extensions need their own typed persistence and are not simulated here. |
| [Yazio tutorial](https://help.yazio.com/hc/en-us/articles/11804776635281-Tutorial-of-the-Yazio-app) and [saved meals](https://help.yazio.com/hc/en-us/articles/360000620798-What-are-meals) | Diary as a daily hub, reusable meals, recipes and overview | Add meal grouping and user-target progress; maintain existing sourced food/recipe and repeat/copy workflows. |

## Selected end-to-end workflows

1. **Find:** Search either language, combine equipment/muscle filters, open the
   existing demo/map and choose exercises without a network request.
2. **Prepare:** Name a program, order exercises, optionally enter working-set
   counts/rep ranges, per-exercise rest and notes. Link an exercise to its previous
   neighbor to form a superset/circuit. Moving/removing exercises normalizes adjacent
   groups, preserves remaining targets/notes and never groups unrelated exercises.
3. **Train:** Starting a program snapshots all details. Explicit Save writes a set
   transactionally, then uses the persisted session-set counts for the next step.
   Warm-ups stay on the current exercise. Linked movements have no timer between
   forward steps; round wraps use configured rest. Entered targets are optional,
   extra sets remain possible and Finish is explicit. New exercises clear old input
   so a previous exercise's kg does not silently carry across.
4. **Review:** Browse completed activity by month/day or search the list. A day
   without logs does not prove no exercise happened. Calendar dates follow the
   device locale's week start; there is no schedule/recovery inference.
5. **Eat:** Choose the logging category, review a saved portion or enter known
   macros, then save. The diary groups recorded entries, and shows progress only
   against targets already entered by the user. Categories never infer meal times.
   Copies retain source/portion snapshots; old entries stay unassigned until edited.
6. **Keep:** Encrypted backups retain all details. New workout-transfer protocol v2
   carries plan snapshots; updated readers retain v1 support. Old readers reject
   v2 rather than silently dropping targets/groups. Apple source is updated but
   still requires actual Mac/Xcode and paired-device validation.

## Selection rationale and boundaries

Highest priority went to daily repeated actions: preparing a reusable program,
moving through it with fewer taps, finding records, and logging meals clearly.
These fit existing encrypted/offline/bilingual architecture and can be checked
with deterministic contracts and native UI tests. This is product judgment, not
proof these choices maximize retention or health outcomes; that needs the study.

Custom/timed/cardio exercise record types, training schedules/reminders, hydration,
licensed barcode/micronutrient catalogs, progress photos, consented social/coaching,
sensor recording and adaptive prescriptions remain separately scoped work. Every
new feature needs real persistence/validation and honest UI, not placeholder buttons.
Public release still requires qualified pose/cue review, physical-device/accessibility
and usability studies, and owner distribution/privacy decisions.
