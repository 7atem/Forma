# 3D exercise motion

Twelve exercises show a looping, rotatable 3D figure in the exercise guide instead of only the two
still photos: **Squat, Barbell bench press, Barbell row, Conventional deadlift, Barbell overhead press,
Barbell curl, Skull crusher, Dumbbell curl, Hammer curl, Dumbbell shrug, Barbell back squat, and
Barbell Romanian deadlift**.
On iPhone, users can switch among **3D motion**, **Video** when a bundled clip exists, and **Photos**.

## What it is
- An original, code-built mannequin with volumetric body segments, drawn with Compose `Canvas` on
  Android and native SceneKit geometry on iPhone. No downloaded model files or network are used.
- Real continuous motion: poses are eased between keyframes up to 60 times a second and loop seamlessly.
- Drag sideways to rotate the figure. Bar, plates and bench are drawn where the exercise needs them.
- Body parts carrying the exercise's catalogued muscles glow on their **front or back** side
  (lime = main, amber = supporting), strongest during the working phase of the rep. Muscle mapping
  comes from `catalog/exercise-muscles.json`; colours show which muscles the catalog lists,
  **not measured activation, effort or recovery**. The iPhone guide has one highlight toggle shared
  by 3D motion, bundled video and photo modes.
- Respects reduced motion (iOS Reduce Motion, Android "Remove animations"): the figure holds a still
  pose. Screen readers hear the exercise and its working muscles.
- Phase order and the figure are never mirrored for Arabic/RTL.

## Source of truth
`catalog/exercise-motion.json` holds the skeleton, the muscle→body-part map and each clip's
keyframes: pelvis height/depth, trunk lean, head tilt and the hand (bar) target, plus timing,
camera angle, props and knee/elbow bend directions. Knees and elbows are solved by two-bone IK, so
feet stay planted and grip width stays constant.

`scripts/generate_exercise_motion.py` generates
`core/model/.../ExerciseMotion.Generated.kt` and `apple/Sources/FormaCore/ExerciseMotion.Generated.swift`.
It also replays the motion engine and fails if any frame overreaches a limb by more than 1.5 cm,
puts a joint below the floor, drives plates through the floor, or breaks the loop. Run with `--check`
in CI; never hand-edit the generated files.

Engine: `core/model/.../ExerciseMotion.kt` and `apple/Sources/FormaCore/ExerciseMotion.swift`
(identical maths). Renderers: `app/.../ExerciseMotion3D.kt`,
`apple/iPhone/ExerciseMotionSceneView.swift` (SceneKit) and
`apple/AppShared/ExerciseMotion3DView.swift` (timing, interaction and shared fallback renderer).
Tests: `ExerciseMotionTest.kt` (JVM) and `ExerciseMotionTests.swift` (`swift test`) check bone lengths,
planted feet, symmetry, floor, plates, loop continuity, IK and highlight roles.

## Adding an exercise
Add a clip to the JSON (start and end keys identical), run the generator, check the loop on a device,
and keep keyframes within the validator's limits. The exercise must already exist in the muscle catalog.
Expanding the pilot requires visual review on real screens; mathematical source checks do not establish
that the demonstration is anatomically or pedagogically correct.

## Limits
- Stylised mannequin, not a lifelike human; poses are illustrative demonstrations of the named
  variation, not motion capture or personal form coaching.
- **Qualified pose and cue review remains a release gate**, as for the photo guides.
- Not yet on Apple Watch screens.
- SceneKit source is compiled in macOS CI, but lighting, geometry, highlights, rotation and animation
  still require visual simulator/device acceptance. Compilation is not evidence of visual quality.
