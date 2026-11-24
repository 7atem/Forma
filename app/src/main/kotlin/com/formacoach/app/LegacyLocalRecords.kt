package com.formacoach.app

import android.content.Context
import com.formacoach.model.*
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Earlier builds kept custom exercises and supplements as plain JSON in app preferences, outside the
 * encrypted database and outside backup. This reads that data once so it can be committed to the
 * database; the preference files are removed only after the database write succeeds.
 */
internal data class LegacyLocalRecords(
    val exercises: List<CustomExercise>,
    val sets: List<CustomExerciseSet>,
    val supplements: List<Supplement>,
    val intakes: List<SupplementIntake>,
)

internal object LegacyLocalStore {
    private const val CUSTOM_PREFS = "forma_custom_exercises_prefs"
    private const val SUPPLEMENT_PREFS = "forma_supplements_prefs"
    private const val KEY = "state_v1"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class OldExercise(val id: String, val name: String, val equipment: String, val primaryMuscle: String, val notes: String = "")
    @Serializable private data class OldSet(val id: String, val exerciseId: String, val date: String, val reps: Int, val weightKg: Double, val warmUp: Boolean = false)
    @Serializable private data class OldExerciseState(val exercises: List<OldExercise> = emptyList(), val sets: List<OldSet> = emptyList())
    @Serializable private data class OldSupplement(val id: String, val name: String, val dose: String = "")
    @Serializable private data class OldSupplementState(val items: List<OldSupplement> = emptyList(), val takenByDate: Map<String, List<String>> = emptyMap())

    /**
     * Returns null when there is nothing to move. Throws if stored data cannot be converted, so callers
     * leave the original preferences untouched rather than importing part of it.
     */
    fun read(context: Context): LegacyLocalRecords? {
        val customRaw = context.getSharedPreferences(CUSTOM_PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        val supplementRaw = context.getSharedPreferences(SUPPLEMENT_PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (customRaw == null && supplementRaw == null) return null
        val custom = customRaw?.let { json.decodeFromString<OldExerciseState>(it) } ?: OldExerciseState()
        val supplementState = supplementRaw?.let { json.decodeFromString<OldSupplementState>(it) } ?: OldSupplementState()
        val exercises = custom.exercises.map {
            CustomExercise(it.id, it.name.trim().take(60), Equipment.valueOf(it.equipment), Muscle.valueOf(it.primaryMuscle), it.notes.trim().take(120))
        }
        val exerciseIds = exercises.map { it.id }.toSet()
        // Older builds removed an exercise's sets together with it, so unreferenced sets have no exercise to show.
        val sets = custom.sets.filter { it.exerciseId in exerciseIds }.map {
            CustomExerciseSet(it.id, it.exerciseId, LocalDate.parse(it.date), it.reps, it.weightKg, it.warmUp)
        }
        val supplements = supplementState.items.map { Supplement(it.id, it.name.trim().take(60), it.dose.trim().take(30)) }
        val supplementIds = supplements.map { it.id }.toSet()
        val intakes = supplementState.takenByDate.flatMap { (date, ids) ->
            val day = LocalDate.parse(date)
            ids.distinct().filter { it in supplementIds }.map { SupplementIntake(day, it) }
        }
        return LegacyLocalRecords(exercises, sets, supplements, intakes)
    }

    /** Removes the plain-text copies after their contents are safely in the encrypted database. */
    fun clear(context: Context) {
        listOf(CUSTOM_PREFS, SUPPLEMENT_PREFS).forEach { context.deleteSharedPreferences(it) }
    }
}
