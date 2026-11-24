package com.formacoach.app

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

@Serializable
internal data class CustomExerciseEntry(
    val id: String,
    val name: String,
    val equipment: String,
    val primaryMuscle: String,
    val notes: String = "",
)

@Serializable
internal data class CustomExerciseSet(
    val id: String,
    val exerciseId: String,
    val date: String,
    val reps: Int,
    val weightKg: Double,
    val warmUp: Boolean = false,
)

@Serializable
internal data class CustomExercisesState(
    val exercises: List<CustomExerciseEntry> = emptyList(),
    val sets: List<CustomExerciseSet> = emptyList(),
)

internal object CustomExercisesStore {
    private const val PREFS = "forma_custom_exercises_prefs"
    private const val KEY = "state_v1"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): CustomExercisesState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return CustomExercisesState()
        return runCatching { json.decodeFromString<CustomExercisesState>(raw) }.getOrDefault(CustomExercisesState())
    }

    fun save(context: Context, state: CustomExercisesState) {
        val clean = state.copy(
            exercises = state.exercises.take(100),
            sets = state.sets.takeLast(2000),
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.encodeToString(clean))
            .apply()
    }
}

@Composable
internal fun CustomExercisesSection(today: LocalDate) {
    val context = LocalContext.current
    val unit = LocalWeightUnit.current
    var state by remember { mutableStateOf(CustomExercisesStore.load(context)) }
    var showCreate by remember { mutableStateOf(false) }
    var loggingExercise by remember { mutableStateOf<CustomExerciseEntry?>(null) }

    fun update(next: CustomExercisesState) {
        state = next
        CustomExercisesStore.save(context, next)
    }

    InfoCard(Modifier.testTag("custom_exercises_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.custom_exercises_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalButton(
                onClick = { showCreate = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_custom_exercise_btn"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.custom_exercise_add), fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.exercises.isEmpty()) {
            Text(
                stringResource(R.string.custom_exercises_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.exercises.forEach { ex ->
                    val exSets = state.sets.filter { it.exerciseId == ex.id }
                    val best = exSets.filterNot { it.warmUp }.maxByOrNull { it.weightKg }
                    val muscleEnum = runCatching { Muscle.valueOf(ex.primaryMuscle) }.getOrNull()
                    val equipEnum = runCatching { Equipment.valueOf(ex.equipment) }.getOrNull()
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
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text(stringResource(R.string.add_set)) }
                            IconButton(
                                onClick = {
                                    update(
                                        state.copy(
                                            exercises = state.exercises.filterNot { it.id == ex.id },
                                            sets = state.sets.filterNot { it.exerciseId == ex.id },
                                        ),
                                    )
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.error)
                            }
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
                    enabled = name.trim().isNotEmpty(),
                    onClick = {
                        update(
                            state.copy(
                                exercises = state.exercises + CustomExerciseEntry(
                                    id = UUID.randomUUID().toString(),
                                    name = name.trim().take(60),
                                    equipment = selectedEquipment.name,
                                    primaryMuscle = selectedMuscle.name,
                                    notes = notes.trim().take(120),
                                ),
                            ),
                        )
                        showCreate = false
                    },
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
        val recentSets = state.sets.filter { it.exerciseId == target.id }.takeLast(5).reversed()
        AlertDialog(
            onDismissRequest = { loggingExercise = null },
            title = { Text(target.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text(stringResource(R.string.reps)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text(stringResource(if (unit == WeightUnit.KG) R.string.weight_kg else R.string.weight_lb)) },
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
                                "${s.date} · ${formatWeight(s.weightKg, 1)} × ${s.reps}" + if (s.warmUp) " (${stringResource(R.string.warmup_set)})" else "",
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
                    enabled = parsedReps != null && parsedWeight != null,
                    onClick = {
                        if (parsedReps != null && parsedWeight != null) {
                            update(
                                state.copy(
                                    sets = state.sets + CustomExerciseSet(
                                        id = UUID.randomUUID().toString(),
                                        exerciseId = target.id,
                                        date = today.toString(),
                                        reps = parsedReps,
                                        weightKg = parsedWeight,
                                        warmUp = warmUp,
                                    ),
                                ),
                            )
                            loggingExercise = null
                        }
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { loggingExercise = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
