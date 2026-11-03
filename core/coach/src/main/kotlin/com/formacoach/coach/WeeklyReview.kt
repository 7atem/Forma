package com.formacoach.coach

import com.formacoach.model.FitnessSnapshot
import java.time.Clock
import java.time.LocalDate

enum class NextAction { LOG_FIRST_SESSION, KEEP_TRAINING_LOG, ADD_MEAL_CONTEXT, ADD_MEASUREMENTS, REVIEW_WITH_CONTEXT }

data class WeeklyReview(
    val start: LocalDate,
    val end: LocalDate,
    val completedSessions: Int,
    val targetSessions: Int?,
    val mealDays: Int,
    val meanLoggedCalories: Double?,
    val meanWeight: Double?,
    val weightObservations: Int,
    val previousWeightObservations: Int,
    val weightDelta: Double?,
    val meanWaist: Double?,
    val sessionEvidence: List<String>,
    val mealEvidence: List<String>,
    val measurementEvidence: List<LocalDate>,
    val comparisonMeasurementEvidence: List<LocalDate>,
    val nextAction: NextAction,
)

/** Descriptive reporting only. No inferred compliance, causes, prescriptions or silent writes. */
class WeeklyReviewEngine(private val clock: Clock = Clock.systemDefaultZone()) {
    fun review(snapshot: FitnessSnapshot): WeeklyReview {
        val today = LocalDate.now(clock)
        val start = today.minusDays(6)
        val previousStart = start.minusDays(7)
        val sessions = snapshot.sessions.filter { it.completedAt != null && it.date in start..today }
        val meals = snapshot.meals.filter { it.date in start..today }
        val measures = snapshot.measurements.filter { it.date in start..today }
        val previous = snapshot.measurements.filter { it.date >= previousStart && it.date < start }
        val weights = measures.mapNotNull { it.weightKg }
        val previousWeights = previous.mapNotNull { it.weightKg }
        val mealDays = meals.map { it.date }.distinct().size
        // A coverage gate for displaying a comparison; not a clinical trend model.
        val delta = if (weights.size >= 3 && previousWeights.size >= 3) {
            weights.average() - previousWeights.average()
        } else null
        return WeeklyReview(
            start, today, sessions.size, snapshot.profile?.sessionsPerWeek, mealDays,
            if (mealDays == 0) null else meals.sumOf { it.calories } / mealDays,
            weights.takeIf { it.isNotEmpty() }?.average(), weights.size, previousWeights.size, delta,
            measures.mapNotNull { it.waistCm }.takeIf { it.isNotEmpty() }?.average(),
            sessions.map { it.id }, meals.map { it.id }, measures.map { it.date },
            previous.filter { it.weightKg != null }.map { it.date },
            when {
                sessions.isEmpty() -> NextAction.LOG_FIRST_SESSION
                mealDays < 3 -> NextAction.ADD_MEAL_CONTEXT
                weights.size < 3 -> NextAction.ADD_MEASUREMENTS
                sessions.size < (snapshot.profile?.sessionsPerWeek ?: 2) -> NextAction.KEEP_TRAINING_LOG
                else -> NextAction.REVIEW_WITH_CONTEXT
            },
        )
    }
}
