# Rigged 3D Character Pipeline (`.usdz`, iPhone only)

Status: **iPhone loader only; no models are bundled.** Forma's procedural mannequin
(`ExerciseMotionSceneView.swift` / `ExerciseMotion3D.kt`, driven by `catalog/exercise-motion.json`)
remains what users see. The iPhone source can prefer a bundled `.usdz` file when one exists; Android has
**no** rigged-model loader and no 3D model library dependency. Adding one (for example Filament or
SceneView for `.glb`) would be a new dependency and needs its own review, size budget and tests.
The iPhone loader is source-only on this Windows host and has not been run on a simulator or device.

## 1. File naming and bundle location
Place exported models under `assets/exercises/res/models/` (the folder does not exist yet):
- **iPhone (SceneKit):** `exercise_3d_<exercise_id_lowercase>.usdz` (e.g. `exercise_3d_squat.usdz`).

`apple/project.yml` lists `../assets/exercises/res/models` as an optional resource folder, and
`RiggedModelLibrary.url(exerciseID:)` in `ExerciseMotionSceneView.swift` prefers a bundled `.usdz`
over the procedural mannequin. Exercise IDs are the stable catalog IDs; never rename them to match a model.

## 2. Rig, scale and orientation
- **Units:** metres (`1.0` Blender unit = `1.0 m`), character height about `1.75 m`.
- **Origin:** feet on `y = 0`, facing `+Z`, `+Y` up.
- **Animation:** one seamless looping repetition (first and last keyframes identical), baked at 30 fps.
  Reduced-motion users must still get a held still pose, and nothing is mirrored in RTL.
- **File budget:** target under `2.5 MB` per file with 1K textures; record the size in the release checklist.

## 3. Muscle highlight materials
- Name primary-muscle materials with `MUSCLE_PRIMARY` and supporting-muscle materials with
  `MUSCLE_SECONDARY` (e.g. `MAT_MUSCLE_PRIMARY`).
- Which regions are primary or supporting must match `catalog/exercise-muscles.json` for that exercise.
  A model whose materials disagree with the catalog must not ship. Colours show catalogued muscles,
  not measured activation.
- With highlights on, `ExerciseMotionSceneUIView` drives emission with `FormaTheme.musclePrimary` /
  `FormaTheme.muscleSupporting`; with highlights off, emission is cleared. Do not bake highlight colours
  into textures.

## 4. Licensing and release gate
- Bundle only original meshes and animations, or ones under a licence that allows redistribution inside an
  app binary (e.g. CC0). Some free services (such as Mixamo) allow use in projects but restrict
  redistributing the raw character or animation files; check the current terms and record the licence,
  source and hash for each file before bundling. Never extract models from other apps.
- Qualified pose/anatomy review remains a release gate before any form-coaching claim.
