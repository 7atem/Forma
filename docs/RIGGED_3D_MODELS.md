# Rigged 3D Character Pipeline (`.usdz` & `.glb`)

Forma's built-in procedural capsule mannequin (`ExerciseMotionSceneView.swift` / `ExerciseMotion3D.kt`)
is a lightweight fallback. To replace it with studio-quality athletic 3D characters, export authored
rigged animations from **Blender / Mixamo** using the naming and material conventions below.

## 1. File Naming & Bundle Location
Place exported models under `assets/exercises/res/models/`:
- **iOS (SceneKit):** `exercise_3d_<exercise_id_lowercase>.usdz` (e.g. `exercise_3d_squat.usdz`)
- **Android (Filament / SceneView):** `exercise_3d_<exercise_id_lowercase>.glb` (e.g. `exercise_3d_squat.glb`)

`apple/project.yml` already includes `../assets/exercises/res/models` as an optional resource folder,
and `RiggedModelLibrary.url(exerciseID:)` in `ExerciseMotionSceneView.swift` automatically prefers
any bundled `.usdz` over the procedural capsule mannequin.

## 2. Rig, Scale & Orientation Specs
- **Units:** Metres (`1.0` Blender unit = `1.0 m`), character height ~`1.75 m`.
- **Origin:** Feet planted on `y = 0`, facing `+Z`, `+Y` up.
- **Animation:** Single seamless looping rep (`0%` and `100%` keyframes identical), baked at 30 fps.
- **File budget:** Target `< 2.5 MB` per `.usdz`/`.glb` (use Draco/meshopt compression for `.glb` and 1K PBR textures).

## 3. Muscle Highlight Material Convention
To let Forma's existing **Muscle highlights** toggle glow working muscles dynamically on the rigged mesh:
- Name primary-muscle mesh materials with `MUSCLE_PRIMARY` (e.g. `MAT_MUSCLE_PRIMARY`).
- Name supporting-muscle mesh materials with `MUSCLE_SECONDARY` (e.g. `MAT_MUSCLE_SECONDARY`).
- When the user toggles Muscle Highlights ON, `ExerciseMotionSceneUIView` drives PBR emission intensity
  with `FormaTheme.musclePrimary` / `FormaTheme.muscleSupporting` in sync with the rep's working phase.
  When toggled OFF, emission is cleared to `0` so the clean athletic character is unobstructed.

## 4. Licensing & Release Gate
- Only bundle original or permissively licensed (e.g. CC0 / owned commercial license) character meshes
  and animations. Never extract or redistribute models from competitor apps.
- Qualified pose/anatomy review remains a release gate before claiming validated form coaching.
