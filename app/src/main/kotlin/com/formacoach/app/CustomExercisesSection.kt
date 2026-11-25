package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.*
import java.time.LocalDate

@Composable
internal fun CustomExercisesSection(app: AppState, vm: FitnessViewModel, today: LocalDate) {
    val unit = LocalWeightUnit.current
    // Stored as typed records in the encrypted database and included in backup.
    val exercises = app.snapshot.customExercises
    val allSets = app.snapshot.customSets
    var showCreate by remember { mutableStateOf(false) }
    var loggingExercise by remember { mutableStateOf<CustomExercise?>(null) }

    InfoCard(Modifier.testTag("custom_exercises_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.custom_exercises_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SectionInfoButton(InfoTopic.CUSTOM_EXERCISES)
            FilledTonalButton(
                onClick = { showCreate = true },
                enabled = !app.busy && exercises.size < MAX_CUSTOM_EXERCISES,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_custom_exercise_btn"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.custom_exercise_add), fontWeight = FontWeight.SemiBold)
            }
        }

        if (exercises.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEach { ex ->
                    val exSets = allSets.filter { it.exerciseId == ex.id }
                    val best = exSets.filterNot { it.warmUp }.maxByOrNull { it.weightKg }
                    val muscleEnum: Muscle? = ex.primaryMuscle
                    val equipEnum: Equipment? = ex.equipment
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val meta = listOfNotNull(
                                    muscleEnum?.let { stringResource(muscleLabel(it)) },
                                    equipEnum?.let { stringResource(equipmentLabel(it)) },
                                ).joinToString(" · ")
                                if (meta.isNotEmpty()) {
                                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (best != null) {
                                    Text(
                                        "${stringResource(R.string.custom_exercise_best)}: ${formatWeight(best.weightKg, 1)} × ${best.reps}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { loggingExercise = ex },
                                enabled = !app.busy,
                                modifier = Modifier.testTag("log_custom_${ex.id}"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text(stringResource(R.string.add_set)) }
                            ConfirmButton(
                                R.string.remove,
                                stringResource(R.string.custom_exercise_delete_note, number(exSets.size.toDouble(), 0)),
                                !app.busy,
                                tag = "remove_custom_${ex.id}",
                            ) { vm.deleteCustomExercise(ex.id) }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var selectedEquipment by remember { mutableStateOf(Equipment.DUMBBELLS) }
        var selectedMuscle by remember { mutableStateOf(Muscle.CHEST) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.custom_exercise_add), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.custom_exercise_name)) },
                        modifier = Modifier.testTag("custom_exercise_name"),
                        singleLine = true,
                    )
                    Text(stringResource(R.string.custom_exercise_muscle), style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Muscle.entries.forEach { m ->
                            FilterChip(
                                selected = selectedMuscle == m,
                                onClick = { selectedMuscle = m },
                                label = { Text(stringResource(muscleLabel(m))) },
                            )
                        }
                    }
                    Text(stringResource(R.string.custom_exercise_equipment), style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Equipment.entries.forEach { eq ->
                            FilterChip(
                                selected = selectedEquipment == eq,
                                onClick = { selectedEquipment = eq },
                                label = { Text(stringResource(equipmentLabel(eq))) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.custom_exercise_notes)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.trim().isNotEmpty() && !app.busy,
                    onClick = {
                        vm.saveCustomExercise(
                            CustomExercise(
                                name = name.trim().take(60),
                                equipment = selectedEquipment,
                                primaryMuscle = selectedMuscle,
                                notes = notes.trim().take(120),
                            ),
                        ) { showCreate = false }
                    },
                    modifier = Modifier.testTag("save_custom_exercise"),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    loggingExercise?.let { target ->
        var repsText by remember(target.id) { mutableStateOf("10") }
        var weightText by remember(target.id) { mutableStateOf("") }
        var warmUp by remember(target.id) { mutableStateOf(false) }
        val recentSets = allSets.filter { it.exerciseId == target.id }.takeLast(5).reversed()
        AlertDialog(
            onDismissRequest = { loggingExercise = null },
            title = { Text(target.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text(stringResource(R.string.reps)) },
                        modifier = Modifier.testTag("custom_set_reps"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text(stringResource(if (unit == WeightUnit.KG) R.string.weight_kg else R.string.weight_lb)) },
                        modifier = Modifier.testTag("custom_set_weight"),
                        singleLine = true,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = warmUp, onCheckedChange = { warmUp = it })
                        Text(stringResource(R.string.warmup_set))
                    }
                    if (recentSets.isNotEmpty()) {
                        HorizontalDivider()
                        recentSets.forEach { s ->
                            Text(
                                "${dateLabel(s.date)} · ${formatWeight(s.weightKg, 1)} × ${s.reps}" + if (s.warmUp) " (${stringResource(R.string.warmup_set)})" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val parsedReps = NumericInput.integer(repsText)?.takeIf { it in 1..1000 }
                val parsedWeight = if (weightText.isBlank()) 0.0 else NumericInput.decimal(weightText)?.let { WeightUnits.toKg(it, unit) }?.takeIf { it in 0.0..1000.0 }
                TextButton(
                    enabled = parsedReps != null && parsedWeight != null && !app.busy,
                    onClick = {
                        if (parsedReps != null && parsedWeight != null) {
                            vm.addCustomSet(
                                CustomExerciseSet(
                                    exerciseId = target.id,
                                    date = today,
                                    reps = parsedReps,
                                    weightKg = parsedWeight,
                                    warmUp = warmUp,
                                ),
                            ) { loggingExercise = null }
                        }
                    },
                    modifier = Modifier.testTag("save_custom_set"),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { loggingExercise = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Keeps the library list manageable; nothing already saved is ever trimmed. */
private const val MAX_CUSTOM_EXERCISES = 100
