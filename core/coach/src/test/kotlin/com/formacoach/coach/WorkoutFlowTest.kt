package com.formacoach.coach

import com.formacoach.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutFlowTest {
    private val order=listOf(Exercise.CHEST_PRESS,Exercise.ROW,Exercise.SQUAT)
    private val details=listOf(ExercisePrescription(order[0],2,8,12,60,1,"Seat setting 3"),ExercisePrescription(order[1],2,8,12,90,1),ExercisePrescription(order[2],1))
    private val session=Session("workout",LocalDate.of(2026,11,4),"custom",plannedExercises=order,prescriptions=details)
    private fun set(exercise:Exercise,kind:SetKind=SetKind.WORKING)=TrainingSet(sessionId=session.id,exercise=exercise,reps=8,weightKg=10.0,kind=kind)
    @Test fun supersetsAlternateWithoutRestThenUseConfiguredRestBetweenRounds() {
        val sets=mutableListOf(set(order[0]))
        assertEquals(WorkoutFlow.Next(order[1],0,true),WorkoutFlow.afterSet(session,sets,order[0],SetKind.WORKING))
        sets.add(set(order[1]))
        assertEquals(WorkoutFlow.Next(order[0],90,true),WorkoutFlow.afterSet(session,sets,order[1],SetKind.WORKING))
        sets.add(set(order[0]));sets.add(set(order[1]))
        assertEquals(WorkoutFlow.Next(order[2],90),WorkoutFlow.afterSet(session,sets,order[1],SetKind.WORKING))
        sets.add(set(order[2]))
        assertEquals(WorkoutFlow.Next(null,0),WorkoutFlow.afterSet(session,sets,order[2],SetKind.WORKING))
    }
    @Test fun warmupsOtherSessionsAndUntargetedWorkDoNotCreateTargetCompletion() {
        val warm=set(order[0],SetKind.WARM_UP)
        val foreign=set(order[0]).copy(sessionId="another")
        assertTrue(WorkoutFlow.counts(session,listOf(warm,foreign)).isEmpty())
        assertEquals(WorkoutFlow.Next(order[0],60),WorkoutFlow.afterSet(session,listOf(warm),order[0],SetKind.WARM_UP))
        val unstructured=session.copy(prescriptions=emptyList())
        assertEquals(WorkoutFlow.Next(order[0],90),WorkoutFlow.afterSet(unstructured,List(10){set(order[0])},order[0],SetKind.WORKING))
    }
    @Test fun unequalTargetsSkipFinishedGroupMembersAndKeepWorkingOnTheRemainingMember() {
        val asymmetric=session.copy(prescriptions=details.map {if(it.exercise==order[0]) it.copy(workingSets=1) else it})
        val sets=listOf(set(order[0]),set(order[1]))
        assertEquals(WorkoutFlow.Next(order[1],90,true),WorkoutFlow.afterSet(asymmetric,sets,order[1],SetKind.WORKING))
    }
    @Test fun editingGroupsKeepsMetadataAndRejectsBrokenOrNonAdjacentGroups() {
        assertThrows(IllegalArgumentException::class.java) {TrainingProgram(name="Invalid",exercises=order,prescriptions=listOf(details.first()))}
        assertThrows(IllegalArgumentException::class.java) {session.copy(prescriptions=details.map {if(it.exercise==order[1]) it.copy(supersetGroup=null) else it})}
        val reordered=PlanEditing.normalize(listOf(order[0],order[2],order[1]),details)
        assertTrue(reordered.all {it.supersetGroup==null})
        assertEquals("Seat setting 3",reordered.first().notes)
        val unlinked=PlanEditing.linkPrevious(order,details,order[1],false)
        assertTrue(unlinked.all {it.supersetGroup==null})
        val linked=PlanEditing.linkPrevious(order,unlinked,order[2],true)
        assertEquals(linked[1].supersetGroup,linked[2].supersetGroup)
        assertNotNull(linked[1].supersetGroup)
    }
    @Test fun calendarRetainsLeapDayAndCountsOnlyRecordedCompletedNonFutureSessions() {
        val today=LocalDate.of(2028,2,29)
        val data=FitnessSnapshot(sessions=listOf(Session(date=today,templateId="A",completedAt=1),Session(date=today,templateId="B",completedAt=2),Session(date=today.minusDays(1),templateId="A"),Session(date=today.plusDays(1),templateId="B",completedAt=3)))
        val days=WorkoutCalendar.month(data,YearMonth.from(today),today)
        assertEquals(29,days.size);assertEquals(2,days.last().completed);assertEquals(0,days[27].completed)
    }
    @Test fun exerciseSearchAcceptsBothCatalogLanguagesAndArabicDiacritics() {
        val text=ExerciseCatalog.searchText(Exercise.DEAD_BUG)
        assertTrue(ExerciseSearch.matches("dead BUG",text))
        assertTrue(ExerciseSearch.matches("الحَشَرة المَيّتة",text))
        assertFalse(ExerciseSearch.matches("chest",text))
        assertTrue(ExerciseSearch.matches("",text))
    }
}
