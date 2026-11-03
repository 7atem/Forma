package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate

data class MuscleSetCount(val muscle:Muscle,val primarySets:Int,val secondarySets:Int)
data class RecordedBest(val weightKg:Double,val repsAtWeight:Int,val date:LocalDate)

object MuscleTraining {
    fun weekly(data:FitnessSnapshot,asOf:LocalDate):List<MuscleSetCount> {
        val sessions=data.sessions.filter {it.completedAt!=null && it.date in asOf.minusDays(6)..asOf}.map {it.id}.toSet()
        val sets=data.sets.filter {it.sessionId in sessions && it.kind==SetKind.WORKING}
        return Muscle.entries.map {muscle -> MuscleSetCount(muscle,
            sets.count {muscle in ExerciseMuscles.of(it.exercise).primary},
            sets.count {muscle in ExerciseMuscles.of(it.exercise).secondary})}
    }
    fun best(data:FitnessSnapshot,exercise:Exercise,asOf:LocalDate):RecordedBest? {
        val sessions=data.sessions.filter {it.completedAt!=null && it.date<=asOf}.associateBy {it.id}
        val set=data.sets.filter {it.exercise==exercise && it.kind==SetKind.WORKING && it.sessionId in sessions}
            .maxWithOrNull(compareBy<TrainingSet> {it.weightKg}.thenBy {it.reps})?:return null
        return RecordedBest(set.weightKg,set.reps,sessions.getValue(set.sessionId).date)
    }
}
