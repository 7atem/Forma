# Forma product brief

Decision date: 3 November 2026. Working name: Forma / فورمة. This is a separate product, repository and Android application. Nemory is preserved for later development.

## User and problem

Initial hypothesis: Arabic/English-speaking adults who train resistance roughly 2–4 times a week and want to track fat-loss efforts alongside strength. Their training, meals and measurements are scattered, and it is hard to review what was actually recorded together.

Value proposition: an easy daily log and an understandable weekly review grounded in the user's own history. Local storage and Arabic usability are product commitments. Their commercial appeal remains unvalidated.

## First testable journey

1. Choose a goal and weekly session count. Optionally enter already-chosen calorie/protein targets.
2. Open a training template, record exercise/reps/external load, and finish the session.
3. Enter a meal's whole-portion calories and macronutrients from a user-selected source. Repeat a recent portion without inventing nutrition data.
4. Record body weight and/or waist for the local calendar date.
5. See completed sessions, food logging coverage, recorded averages and the evidence window. Comparisons require adequate coverage in both windows.

The first alpha implements this logging journey. It does **not** deliver the full adaptive-coaching MVP. Templates are starting points for logging, not an individualized training program. Food records are user-entered whole-portion totals; there is no verified food catalog yet.

## Next product milestone

A reviewed program builder, exercise history, editable meal portions with reliable source provenance, explicit weekly check-in, and reviewed coaching proposals that the user can accept or reject. No LLM is responsible for numeric arithmetic or authoritative nutrition values. Targets do not change silently.

## Out of scope for the first release

Medical conditions, injury rehabilitation, pregnancy/minor-specific advice, therapeutic diets, camera-based form correction, social feeds, trainer/client dashboards, cloud synchronization and subscriptions. AI conversation, OCR, barcode/food databases, reminders and portable backup require separate implementation and validation before being advertised.

## Validation and success

Start with 10–15 consenting adult testers using their existing routines. Ask them to complete the journey without help and review the result. Observe logging friction, misunderstood quantities, RTL issues, and whether the review is understandable. Then a 30–50 person beta can evaluate repeat usage.

Proposed measures: onboarding completion, time to save a set, completed logged sessions, food-logging days, weekly return, review comprehension and willingness to pay. These are hypotheses, not observed outcomes. No analytics SDK is installed; early feedback can be collected through an opt-in study without exporting private logs by default. Do not assume food logging coverage proves adherence.

Pricing and the 30/60/90-day timeline from the concept are unvalidated planning inputs. Set release dates after the first usability study. No market-size or revenue claim has been accepted as evidence by this project.
