# Fitness product review — 4 November 2026

Reviewed official product/help pages, not an independent download ranking or a claim
that every advertised feature is available on every platform or free tier. Vendor
claims about recovery, AI accuracy and outcomes are not clinical validation for Forma.
No proprietary images, datasets, routines or written instructions were copied.

| Product / primary source | Useful patterns observed | Forma decision |
| --- | --- | --- |
| [Hevy features](https://www.hevyapp.com/features/) | Previous values, set types/RPE, rest timer, muscle charts, notes, routines, plate calculator, supersets | Existing logging/history preserved; alpha05 adds notes, reusable program copies and exact plate loading. Superset scheduling remains future work. |
| [Strong](https://www.strong.app/) | Simple training log, progress charts, Apple Watch, data export | Existing charts and explicit Watch file bridge; alpha05 adds completed-set CSV. Native Apple source still needs Mac and paired-device testing. |
| [Fitbod FAQ](https://fitbod.me/faqs/) | Equipment-aware exercise selection, demonstrations, history-informed workouts | Add equipment filters and similar-primary-muscle exploration. Keep exercise choices explicit; no unvalidated automatic load/recovery prescription. |
| [JEFIT](https://www.jefit.com/) | Exercise library, configurable routines/rest, workout notes, watch logging | Expand to 40 named exercises with original stills/cues, retain rest and program snapshots, add personal notes and repeat-as-new draft. |
| [FitNotes tools](https://www.fitnotesapp.com/workout_tools/) and [exercises](https://www.fitnotesapp.com/exercises/) | Focused logging, exercise organization, plate calculations | Add filters/favorites/previously logged filter and a bounded, exact balanced plate solver. No estimated 1RM used as a prescription. |
| [MacroFactor](https://macrofactor.com/macrofactor/) | Fast logging, custom foods/recipes, copy meals/days, weekly review, explicit manual mode | Existing source-attributed food/recipe snapshots and manual targets; add review/confirm copy-day preserving portions and sources. No automatic calorie changes or photo-derived nutrition claims. |
| [Cronometer features](https://cronometer.com/features/) | Food/recipe tools, nutrient analysis and device integrations | Keep explicit sourced portions and logged totals. Micronutrient database and device sensor integrations need separately licensed/verified inputs before implementation. |

## Selected alpha05 engineering scope

- 32 additional exercises, for 40 total; each has EN/AR labels, equipment, primary
  and simplified supporting muscles, original two-phase 3D PNG and three cues.
- Search + equipment + primary muscle + favorites + previously logged filters.
  These filters combine; clear restores the full catalog. The library starts with
  12 cards and loads more explicitly to bound initial thumbnail work.
- Favorites and personal exercise notes persist in encrypted Room v4, participate
  in snapshot/backup replacement and preserve each other on edits.
- Explore exercises sharing a primary muscle. This is a browsing relationship,
  not proof of equivalent biomechanics, rehabilitation suitability or plan advice.
- Duplicate a saved program into an editable copy; repeat completed history into
  a fresh draft without completed sets, old IDs, imported sensor data or loads.
- Export completed working/warm-up sets as canonical kg CSV through Android SAF.
  Plaintext disclosure before export; quoted cells and formula-sensitive names
  escaped; drafts excluded. This is separate from authenticated encrypted backup.
- Exact balanced plate loading from user-selected total, bar and available metric
  pairs; bounded search minimizes plate count and reports unavailable combinations.
- Copy a recorded meal day after review/confirmation. Append new IDs, retain source
  snapshots and portions, reject stale source previews atomically, maximum 200 meals.

## Deferred product work

Superset/circuit timing, duration/cardio/assistance-weight record types, user-defined
exercises, multi-week periodization, social/coaching services, barcode catalogs,
micronutrients, progress photos, sensor recording and automatic prescriptions are
separate future milestones. They need typed models, explicit privacy/licensing and
validation rather than cosmetic switches or simulated integrations.

Expert exercise pose/cue review, actual Apple builds/paired hardware and physical
phone usability studies remain release gates. Completing this engineering milestone
does not mean all competitors' products or those external gates have been completed.
