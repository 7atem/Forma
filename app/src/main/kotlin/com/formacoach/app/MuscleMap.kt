package com.formacoach.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.formacoach.model.*

// Follows the app's own theme switch, not the system setting.
@Composable
internal fun primaryMuscleColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFF8B73) else Color(0xFFC0473A)

@Composable
internal fun supportingMuscleColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF7CC7FF) else Color(0xFF2A74AB)

/** Detailed region names (e.g. "Upper chest") for legends and screen readers. */
@Composable
private fun detailNames(details: List<MuscleDetail>): String = details.map { stringResource(muscleDetailLabel(it)) }.joinToString()

@Composable
private fun MuscleLegendRow(color: Color, text: String, dashed: Boolean = false) {
    val ring = MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(12.dp)) {
            drawCircle(color)
            if (dashed) drawCircle(ring, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 2f))))
        }
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Always-visible text equivalent for the demo overlay; meaning never depends on color alone. */
@Composable
internal fun MuscleDemoLegend(exercise: Exercise) {
    val activation = ExerciseMuscles.of(exercise)
    val primary = detailNames(activation.primaryDetail)
    val secondary = detailNames(activation.secondaryDetail)
    Column(Modifier.fillMaxWidth().testTag("demo_muscle_legend_${exercise.name}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MuscleLegendRow(primaryMuscleColor(), stringResource(R.string.muscle_primary, primary))
        if (secondary.isNotBlank()) MuscleLegendRow(supportingMuscleColor(), stringResource(R.string.muscle_secondary, secondary), dashed = true)
    }
}

private fun roleOf(region: MuscleRegion, activation: MuscleActivation): MuscleRole = when (region.detail) {
    in activation.primaryDetail -> MuscleRole.PRIMARY
    in activation.secondaryDetail -> MuscleRole.SUPPORTING
    else -> MuscleRole.OTHER
}

/** Original shaded schematic. Text labels communicate the same information without color. */
@Composable
internal fun MuscleMap(exercise: Exercise) {
    val activation = ExerciseMuscles.of(exercise)
    val primary = detailNames(activation.primaryDetail)
    val secondary = detailNames(activation.secondaryDetail).ifBlank { stringResource(R.string.no_supporting_mapped) }
    val description = stringResource(R.string.muscle_accessibility, stringResource(exerciseLabel(exercise)), primary, secondary)
    val palette = musclePalette()
    val main = primaryMuscleColor()
    val supporting = supportingMuscleColor()
    Column(Modifier.fillMaxWidth().testTag("muscle_map_${exercise.name}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.muscles_working), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().semantics { contentDescription = description }, horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(true, false).forEach { front ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(if (front) R.string.body_front else R.string.body_back), style = MaterialTheme.typography.labelMedium)
                    Canvas(Modifier.fillMaxWidth().height(280.dp)) {
                        drawMuscleBody(front, palette, fibres = true) { region ->
                            when (roleOf(region, activation)) {
                                MuscleRole.PRIMARY -> main to MuscleRole.PRIMARY
                                MuscleRole.SUPPORTING -> supporting to MuscleRole.SUPPORTING
                                MuscleRole.OTHER -> null
                            }
                        }
                    }
                }
            }
        }
        MuscleLegendRow(main, stringResource(R.string.muscle_primary, primary))
        if (activation.secondaryDetail.isNotEmpty()) MuscleLegendRow(supporting, stringResource(R.string.muscle_secondary, secondary), dashed = true)
        Text(stringResource(R.string.muscle_note), style = MaterialTheme.typography.bodySmall)
    }
}

/** 7-day descriptive muscle heat map colored purely by recorded completed working sets per broad group. */
@Composable
internal fun WeeklyMuscleHeatMap(primarySetsByMuscle: Map<Muscle, Int>) {
    val palette = musclePalette()
    val light = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
    val moderate = MaterialTheme.colorScheme.primary
    val high = MaterialTheme.colorScheme.secondary
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.muscle_heatmap_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            ScopeInfoButton(R.string.muscle_heatmap_title, R.string.muscle_heatmap_note)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(true, false).forEach { front ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(if (front) R.string.body_front else R.string.body_back), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Canvas(Modifier.fillMaxWidth().height(230.dp)) {
                        drawMuscleBody(front, palette, fibres = true) { region ->
                            val sets = primarySetsByMuscle[region.muscle] ?: 0
                            when {
                                sets >= 8 -> high to MuscleRole.PRIMARY
                                sets >= 4 -> moderate to MuscleRole.PRIMARY
                                sets >= 1 -> light to MuscleRole.PRIMARY
                                else -> null
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillBadge(stringResource(R.string.heatmap_light), containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.tertiary)
            PillBadge(stringResource(R.string.heatmap_moderate), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
            PillBadge(stringResource(R.string.heatmap_high), containerColor = MaterialTheme.colorScheme.accentContainer, contentColor = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Compact floating front/back overlay for exercise demos showing marked primary/supporting regions. */
@Composable
internal fun CompactMuscleOverlay(exercise: Exercise, pulsePeak: Boolean = false, modifier: Modifier = Modifier) {
    val activation = ExerciseMuscles.of(exercise)
    val primary = detailNames(activation.primaryDetail)
    val secondary = detailNames(activation.secondaryDetail).ifBlank { stringResource(R.string.no_supporting_mapped) }
    val description = stringResource(R.string.muscle_accessibility, stringResource(exerciseLabel(exercise)), primary, secondary)
    val palette = musclePalette()
    val main = primaryMuscleColor().copy(alpha = if (pulsePeak) 1f else 0.82f)
    val supporting = supportingMuscleColor().copy(alpha = if (pulsePeak) 0.95f else 0.75f)
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, main.copy(alpha = if (pulsePeak) 0.65f else 0.3f)),
        modifier = modifier.testTag("compact_muscle_overlay_${exercise.name}").semantics { contentDescription = description },
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(true, false).forEach { front ->
                Canvas(Modifier.size(width = 34.dp, height = 72.dp)) {
                    // Fibres are omitted at this size; shading alone keeps the small figures readable.
                    drawMuscleBody(front, palette, fibres = false) { region ->
                        when (roleOf(region, activation)) {
                            MuscleRole.PRIMARY -> main to MuscleRole.PRIMARY
                            MuscleRole.SUPPORTING -> supporting to MuscleRole.SUPPORTING
                            MuscleRole.OTHER -> null
                        }
                    }
                }
            }
        }
    }
}
