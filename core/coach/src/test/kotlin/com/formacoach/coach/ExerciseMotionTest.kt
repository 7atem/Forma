package com.formacoach.coach

import com.formacoach.model.*
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class ExerciseMotionTest {
    private val pilot = listOf(
        Exercise.SQUAT, Exercise.BARBELL_BENCH_PRESS, Exercise.BARBELL_ROW,
        Exercise.CONVENTIONAL_DEADLIFT, Exercise.BARBELL_OVERHEAD_PRESS,
    )
    private val sk = ExerciseMotionCatalog.skeleton

    @Test fun `pilot exercises have clips and others fall back to photos`() {
        pilot.forEach { assertNotNull(it.name, ExerciseMotionCatalog.of(it)) }
        assertNull(ExerciseMotionCatalog.of(Exercise.LEG_PRESS))
    }

    @Test fun `bones keep their length, feet stay planted and nothing sinks below the floor`() {
        pilot.forEach { exercise ->
            val clip = ExerciseMotionCatalog.of(exercise)!!
            for (step in 0..200) {
                val pose = MotionEngine.pose(clip, MotionEngine.sample(clip, step / 200.0))
                val label = "${exercise.name} @ ${step / 200.0}"
                assertEquals(label, sk.thigh, (pose.kneeR - pose.hipR).length, 1e-6)
                assertEquals(label, sk.shin, (pose.ankleR - pose.kneeR).length, 1e-6)
                assertEquals(label, sk.upperArm, (pose.elbowR - pose.shoulderR).length, 1e-6)
                assertEquals(label, sk.forearm, (pose.handR - pose.elbowR).length, 1e-6)
                assertEquals(label, sk.torso, (pose.neckBase - pose.pelvis).length, 1e-6)
                assertEquals(label, clip.ankleHalf, pose.ankleR.x, 1e-6)
                assertEquals(label, 0.0, pose.ankleR.z, 1e-6)
                assertTrue(label, pose.joints.all { it.y >= -1e-3 })
                // Left and right mirror each other, so the bar stays level and centred.
                assertEquals(label, pose.handR.x, -pose.handL.x, 1e-9)
                assertEquals(label, pose.handR.y, pose.handL.y, 1e-9)
                if (clip.prop == MotionProp.BARBELL) assertTrue(label, pose.handR.y - clip.plateRadius >= -0.005)
            }
        }
    }

    @Test fun `loops are seamless and easing never overshoots`() {
        pilot.forEach { exercise ->
            val clip = ExerciseMotionCatalog.of(exercise)!!
            val start = MotionEngine.sample(clip, 0.0)
            val end = MotionEngine.sample(clip, 0.999999)
            assertEquals(start.pelvisY, end.pelvisY, 1e-4)
            assertEquals(start.hand.y, end.hand.y, 1e-4)
            val low = clip.keys.minOf { it.pelvisY }
            val high = clip.keys.maxOf { it.pelvisY }
            for (step in 0..100) {
                val y = MotionEngine.sample(clip, step / 100.0).pelvisY
                assertTrue(y >= low - 1e-9 && y <= high + 1e-9)
            }
        }
    }

    @Test fun `two-bone IK reaches reachable targets and bends towards the pole`() {
        val root = Vec3(0.0, 1.0, 0.0)
        val target = Vec3(0.0, 0.5, 0.0)
        val (knee, end) = MotionEngine.twoBone(root, target, 0.44, 0.42, Vec3(0.0, 0.0, 1.0))
        assertEquals(0.0, (end - target).length, 1e-9)
        assertTrue("bends forward", knee.z > 0.1)
        val (_, clamped) = MotionEngine.twoBone(root, Vec3(0.0, -2.0, 0.0), 0.44, 0.42, Vec3(0.0, 0.0, 1.0))
        assertTrue("unreachable target leaves a straight limb", abs((clamped - root).length - 0.86) < 1e-3)
    }

    @Test fun `highlights follow the muscle catalog and effort peaks in the working phase`() {
        val roles = MotionEngine.highlights(Exercise.SQUAT)
        assertEquals(2, roles[MusclePart(BodyPart.THIGH, MuscleSide.FRONT)])
        assertEquals(2, roles[MusclePart(BodyPart.PELVIS, MuscleSide.BACK)])
        assertEquals(1, roles[MusclePart(BodyPart.LOWER_TORSO, MuscleSide.FRONT)])
        val clip = ExerciseMotionCatalog.of(Exercise.SQUAT)!!
        val mid = (clip.workingStart + clip.workingEnd) / 2
        assertTrue(MotionEngine.effort(clip, mid) > 0.95)
        assertEquals(0.35, MotionEngine.effort(clip, clip.workingStart / 2), 1e-9)
    }
}
