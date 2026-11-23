# Exercise discomfort filters

## Product boundary

Forma provides **user-controlled library exclusions**, not diagnosis, treatment, rehabilitation prescriptions, or a medical contraindication engine. The feature is off by default and changes only which exercises are visible in the iPhone exercise library. It never rewrites saved programs, active sessions, logged workouts, transfer files, or history.

The app does not label exercises or machines as “recommended for pain.” A general exercise association is not enough evidence to recommend a movement for an individual's symptoms. Any future encouragement or rehabilitation rules require qualified clinical review, explicit boundaries, and regression cases before implementation.

## Current rules

A selected region hides exercises where any mapped muscle is either primary or supporting:

| User selection | Catalogued muscles used by the filter |
| --- | --- |
| Neck / upper back | `UPPER_BACK` |
| Shoulders | `FRONT_DELTS`, `REAR_DELTS` |
| Lower back | `ERECTORS` |

These are intentionally simple, deterministic direct-target preferences. The catalog has no distinct neck muscle, so the neck/upper-back choice uses only the existing upper-back association. It is not a claim that upper-back work causes or treats neck pain.

The predicate and stable preference encoding live in `apple/Sources/FormaCore/ExerciseDiscomfort.swift`. The iPhone presentation is in `apple/iPhone/ExerciseDiscomfortFilter.swift`. Selections are stored locally under `forma_discomfort_exclusions`; unknown stored values are ignored.

## Important limitations

- Indirect stabilization, axial loading, range of motion, tempo, load, technique, symptoms, and individual diagnoses are not fully represented by muscle associations.
- An exercise that remains visible is **not** certified suitable for the user's condition.
- Hiding an exercise is **not** a claim that it is universally unsafe.
- The filter does not modify an existing program even when that program contains a now-hidden exercise. The user remains in control of any program edits.
- English and Arabic explain these limits in the filter sheet.

## Evidence considered

NHS resources support a diagnosis-dependent, individualized approach rather than universal avoidance or universal exercise prescriptions:

- [Hampshire and Isle of Wight NHS MSK shoulder self-help](https://hiowhealthcare.nhs.uk/msk/self-help/shoulder): self-management and modifying aggravating activity.
- [Dynamic Health NHS neck-pain guidance](https://www.dynamichealth.nhs.uk/help-and-advice/neck-pain/): general neck-pain information and remaining active where appropriate.
- [NHS TIMS Management of Neck Pain](https://www.tims.nhs.uk/wp-content/uploads/2024/07/TIMS-Management-of-Neck-Pain-May-2024.pdf): describes shoulder strengthening for some neck-pain patients with gradual progression. This does not justify recommending an upper-back machine to everyone with neck symptoms.
- [NHS back-pain guidance](https://www.nhs.uk/conditions/back-pain/): general guidance and warning signs; not a Forma exercise-selection rule set.
- [NHS shoulder-impingement guidance](https://www.nhs.uk/conditions/shoulder-impingement-syndrome/): condition-specific context rather than a universal shoulder exclusion list.

These references informed the safety boundary; they are not represented as clinical validation of the filter.

## Verification

- `ExerciseDiscomfortTests.swift` covers default-off behavior, primary and supporting associations, combined regions, unrelated exercises, deterministic persistence, and unknown stored values.
- `scripts/check_apple_sources.py` checks the region mapping, library wiring, localization, and safety copy.
- GitHub Actions run [`45191884998`](https://github.com/7atem/Forma/actions/runs/45191884998), job `137752084876`, passed on commit `ff9cd48`: Swift tests, iPhone Simulator build, watchOS Simulator build and unsigned ARM64 IPA packaging. The portable source suite also passed with 191 matched EN/AR keys.
- No visual simulator/device interaction or qualified clinical review has occurred. Compilation validates source compatibility, not the filter's medical suitability, accessibility or real-device presentation. Any future encouragement/rehabilitation rules remain blocked pending qualified clinical rules and regression cases.
