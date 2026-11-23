package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MuscleTrainingTest {
    private val day=LocalDate.of(2026,11,3)
    @Test fun everyExerciseHasMappedPrimaryAndSeparateSupportingMusclesWithVisibleRegions() {
        Exercise.entries.forEach {exercise ->
            val mapping=ExerciseMuscles.of(exercise)
            assertTrue(mapping.primary.isNotEmpty())
            assertEquals(mapping.primary.size,mapping.primary.distinct().size)
            assertTrue(mapping.primary.toSet().intersect(mapping.secondary.toSet()).isEmpty())
            assertTrue(mapping.reference.startsWith("https://"))
            (mapping.primary+mapping.secondary).forEach {muscle ->assertTrue(ExerciseMuscles.regions.any {it.muscle==muscle})}
        }
        assertTrue(ExerciseMuscles.regions.any {it.front})
        assertTrue(ExerciseMuscles.regions.any {!it.front})
        assertEquals(setOf(Muscle.QUADRICEPS,Muscle.GLUTES),ExerciseMuscles.of(Exercise.SQUAT).primary.toSet())
        assertEquals(setOf(Muscle.CHEST),ExerciseMuscles.of(Exercise.CHEST_PRESS).primary.toSet())
    }
    @Test fun detailedRegionsRollUpExactlyToBroadGroupsAndAreAllDrawn() {
        assertEquals(20,MuscleDetail.entries.size)
        MuscleDetail.entries.forEach {detail ->assertTrue(ExerciseMuscles.regions.any {it.detail==detail})}
        // Each drawn region appears once per side.
        MuscleDetail.entries.forEach {detail ->
            listOf(true,false).forEach {front ->
                val sides=ExerciseMuscles.regions.count {it.detail==detail&&it.front==front}
                assertTrue(sides==0||sides==2)
            }
        }
        Exercise.entries.forEach {exercise ->
            val mapping=ExerciseMuscles.of(exercise)
            assertTrue(mapping.primaryDetail.isNotEmpty())
            assertTrue(mapping.primaryDetail.toSet().intersect(mapping.secondaryDetail.toSet()).isEmpty())
            assertEquals(mapping.primary.toSet(),mapping.primaryDetail.map {it.group}.toSet())
            assertEquals(mapping.secondary.toSet(),mapping.secondaryDetail.map {it.group}.toSet()-mapping.primary.toSet())
        }
        assertEquals(listOf(MuscleDetail.UPPER_CHEST),ExerciseMuscles.of(Exercise.INCLINE_BARBELL_PRESS).primaryDetail)
        assertEquals(listOf(MuscleDetail.MID_LOWER_CHEST),ExerciseMuscles.of(Exercise.BARBELL_BENCH_PRESS).primaryDetail)
        assertEquals(listOf(MuscleDetail.SIDE_DELT),ExerciseMuscles.of(Exercise.LATERAL_RAISE).primaryDetail)
        assertEquals(listOf(MuscleDetail.SOLEUS),ExerciseMuscles.of(Exercise.SEATED_CALF_RAISE).primaryDetail)
    }
    @Test fun weeklyCountsExcludeWarmupDraftsFutureAndOutsideWindowWithoutAddingSecondaryVolume() {
        val sessions=listOf(Session("now",day,"A",1),Session("old",day.minusDays(7),"A",1),Session("draft",day,"A"),Session("future",day.plusDays(1),"A",1))
        val sets=sessions.map {TrainingSet(sessionId=it.id,exercise=Exercise.ROW,reps=8,weightKg=20.0)}+
            TrainingSet(sessionId="now",exercise=Exercise.ROW,reps=8,weightKg=200.0,kind=SetKind.WARM_UP)
        val data=FitnessSnapshot(sessions=sessions,sets=sets)
        val counts=MuscleTraining.weekly(data,day).associateBy {it.muscle}
        assertEquals(1,counts.getValue(Muscle.LATS).primarySets)
        assertEquals(0,counts.getValue(Muscle.LATS).secondarySets)
        assertEquals(1,counts.getValue(Muscle.BICEPS).secondarySets)
        assertEquals(20.0,MuscleTraining.best(data,Exercise.ROW,day)!!.weightKg,0.0)
        assertEquals(20.0,TrainingHistory.loads(data,Exercise.ROW,day).last().value,0.0)
    }
}
