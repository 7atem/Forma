package com.formacoach.data

import android.content.Context
import com.formacoach.model.*
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.Dispatchers

class FitnessRepository(daoProvider: () -> FitnessDao, private val clock: Clock = Clock.systemDefaultZone()) {
    companion object {
        fun open(context: Context): FitnessRepository = FitnessRepository({ FitnessDatabase.open(context).dao() })
    }
    private val dao by lazy(daoProvider)
    val snapshot = flow {
        emitAll(combine(dao.profile(), dao.sessions(), dao.sets(), dao.meals(), dao.measurements()) { profile, sessions, sets, meals, measurements ->
        FitnessSnapshot(
            profile?.let { Profile(it.name, Goal.valueOf(it.goal), it.sessionsPerWeek, it.caloriesTarget, it.proteinTarget) },
            sessions.map { Session(it.id, LocalDate.ofEpochDay(it.day), it.templateId, it.completedAt) },
            sets.map { TrainingSet(it.id, it.sessionId, Exercise.valueOf(it.exercise), it.reps, it.weightKg) },
            meals.map { Meal(it.id, LocalDate.ofEpochDay(it.day), it.name, it.calories, it.protein, it.carbs, it.fat) },
            measurements.map { Measurement(LocalDate.ofEpochDay(it.day), it.weightKg, it.waistCm) },
        )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun saveProfile(profile: Profile) = dao.saveProfile(ProfileRow(name=profile.name, goal=profile.goal.name, sessionsPerWeek=profile.sessionsPerWeek, caloriesTarget=profile.caloriesTarget, proteinTarget=profile.proteinTarget))

    suspend fun startSession(templateId: String): String {
        val session = Session(date=LocalDate.now(clock), templateId=templateId)
        dao.startSession(SessionRow(session.id, session.date.toEpochDay(), session.templateId, null))
        return session.id
    }

    suspend fun addSet(set: TrainingSet) = dao.addSet(SetRow(set.id, set.sessionId, set.exercise.name, set.reps, set.weightKg))
    suspend fun finishSession(id: String) { check(dao.finishSession(id, clock.millis()) == 1) }
    suspend fun removeSet(id: String) = dao.removeSet(id)
    suspend fun addMeal(meal: Meal) = dao.addMeal(MealRow(meal.id, meal.date.toEpochDay(), meal.name, meal.calories, meal.protein, meal.carbs, meal.fat))
    suspend fun removeMeal(id: String) = dao.removeMeal(id)
    suspend fun saveMeasurement(value: Measurement) = dao.saveMeasurement(MeasurementRow(value.date.toEpochDay(), value.weightKg, value.waistCm))
    suspend fun removeMeasurement(date: LocalDate) = dao.removeMeasurement(date.toEpochDay())
}
