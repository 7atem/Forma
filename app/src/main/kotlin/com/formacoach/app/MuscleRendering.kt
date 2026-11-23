package com.formacoach.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.core.graphics.PathParser
import com.formacoach.model.ExerciseMuscles
import com.formacoach.model.MuscleRegion
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Parsed once: the catalog geometry is static and shared by every muscle drawing. */
internal object MuscleGeometry {
    val outline: Path by lazy { parse(ExerciseMuscles.outline) }
    val regions: List<ParsedRegion> by lazy {
        ExerciseMuscles.regions.map { region -> parse(region.path).let { ParsedRegion(region, it, it.getBounds()) } }
    }
    private fun parse(data: String): Path = checkNotNull(PathParser.createPathFromPathData(data)).asComposePath()
}

internal class ParsedRegion(val region: MuscleRegion, val path: Path, val bounds: Rect)

/** How a region is marked; supporting regions also get a dashed edge so meaning never relies on colour alone. */
internal enum class MuscleRole { PRIMARY, SUPPORTING, OTHER }

internal data class MusclePalette(
    val skinLight: Color,
    val skin: Color,
    val skinShade: Color,
    val tissue: Color,
    val contour: Color,
    val dark: Boolean,
)

@Composable
internal fun musclePalette(): MusclePalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) {
        MusclePalette(Color(0xFF3A4C45), Color(0xFF2B3A34), Color(0xFF1B2723), Color(0xFF55665E), Color(0xFF8FA39A), true)
    } else {
        MusclePalette(Color(0xFFF2F5F2), Color(0xFFDCE4DE), Color(0xFFC3CEC6), Color(0xFFB9C6BE), Color(0xFF6F8378), false)
    }
}

/**
 * Draws one view of the original schematic body in the catalog's 100 × 220 space, scaled to fit.
 * Shading, fibres and contours are illustrative only: colours show catalogued muscles, never measured activation.
 */
internal fun DrawScope.drawMuscleBody(
    front: Boolean,
    palette: MusclePalette,
    fibres: Boolean,
    fill: (MuscleRegion) -> Pair<Color, MuscleRole>?,
) {
    val factor = minOf(size.width / 100f, size.height / 220f)
    translate((size.width - 100f * factor) / 2f, (size.height - 220f * factor) / 2f) {
        scale(factor, factor, pivot = Offset.Zero) {
            val outline = MuscleGeometry.outline
            // Soft drop shadow and a lit-from-upper-left body give the figure volume.
            translate(1.4f, 1.8f) { drawPath(outline, Color.Black.copy(alpha = if (palette.dark) 0.35f else 0.14f)) }
            drawPath(
                outline,
                Brush.linearGradient(listOf(palette.skinLight, palette.skin, palette.skinShade), start = Offset(25f, 10f), end = Offset(80f, 210f)),
            )
            MuscleGeometry.regions.filter { it.region.front == front }.forEach { parsed ->
                val marked = fill(parsed.region)
                val base = marked?.first ?: palette.tissue
                val role = marked?.second ?: MuscleRole.OTHER
                drawMuscleRegion(parsed, base, role, palette, fibres)
            }
            drawPath(outline, palette.contour.copy(alpha = 0.7f), style = Stroke(width = 0.7f))
        }
    }
}

private fun DrawScope.drawMuscleRegion(parsed: ParsedRegion, base: Color, role: MuscleRole, palette: MusclePalette, fibres: Boolean) {
    val path = parsed.path
    val b = parsed.bounds
    val extent = max(b.width, b.height)
    val active = role != MuscleRole.OTHER
    drawPath(path, base)
    clipPath(path) {
        // Rounded belly: a highlight toward the light, and darker edges where the muscle turns away.
        drawRect(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = if (active) 0.38f else 0.2f), Color.Transparent),
                center = Offset(b.left + b.width * 0.38f, b.top + b.height * 0.3f),
                radius = extent * 0.7f,
            ),
            topLeft = b.topLeft, size = b.size,
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = if (palette.dark) 0.42f else 0.26f)),
                center = b.center,
                radius = extent * 0.62f,
            ),
            topLeft = b.topLeft, size = b.size,
        )
        if (fibres) drawFibres(b, parsed.region.fiber, if (active) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f))
    }
    drawPath(path, Color.Black.copy(alpha = if (active) 0.45f else 0.25f), style = Stroke(width = 0.45f))
    if (role == MuscleRole.SUPPORTING) {
        drawPath(
            path,
            Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 0.55f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.4f, 1f))),
        )
    }
}

/** Parallel striations along the region's catalogued fibre direction, clipped by the caller. */
private fun DrawScope.drawFibres(b: Rect, degrees: Float, color: Color) {
    val radians = Math.toRadians(degrees.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val reach = max(b.width, b.height)
    val spacing = 1.5f
    var offset = -reach
    while (offset <= reach) {
        val px = b.center.x - dy * offset
        val py = b.center.y + dx * offset
        drawLine(color, Offset(px - dx * reach, py - dy * reach), Offset(px + dx * reach, py + dy * reach), strokeWidth = 0.28f, cap = StrokeCap.Round)
        offset += spacing
    }
}
