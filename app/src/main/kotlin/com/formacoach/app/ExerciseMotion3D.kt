package com.formacoach.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.formacoach.model.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Looping, rotatable 3D mannequin for the exercise guide. Body parts carrying the exercise's
 * catalogued muscles glow on their front or back side, strongest during the working phase.
 * Drawn with Compose primitives: no 3D library, model files or network.
 */
@Composable
internal fun ExerciseMotion3D(exercise: Exercise, playing: Boolean, modifier: Modifier = Modifier, showMuscleHighlights: Boolean = true) {
    val clip = remember(exercise) { ExerciseMotionCatalog.of(exercise) } ?: return
    val reduceMotion = rememberReduceMotion()
    val animate = playing && !reduceMotion
    // When still, show the most informative pose: the end of the first movement.
    var phase by remember(exercise) { mutableDoubleStateOf(clip.keys[1].t) }
    var yaw by rememberSaveable(exercise) { mutableDoubleStateOf(clip.defaultYaw) }
    LaunchedEffect(animate, clip) {
        if (!animate) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                phase = (phase + (now - last) / 1e9 / clip.cycleSeconds) % 1.0
                last = now
            }
        }
    }
    val bounds = remember(clip) { MotionBounds.of(clip) }
    val highlights = remember(exercise, showMuscleHighlights) {
        if (showMuscleHighlights) MotionEngine.highlights(exercise) else emptyMap()
    }
    val colors = MaterialTheme.colorScheme
    val palette = MotionPalette(
        body = lerp(colors.onSurfaceVariant, colors.surface, 0.15f),
        bodyFar = lerp(colors.onSurfaceVariant, colors.surface, 0.55f),
        sheen = colors.onSurface,
        primary = primaryMuscleColor(),
        secondary = supportingMuscleColor(),
        metal = lerp(colors.onSurface, colors.surface, 0.35f),
        plate = lerp(colors.onSurface, colors.surface, 0.7f),
        bench = lerp(colors.surfaceVariant, colors.onSurface, 0.15f),
        shadow = colors.onSurface.copy(alpha = 0.10f),
    )
    val activation = remember(exercise) { ExerciseMuscles.of(exercise) }
    val muscleNames = (activation.primary + activation.secondary).map { stringResource(muscleLabel(it)) }.joinToString()
    val description = stringResource(R.string.motion_3d_description, stringResource(exerciseLabel(exercise)), muscleNames)
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("exercise_motion_3d")
            .semantics { contentDescription = description }
            .pointerInput(exercise) {
                detectHorizontalDragGestures { change, dx ->
                    change.consume()
                    yaw = (yaw - dx * 0.45 + 360.0) % 360.0
                }
            },
    ) {
        val pose = MotionEngine.pose(clip, MotionEngine.sample(clip, phase))
        drawFigure(clip, pose, highlights, MotionEngine.effort(clip, phase), bounds, yaw, palette)
    }
}

private data class MotionPalette(
    val body: Color, val bodyFar: Color, val sheen: Color, val primary: Color, val secondary: Color,
    val metal: Color, val plate: Color, val bench: Color, val shadow: Color,
)

/** Bounding sphere over the whole loop, so the camera framing never jumps while rotating. */
private class MotionBounds(val center: Vec3, val radius: Double) {
    companion object {
        fun of(clip: MotionClip): MotionBounds {
            var lo = Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
            var hi = Vec3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)
            fun include(p: Vec3, pad: Double) {
                lo = Vec3(min(lo.x, p.x - pad), min(lo.y, p.y - pad), min(lo.z, p.z - pad))
                hi = Vec3(max(hi.x, p.x + pad), max(hi.y, p.y + pad), max(hi.z, p.z + pad))
            }
            for (step in 0..32) {
                val pose = MotionEngine.pose(clip, MotionEngine.sample(clip, step / 32.0))
                pose.joints.forEach { include(it, 0.12) }
                if (clip.prop == MotionProp.BARBELL) {
                    val mid = (pose.handL + pose.handR) * 0.5
                    include(mid + Vec3(0.62, 0.0, 0.0), clip.plateRadius)
                    include(mid - Vec3(0.62, 0.0, 0.0), clip.plateRadius)
                }
            }
            clip.bench?.let { include(Vec3(it.halfWidth, it.top, it.zFrom), 0.0); include(Vec3(-it.halfWidth, 0.0, it.zTo), 0.0) }
            include(Vec3(0.0, 0.0, 0.0), 0.0)
            val center = (lo + hi) * 0.5
            return MotionBounds(center, (hi - lo).length * 0.5)
        }
    }
}

private class Camera(size: Size, private val bounds: MotionBounds, yawDegrees: Double) {
    private val yaw = Math.toRadians(yawDegrees)
    private val pitch = Math.toRadians(12.0)
    private val distance = bounds.radius * 4.0
    private val scale = min(size.width, size.height) * 0.47 / bounds.radius
    private val cx = size.width / 2
    private val cy = size.height / 2

    data class Projected(val offset: Offset, val depth: Double, val perspective: Double)

    fun project(p: Vec3): Projected {
        val q = p - bounds.center
        val x1 = q.x * cos(yaw) - q.z * sin(yaw)
        val z1 = q.x * sin(yaw) + q.z * cos(yaw)
        val y2 = q.y * cos(pitch) - z1 * sin(pitch)
        val z2 = q.y * sin(pitch) + z1 * cos(pitch)
        val depth = distance - z2
        val perspective = distance / depth
        return Projected(
            Offset((cx + x1 * scale * perspective).toFloat(), (cy - y2 * scale * perspective).toFloat()),
            depth,
            perspective,
        )
    }

    fun pixels(meters: Double, perspective: Double): Float = (meters * scale * perspective).toFloat()

    /** Horizontal squash of a disc whose face points along x (barbell plates). */
    val plateSquash: Float get() = abs(sin(yaw)).toFloat().coerceAtLeast(0.08f)
}

private fun DrawScope.drawFigure(
    clip: MotionClip,
    pose: BodyPose,
    highlights: Map<MusclePart, Int>,
    effort: Double,
    bounds: MotionBounds,
    yaw: Double,
    palette: MotionPalette,
) {
    val camera = Camera(size, bounds, yaw)
    val items = mutableListOf<Pair<Double, () -> Unit>>()
    val nearDepth = camera.project(bounds.center).depth - bounds.radius
    fun shade(depth: Double): Color {
        val t = ((depth - nearDepth) / (2 * bounds.radius)).coerceIn(0.0, 1.0).toFloat()
        return lerp(palette.body, palette.bodyFar, t)
    }
    fun roleColor(role: Int) = if (role >= 2) palette.primary else palette.secondary

    fun capsule(a: Vec3, b: Vec3, radius: Double, part: BodyPart?, front: Vec3?) {
        val pa = camera.project(a)
        val pb = camera.project(b)
        val depth = (pa.depth + pb.depth) / 2
        items += depth to {
            val width = camera.pixels(radius * 2, (pa.perspective + pb.perspective) / 2)
            drawLine(shade(depth), pa.offset, pb.offset, width, StrokeCap.Round)
            // Soft top-light sheen gives the limb volume.
            val lift = Offset(-width * 0.12f, -width * 0.16f)
            drawLine(palette.sheen.copy(alpha = 0.10f), pa.offset + lift, pb.offset + lift, width * 0.38f, StrokeCap.Round)
            if (part != null) {
                // Supporting muscles first and thinner, so main muscles stay on top and readable.
                val bands = MuscleSide.entries.mapNotNull { side -> highlights[MusclePart(part, side)]?.let { side to it } }
                    .sortedBy { it.second }
                for ((side, role) in bands) {
                    val alpha = (0.35 + 0.65 * effort).toFloat() * if (role >= 2) 1f else 0.75f
                    val color = roleColor(role).copy(alpha = alpha)
                    if (side == MuscleSide.BOTH || front == null) {
                        drawLine(color, pa.offset, pb.offset, width * if (role >= 2) 0.62f else 0.3f, StrokeCap.Round)
                    } else {
                        val mid = (a + b) * 0.5
                        val tip = camera.project(mid + front * (if (side == MuscleSide.FRONT) 0.05 else -0.05))
                        val origin = camera.project(mid)
                        var dir = tip.offset - origin.offset
                        val len = sqrt(dir.x * dir.x + dir.y * dir.y)
                        // Facing the camera straight on: the band sits over the middle of the limb.
                        dir = if (len > 0.5f) dir / len else Offset.Zero
                        val shift = dir * (width * 0.22f)
                        drawLine(color, pa.offset + shift, pb.offset + shift, width * 0.5f, StrokeCap.Round)
                    }
                }
            }
        }
    }

    fun limbFront(from: Vec3, to: Vec3): Vec3 {
        val f = (to - from).normalized() cross Vec3(1.0, 0.0, 0.0)
        return if (f.length < 0.2) pose.torsoFront else f.normalized()
    }

    // Floor shadow first, always behind everything.
    val shadowCenter = camera.project(Vec3(0.0, 0.0, if (clip.bench != null) -0.4 else 0.0))
    val shadowW = camera.pixels(0.55, shadowCenter.perspective)
    drawOval(palette.shadow, shadowCenter.offset - Offset(shadowW, shadowW * 0.22f), Size(shadowW * 2, shadowW * 0.44f))

    clip.bench?.let { bench ->
        val corners = listOf(
            Vec3(-bench.halfWidth, bench.top, bench.zFrom), Vec3(bench.halfWidth, bench.top, bench.zFrom),
            Vec3(bench.halfWidth, bench.top, bench.zTo), Vec3(-bench.halfWidth, bench.top, bench.zTo),
        ).map { camera.project(it) }
        val depth = corners.maxOf { it.depth } + 0.01
        items += depth to {
            val path = Path().apply {
                moveTo(corners[0].offset.x, corners[0].offset.y)
                corners.drop(1).forEach { lineTo(it.offset.x, it.offset.y) }
                close()
            }
            drawPath(path, palette.bench)
            listOf(bench.zFrom + 0.08, bench.zTo - 0.08).forEach { z ->
                val top = camera.project(Vec3(0.0, bench.top, z))
                val foot = camera.project(Vec3(0.0, 0.0, z))
                drawLine(palette.bench, top.offset, foot.offset, camera.pixels(0.05, top.perspective), StrokeCap.Round)
            }
        }
    }

    // Torso, pelvis, neck and head.
    val midTorso = pose.pelvis + pose.torsoUp * 0.24
    capsule(pose.hipL, pose.hipR, 0.10, BodyPart.PELVIS, pose.torsoFront)
    capsule(pose.pelvis, midTorso, 0.125, BodyPart.LOWER_TORSO, pose.torsoFront)
    capsule(midTorso, pose.neckBase - pose.torsoUp * 0.06, 0.15, BodyPart.UPPER_TORSO, pose.torsoFront)
    capsule(pose.shoulderL, pose.shoulderR, 0.07, null, null)
    capsule(pose.neckBase, pose.neckBase + (pose.head - pose.neckBase) * 0.45, 0.05, null, null)
    val head = camera.project(pose.head)
    items += head.depth to {
        val r = camera.pixels(ExerciseMotionCatalog.skeleton.headRadius, head.perspective)
        drawCircle(shade(head.depth), r, head.offset)
        drawCircle(palette.sheen.copy(alpha = 0.10f), r * 0.55f, head.offset + Offset(-r * 0.25f, -r * 0.3f))
    }

    // Limbs, each side separately so depth sorting handles crossing arms and legs.
    listOf(
        listOf(pose.hipR, pose.kneeR, pose.ankleR, pose.toeR, pose.shoulderR, pose.elbowR, pose.handR),
        listOf(pose.hipL, pose.kneeL, pose.ankleL, pose.toeL, pose.shoulderL, pose.elbowL, pose.handL),
    ).forEach { (hip, knee, ankle, toe, shoulder, elbow, hand) ->
        capsule(hip, knee, 0.085, BodyPart.THIGH, limbFront(hip, knee))
        capsule(knee, ankle, 0.06, BodyPart.SHIN, limbFront(knee, ankle))
        capsule(ankle, toe, 0.04, BodyPart.FOOT, null)
        capsule(shoulder, shoulder + (elbow - shoulder) * 0.3, 0.075, BodyPart.SHOULDER, pose.torsoFront)
        capsule(shoulder, elbow, 0.055, BodyPart.UPPER_ARM, limbFront(shoulder, elbow))
        capsule(elbow, hand, 0.045, BodyPart.FOREARM, limbFront(elbow, hand))
    }

    if (clip.prop == MotionProp.BARBELL) {
        val mid = (pose.handL + pose.handR) * 0.5
        val axis = Vec3(1.0, 0.0, 0.0)
        val barA = camera.project(mid - axis * 0.62)
        val barB = camera.project(mid + axis * 0.62)
        items += min(barA.depth, barB.depth) - 0.02 to {
            drawLine(palette.metal, barA.offset, barB.offset, camera.pixels(0.03, barA.perspective), StrokeCap.Round)
        }
        listOf(-0.50, 0.50).forEach { x ->
            val plate = camera.project(mid + axis * x)
            items += plate.depth to {
                val ry = camera.pixels(clip.plateRadius, plate.perspective)
                val rx = ry * camera.plateSquash + camera.pixels(0.025, plate.perspective)
                drawOval(palette.plate, plate.offset - Offset(rx, ry), Size(rx * 2, ry * 2))
                drawOval(palette.metal.copy(alpha = 0.5f), plate.offset - Offset(rx * 0.3f, ry * 0.3f), Size(rx * 0.6f, ry * 0.6f))
            }
        }
    }

    items.sortedByDescending { it.first }.forEach { (_, draw) -> draw() }
}

private operator fun <T> List<T>.component6(): T = this[5]
private operator fun <T> List<T>.component7(): T = this[6]
