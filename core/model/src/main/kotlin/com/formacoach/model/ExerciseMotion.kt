package com.formacoach.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Code-built 3D exercise motion. Clips come from catalog/exercise-motion.json via
 * scripts/generate_exercise_motion.py, which replays this same maths to validate every clip.
 * Motion is an illustrative demonstration, not measured capture or personal coaching.
 * Origin: between the planted ankles on the floor; +x figure's right, +y up, +z forward.
 */

data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length: Double get() = sqrt(this dot this)
    fun normalized(): Vec3 = if (length > 1e-9) this * (1.0 / length) else Vec3(0.0, 0.0, 0.0)
    fun mirroredX(sign: Double) = Vec3(x * sign, y, z)
}

data class MotionSkeleton(
    val ankleHeight: Double, val hipHalf: Double, val thigh: Double, val shin: Double, val foot: Double,
    val torso: Double, val shoulderHalf: Double, val neck: Double, val headRadius: Double,
    val upperArm: Double, val forearm: Double,
)

enum class BodyPart { PELVIS, LOWER_TORSO, UPPER_TORSO, SHOULDER, UPPER_ARM, FOREARM, THIGH, SHIN, FOOT, NECK, HEAD }
enum class MuscleSide { FRONT, BACK, BOTH }
data class MusclePart(val part: BodyPart, val side: MuscleSide)
enum class MotionProp { NONE, BARBELL }
data class BenchProp(val top: Double, val zFrom: Double, val zTo: Double, val halfWidth: Double)

/** One keyframe: pelvis height/depth, trunk lean (+forward), head tilt (+chin down), right-hand target. */
data class MotionKey(val t: Double, val pelvisY: Double, val pelvisZ: Double, val trunk: Double, val head: Double, val hand: Vec3)

data class MotionClip(
    val cycleSeconds: Double,
    val defaultYaw: Double,
    val prop: MotionProp,
    val plateRadius: Double,
    val bench: BenchProp?,
    val ankleHalf: Double,
    val kneePole: Vec3,
    val elbowPole: Vec3,
    val workingStart: Double,
    val workingEnd: Double,
    val keys: List<MotionKey>,
)

/** World-space joints for one frame, plus the torso's forward-facing direction. */
data class BodyPose(
    val pelvis: Vec3, val neckBase: Vec3, val head: Vec3, val torsoUp: Vec3, val torsoFront: Vec3,
    val hipR: Vec3, val hipL: Vec3, val kneeR: Vec3, val kneeL: Vec3, val ankleR: Vec3, val ankleL: Vec3,
    val toeR: Vec3, val toeL: Vec3, val shoulderR: Vec3, val shoulderL: Vec3,
    val elbowR: Vec3, val elbowL: Vec3, val handR: Vec3, val handL: Vec3,
) {
    val joints: List<Vec3> get() = listOf(pelvis, neckBase, head, hipR, hipL, kneeR, kneeL, ankleR, ankleL, toeR, toeL, shoulderR, shoulderL, elbowR, elbowL, handR, handL)
}

object MotionEngine {
    private fun Double.rad() = this * PI / 180.0

    /** Pose parameters at loop phase [t] (0..1), eased between keyframes. */
    fun sample(clip: MotionClip, t: Double): MotionKey {
        val phase = ((t % 1.0) + 1.0) % 1.0
        val keys = clip.keys
        for (i in 0 until keys.size - 1) {
            val a = keys[i]
            val b = keys[i + 1]
            if (phase >= a.t && phase <= b.t) {
                val f = if (b.t == a.t) 0.0 else (phase - a.t) / (b.t - a.t)
                val e = f * f * (3 - 2 * f)
                fun lerp(x: Double, y: Double) = x + (y - x) * e
                return MotionKey(
                    phase, lerp(a.pelvisY, b.pelvisY), lerp(a.pelvisZ, b.pelvisZ), lerp(a.trunk, b.trunk), lerp(a.head, b.head),
                    Vec3(lerp(a.hand.x, b.hand.x), lerp(a.hand.y, b.hand.y), lerp(a.hand.z, b.hand.z)),
                )
            }
        }
        return keys.first()
    }

    /** Two-bone IK: returns (middle joint, reached end). An unreachable target leaves the limb straight. */
    fun twoBone(root: Vec3, target: Vec3, l1: Double, l2: Double, pole: Vec3): Pair<Vec3, Vec3> {
        val offset = target - root
        val d = offset.length
        val direction = if (d > 1e-9) offset * (1.0 / d) else Vec3(0.0, -1.0, 0.0)
        val reach = min(max(d, abs(l1 - l2) + 1e-4), l1 + l2 - 1e-4)
        val a = (l1 * l1 - l2 * l2 + reach * reach) / (2 * reach)
        val h = sqrt(max(0.0, l1 * l1 - a * a))
        var bend = pole - direction * (pole dot direction)
        if (bend.length < 1e-6) bend = Vec3(0.0, 0.0, 1.0)
        bend = bend.normalized()
        return Pair(root + direction * a + bend * h, root + direction * reach)
    }

    fun pose(clip: MotionClip, key: MotionKey, sk: MotionSkeleton = ExerciseMotionCatalog.skeleton): BodyPose {
        val pelvis = Vec3(0.0, key.pelvisY, key.pelvisZ)
        val alpha = key.trunk.rad()
        val up = Vec3(0.0, cos(alpha), sin(alpha))
        val front = Vec3(0.0, -sin(alpha), cos(alpha))
        val neckBase = pelvis + up * sk.torso
        val beta = key.head.rad()
        val headDir = up * cos(beta) + front * sin(beta)
        val head = neckBase + headDir * (sk.neck + sk.headRadius)
        fun side(s: Double): List<Vec3> {
            val hip = pelvis + Vec3(s * sk.hipHalf, 0.0, 0.0)
            val (knee, ankle) = twoBone(hip, Vec3(s * clip.ankleHalf, sk.ankleHeight, 0.0), sk.thigh, sk.shin, clip.kneePole.mirroredX(s))
            val toe = ankle + Vec3(0.0, -0.06, sk.foot)
            val shoulder = neckBase + Vec3(s * sk.shoulderHalf, 0.0, 0.0)
            val (elbow, hand) = twoBone(shoulder, key.hand.mirroredX(s), sk.upperArm, sk.forearm, clip.elbowPole.mirroredX(s))
            return listOf(hip, knee, ankle, toe, shoulder, elbow, hand)
        }
        val r = side(1.0)
        val l = side(-1.0)
        return BodyPose(
            pelvis, neckBase, head, up, front,
            r[0], l[0], r[1], l[1], r[2], l[2], r[3], l[3], r[4], l[4], r[5], l[5], r[6], l[6],
        )
    }

    /**
     * Highlight strength for the working muscles: a steady base, rising through the working
     * (usually lifting) phase. This visualises the demo's timing, not measured activation.
     */
    fun effort(clip: MotionClip, t: Double): Double {
        val phase = ((t % 1.0) + 1.0) % 1.0
        if (phase < clip.workingStart || phase > clip.workingEnd) return 0.35
        val p = (phase - clip.workingStart) / (clip.workingEnd - clip.workingStart)
        return 0.35 + 0.65 * sin(PI * p)
    }

    /** Highest role per body part and side: 2 = primary, 1 = supporting. */
    fun highlights(exercise: Exercise): Map<MusclePart, Int> {
        val activation = ExerciseMuscles.of(exercise)
        val roles = mutableMapOf<MusclePart, Int>()
        activation.secondary.forEach { m -> ExerciseMotionCatalog.muscleParts[m]?.let { roles[it] = max(roles[it] ?: 0, 1) } }
        activation.primary.forEach { m -> ExerciseMotionCatalog.muscleParts[m]?.let { roles[it] = 2 } }
        return roles
    }
}
