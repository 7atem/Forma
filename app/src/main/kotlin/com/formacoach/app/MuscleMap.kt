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
    val secondary=activation.secondary.map {stringResource(muscleLabel(it))}.joinToString()
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
        Text(stringResource(R.string.muscle_secondary,secondary),color=supporting)
        Text(stringResource(R.string.muscle_note),style=MaterialTheme.typography.bodySmall)
    }
}
