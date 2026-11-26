# Exercise discomfort filters

## Product boundary

Forma provides **user-controlled library exclusions**, not diagnosis, treatment, rehabilitation prescriptions, or a medical contraindication engine. The feature is off by default and changes only which exercises are visible in the Android and iPhone exercise libraries. It never rewrites saved programs, active sessions, logged workouts, transfer files, or history.

The app does not label exercises or machines as “recommended for pain.” A general exercise association is not enough evidence to recommend a movement for an individual's symptoms. Any future encouragement or rehabilitation rules require qualified clinical review, explicit boundaries, and regression cases before implementation.

## Current rules (alpha07)

The person chooses **areas to go easy on** and, separately, **muscles they want to train more**. Forma never connects the two: it does not suggest muscles for an area or a condition.

| Area | Catalogued muscles used |
| --- | --- |
| Neck | `UPPER_TRAPS` detail (shrugs, overhead presses, raises, deadlifts) |
| Upper back | `MID_BACK` detail (rows and similar) |
| Shoulders | `FRONT_DELTS`, `REAR_DELTS` groups |
| Lower back | `ERECTORS` group |

An exercise matches when a listed muscle is primary or supporting. **Mark** (default for new selections) keeps every exercise visible with a "Works: …" note; **Hide** removes matches from the library list only. Muscles chosen to train more are listed first and their groups are labelled "Focus".

Earlier builds combined neck and upper back into one `NECK_UPPER_BACK` area that hid every upper-back exercise, including rows that some people want to keep. That stored value now decodes to both new areas, and a stored selection without a saved mode keeps hiding, so nothing changes silently for existing users; they can clear either area or switch to marking.

## Important limitations

- Indirect stabilization, axial loading, range of motion, tempo, load, technique, symptoms, and individual diagnoses are not fully represented by muscle associations.
- An exercise that remains visible is **not** certified suitable for the user's condition.
- Hiding an exercise is **not** a claim that it is universally unsafe.
- The filter does not modify an existing program even when that program contains a now-hidden exercise. The user remains in control of any program edits.
- English and Arabic explain these limits in the filter sheet.

## Evidence considered

Reviews of exercise for chronic neck pain report moderate-quality evidence that combined cervical, shoulder-blade and upper-body strengthening reduces pain in the short term, with uncertain dosing and no high-quality evidence overall ([Cochrane CD004250](https://www.cochrane.org/CD004250/BACK_exercise-therapy-neck-pain); [Gross et al. 2012](https://pubmed.ncbi.nlm.nih.gov/22895940/); [scapular dysfunction commentary](https://pubmed.ncbi.nlm.nih.gov/24816504/)). Regional NHS physiotherapy leaflets advise staying active within tolerance and stopping exercises that worsen pain ([Berkshire Healthcare](https://www.berkshirehealthcare.nhs.uk/advice/neck-pain); [Wrightington, Wigan and Leigh](https://wwl.nhs.uk/msk-neck-and-upper-back-pain)). This is why neck no longer hides mid-back work and why "train more" is a choice the person makes, not a rule Forma applies to symptoms.


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
