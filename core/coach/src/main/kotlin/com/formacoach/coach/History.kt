package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate

data class HistoryPoint(val date: LocalDate, val value: Double)

object TrainingHistory {
    fun loads(data: FitnessSnapshot, exercise: Exercise, asOf: LocalDate): List<HistoryPoint> {
        val sessions=data.sessions.filter {it.completedAt!=null && it.date<=asOf}.associateBy {it.id}
        return data.sets.filter {it.exercise==exercise && it.kind==SetKind.WORKING && it.sessionId in sessions}
            .groupBy {sessions.getValue(it.sessionId).date}.map {(date,sets) -> HistoryPoint(date,sets.maxOf {it.weightKg})}
            .sortedBy {it.date}
    }
    fun weights(data: FitnessSnapshot, asOf: LocalDate): List<HistoryPoint> = data.measurements
        .filter {it.date in asOf.minusDays(89)..asOf && it.weightKg!=null}
        .sortedBy {it.date}.map {HistoryPoint(it.date,it.weightKg!!)}
}

object RestTimer {
    fun remainingSeconds(deadlineElapsedMillis: Long, nowElapsedMillis: Long, durationSeconds: Int): Long {
        if(deadlineElapsedMillis<=nowElapsedMillis || durationSeconds !in 15..600) return 0
        val delta=deadlineElapsedMillis-nowElapsedMillis
        // Monotonic time resets on reboot; a saved deadline must never become a multi-day timer.
        if(delta<0 || delta>durationSeconds*1000L) return 0
        return (delta+999)/1000
    }
}

enum class CoachQuestion { WEEK, TRAINING, FOOD, WEIGHT, UNSUPPORTED }

object CoachQuestions {
    fun resolve(text: String): CoachQuestion {
        val value=text.lowercase().trim().take(500)
        return when {
            listOf("weight","وزن").any(value::contains) -> CoachQuestion.WEIGHT
            listOf("food","calor","protein","أكل","اكل","سعر","بروتين").any(value::contains) -> CoachQuestion.FOOD
            listOf("train","workout","تمرين","تمار").any(value::contains) -> CoachQuestion.TRAINING
            listOf("week","progress","أسبوع","اسبوع","تقدم").any(value::contains) -> CoachQuestion.WEEK
            else -> CoachQuestion.UNSUPPORTED
        }
    }
}
