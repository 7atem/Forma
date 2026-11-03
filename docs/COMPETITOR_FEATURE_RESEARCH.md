# Fitness and nutrition feature research

Researched 3 November 2026 from the products' own documentation. These are representative established related apps, not an independently measured popularity ranking. Availability may vary by platform, region or subscription. Features below describe the vendors' offerings, not independent evidence of coaching effectiveness. Forma does not reuse their proprietary artwork, exercise videos, nutrition databases or algorithms.

| App and primary source | Relevant patterns | Forma decision in alpha03 |
|---|---|---|
| [Hevy features](https://www.hevyapp.com/features/) | Previous values, rest timers, set types/RPE, weekly sets per muscle, muscle distribution, exercise progress, routines | Implemented original front/back muscle diagrams for all eight current exercises, primary/supporting counts, manual warm-up/RPE, previous-value copy and highest recorded working load. Existing saved routines/timers/history retained. |
| [Strong](https://www.strong.app/), [Apple Watch help](https://help.strongapp.io/article/222-strong-for-apple-watch), [warm-up calculator](https://help.strongapp.io/article/171-warm-up-calculator) | Simple set logging, warm-up sets, charts, watch set/timer controls, live sync and optional heart-rate/energy information | Warm-ups excluded from load charts/records and muscle counts. Native phone/watch logging source added. Forma does not claim Strong-style live mirrored sessions or HealthKit recording. |
| [Fitbod FAQs](https://fitbod.me/faqs/) | Equipment/goal-aware workouts, logged-history-based progression and recovery, exercise demonstrations and offline workouts | Keep local history and offline logging. Equipment-specific variants, licensed demonstrations and reviewed adaptation rules are future work. Muscle coloring is educational anatomy, never a recovery estimator. |
| [MyFitnessPal voice logging](https://support.myfitnesspal.com/hc/en-us/articles/30332897072269-How-to-log-food-with-your-voice), [food-entry guidance](https://blog.myfitnesspal.com/what-you-need-to-know-before-logging-food/) | Fast food entry, portion edits, saved foods/recipes and voice/barcode/photo entry | Existing sourced food/recipe library, portions and corrections retained. Prioritize validated Arabic entry friction; voice/OCR/barcode need licensing, coverage, offline capability and review. No fabricated food database or photo-to-calorie claims. |
| [MacroFactor product](https://macrofactor.com/macrofactor/) | Fast food logging, weekly macro adjustments and expenditure estimation from logged intake/weight | Keep coverage-aware reports and explicit check-ins. Actual calorie/expenditure adaptation remains behind reviewed rules and sufficient-history validation. Sensor calories never automatically increase food targets. |

## Implemented priority

1. Make each current exercise understandable: original front/back schematic, primary/supporting muscle names in English and Arabic, identical catalog on both platforms.
2. Reduce set-entry friction: copy previous values, optional warm-up and subjective RPE, editable sets and rest timer.
3. Show descriptive progress: best recorded external working load and separate primary/supporting associations over seven local dates. No estimated 1RM or unvalidated recovery score.
4. Add a practical Apple path: offline wrist logging, a paired iPhone companion and an explicit file bridge to Android. Source/build-validation status is in [APPLE_WATCH.md](APPLE_WATCH.md).

## Next priorities

Run the existing usability study before selecting additional fast-entry features. Then define equipment-specific exercises and obtain qualified review of technique text and the current simplified muscle mappings. Supersets, custom exercises, plate calculators, licensed demonstrations, reviewed coaching, HealthKit/Health Connect and reminders are separate roadmap items. Social feeds, leaderboards, subscriptions and cloud accounts are not required for this private local logging milestone.

No API, billing account, paid service or vendor data feed was introduced by this work. The product decisions in this document are our interpretation of the observed patterns.
