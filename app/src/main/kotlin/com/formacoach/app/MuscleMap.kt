package com.formacoach.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.formacoach.model.*

/** Original schematic geometry. Text labels communicate the same information without color. */
@Composable
internal fun MuscleMap(exercise:Exercise) {
    val activation=ExerciseMuscles.of(exercise)
    val primary=activation.primary.map {stringResource(muscleLabel(it))}.joinToString()
    val secondary=activation.secondary.map {stringResource(muscleLabel(it))}.joinToString().ifBlank {stringResource(R.string.no_supporting_mapped)}
    val description=stringResource(R.string.muscle_accessibility,stringResource(exerciseLabel(exercise)),primary,secondary)
    val outline=remember {checkNotNull(PathParser.createPathFromPathData(ExerciseMuscles.outline)).asComposePath()}
    val regions=remember {ExerciseMuscles.regions.map {it to checkNotNull(PathParser.createPathFromPathData(it.path)).asComposePath()}}
    val neutral=MaterialTheme.colorScheme.surfaceVariant
    val inactive=MaterialTheme.colorScheme.outline.copy(alpha=0.25f)
    val main=MaterialTheme.colorScheme.primary
    val supporting=MaterialTheme.colorScheme.secondary
    Column(Modifier.fillMaxWidth().testTag("muscle_map_${exercise.name}"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.muscles_working),style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(true,false).forEach {front -> Column(Modifier.weight(1f)) {
                Text(stringResource(if(front) R.string.body_front else R.string.body_back))
                Canvas(Modifier.fillMaxWidth().height(235.dp).semantics {contentDescription=description}) {
                    val factor=minOf(size.width/100f,size.height/220f)
                    translate((size.width-100f*factor)/2f,0f) {scale(factor,factor,pivot=androidx.compose.ui.geometry.Offset.Zero) {
                        drawPath(outline,neutral)
                        regions.filter {it.first.front==front}.forEach {(region,path) ->
                            val color=when(region.muscle) {in activation.primary->main;in activation.secondary->supporting;else->inactive}
                            drawPath(path,color)
                            if(region.muscle in activation.secondary) drawPath(path,neutral,style=Stroke(width=1.3f))
                        }
                    }}
                }
            }}
        }
        Text(stringResource(R.string.muscle_primary,primary),color=main)
        if(activation.secondary.isNotEmpty()) Text(stringResource(R.string.muscle_secondary,secondary),color=supporting)
        Text(stringResource(R.string.muscle_note),style=MaterialTheme.typography.bodySmall)
    }
}

/** 7-day descriptive muscle heat map colored purely by recorded completed working sets. */
@Composable
internal fun WeeklyMuscleHeatMap(primarySetsByMuscle: Map<Muscle, Int>) {
    val outline = remember { checkNotNull(PathParser.createPathFromPathData(ExerciseMuscles.outline)).asComposePath() }
    val regions = remember { ExerciseMuscles.regions.map { it to checkNotNull(PathParser.createPathFromPathData(it.path)).asComposePath() } }
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    val untrained = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val light = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
    val moderate = MaterialTheme.colorScheme.primary
    val high = MaterialTheme.colorScheme.secondary
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(stringResource(R.string.muscle_heatmap_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            ScopeInfoButton(R.string.muscle_heatmap_title, R.string.muscle_heatmap_note)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(true, false).forEach { front ->
                Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(stringResource(if (front) R.string.body_front else R.string.body_back), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Canvas(Modifier.fillMaxWidth().height(210.dp)) {
                        val factor = minOf(size.width / 100f, size.height / 220f)
                        translate((size.width - 100f * factor) / 2f, 0f) {
                            scale(factor, factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                                drawPath(outline, neutral)
                                regions.filter { it.first.front == front }.forEach { (region, path) ->
                                    val sets = primarySetsByMuscle[region.muscle] ?: 0
                                    val color = when {
                                        sets >= 8 -> high
                                        sets >= 4 -> moderate
                                        sets >= 1 -> light
                                        else -> untrained
                                    }
                                    drawPath(path, color)
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillBadge(stringResource(R.string.heatmap_light), containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.tertiary)
            PillBadge(stringResource(R.string.heatmap_moderate), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
            PillBadge(stringResource(R.string.heatmap_high), containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** Compact floating front/back schematic overlay for exercise demos & cards showing marked primary/supporting muscles. */
@Composable
internal fun CompactMuscleOverlay(exercise: Exercise, pulsePeak: Boolean = false, modifier: Modifier = Modifier) {
    val activation = ExerciseMuscles.of(exercise)
    val outline = remember { checkNotNull(PathParser.createPathFromPathData(ExerciseMuscles.outline)).asComposePath() }
    val regions = remember { ExerciseMuscles.regions.map { it to checkNotNull(PathParser.createPathFromPathData(it.path)).asComposePath() } }
    val neutral = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val inactive = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val primaryAlpha = if (pulsePeak) 1f else 0.78f
    val main = MaterialTheme.colorScheme.primary.copy(alpha = primaryAlpha)
    val supporting = MaterialTheme.colorScheme.secondary.copy(alpha = if (pulsePeak) 0.95f else 0.72f)
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (pulsePeak) 0.65f else 0.3f)),
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(true, false).forEach { front ->
                Canvas(Modifier.size(width = 30.dp, height = 64.dp)) {
                    val factor = minOf(size.width / 100f, size.height / 220f)
                    translate((size.width - 100f * factor) / 2f, 0f) {
                        scale(factor, factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                            drawPath(outline, neutral)
                            regions.filter { it.first.front == front }.forEach { (region, path) ->
                                val color = when (region.muscle) {
                                    in activation.primary -> main
                                    in activation.secondary -> supporting
                                    else -> inactive
                                }
                                drawPath(path, color)
                            }
                        }
                    }
                }
            }
        }
    }
}
