package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DailyWorkflowTest {
    private val date=LocalDate.of(2026,11,3)
    private val food=Food(name="Label food",referenceAmount=100.0,unit=NutritionUnit.GRAMS,
        nutrients=Nutrients(200.0,10.0,20.0,8.0),source="User label")

    @Test fun `grams scale all nutrients without changing the source food`() {
        val meal=food.portion(150.0,date)
        assertEquals(300.0,meal.calories,0.0001)
        assertEquals(15.0,meal.protein,0.0001)
        assertEquals(150.0,meal.quantity!!,0.0001)
        assertEquals("User label",meal.source)
        assertEquals(200.0,food.nutrients.calories,0.0001)
    }
    @Test fun `recipe totals divide by yield and retain immutable ingredient evidence`() {
        val recipe=Food.recipe("Recipe",4.0,"My recipe",listOf(food.ingredient(150.0),food.ingredient(50.0)))
        val meal=recipe.portion(1.0,date)
        assertEquals(100.0,meal.calories,0.0001)
        assertEquals(2,recipe.ingredients.size)
        assertEquals(300.0,recipe.ingredients.first().nutrients.calories,0.0001)
        assertEquals(NutritionUnit.SERVING,meal.unit)
    }
    @Test fun `recipe cannot claim nutrition different from its ingredients`() {
        assertThrows(IllegalArgumentException::class.java) {
            Food(name="bad",referenceAmount=1.0,unit=NutritionUnit.SERVING,nutrients=Nutrients(500.0,0.0,0.0,0.0),source="x",sourceKind=NutritionSource.RECIPE,ingredients=listOf(food.ingredient(100.0)))
        }
    }
    @Test fun `load history excludes drafts and uses actual day spacing`() {
        val sessions=listOf(Session("a",date.minusDays(4),"A",1),Session("b",date,"A",1),Session("draft",date,"B"))
        val sets=listOf(TrainingSet(sessionId="a",exercise=Exercise.ROW,reps=8,weightKg=10.0),TrainingSet(sessionId="b",exercise=Exercise.ROW,reps=6,weightKg=20.0),TrainingSet(sessionId="draft",exercise=Exercise.ROW,reps=8,weightKg=100.0))
        val points=TrainingHistory.loads(FitnessSnapshot(sessions=sessions,sets=sets),Exercise.ROW,date)
        assertEquals(listOf(10.0,20.0),points.map {it.value})
        assertEquals(4,date.toEpochDay()-points.first().date.toEpochDay())
    }
    @Test fun `timer handles elapsed time expiry and reboot`() {
        assertEquals(30L,RestTimer.remainingSeconds(31000,1000,30))
        assertEquals(1L,RestTimer.remainingSeconds(31000,30999,30))
        assertEquals(0L,RestTimer.remainingSeconds(31000,31000,30))
        assertEquals(0L,RestTimer.remainingSeconds(900000,1000,30))
    }
    @Test fun `coach routes bilingual record questions and admits unsupported requests`() {
        assertEquals(CoachQuestion.WEIGHT,CoachQuestions.resolve("ليه وزني ثابت؟"))
        assertEquals(CoachQuestion.FOOD,CoachQuestions.resolve("Show calories"))
        assertEquals(CoachQuestion.UNSUPPORTED,CoachQuestions.resolve("Diagnose me"))
    }
    @Test fun `production clock follows timezone changes without recreation`() {
        val previous=java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Kiritimati"))
            assertEquals(java.time.ZoneId.of("Pacific/Kiritimati"),DeviceClock.zone)
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(java.time.ZoneId.of("America/Los_Angeles"),DeviceClock.zone)
        } finally {java.util.TimeZone.setDefault(previous)}
    }

    @Test fun `e1rm and tonnage exclude warmups drafts and high rep sets`() {
        val sessions = listOf(
            Session("s1", date.minusDays(2), "A", completedAt = 100L),
            Session("s2", date.minusDays(10), "A", completedAt = 100L),
            Session("draft", date, "A", completedAt = null),
        )
        val sets = listOf(
            TrainingSet(sessionId = "s1", exercise = Exercise.BARBELL_BACK_SQUAT, reps = 10, weightKg = 60.0, kind = SetKind.WARM_UP),
            TrainingSet(sessionId = "s1", exercise = Exercise.BARBELL_BACK_SQUAT, reps = 5, weightKg = 100.0, kind = SetKind.WORKING),
            TrainingSet(sessionId = "s1", exercise = Exercise.BARBELL_BACK_SQUAT, reps = 20, weightKg = 40.0, kind = SetKind.WORKING),
            TrainingSet(sessionId = "s2", exercise = Exercise.BARBELL_BACK_SQUAT, reps = 1, weightKg = 110.0, kind = SetKind.WORKING),
            TrainingSet(sessionId = "draft", exercise = Exercise.BARBELL_BACK_SQUAT, reps = 5, weightKg = 200.0, kind = SetKind.WORKING),
        )
        val snapshot = FitnessSnapshot(sessions = sessions, sets = sets)
        assertNull(TrainingHistory.estimatedOneRepMax(0.0, 5))
        assertNull(TrainingHistory.estimatedOneRepMax(60.0, 20))
        assertEquals(110.0, TrainingHistory.estimatedOneRepMax(110.0, 1)!!, 0.0001)
        assertEquals(116.6667, TrainingHistory.estimatedOneRepMax(100.0, 5)!!, 0.01)
        assertEquals(116.6667, TrainingHistory.bestE1rm(snapshot, Exercise.BARBELL_BACK_SQUAT, date)!!.value, 0.01)
        // s1 working tonnage: (5 * 100) + (20 * 40) = 1300 (warm-up excluded, s2 outside 7-day window, draft excluded)
        assertEquals(1300.0, TrainingHistory.weeklyTonnage(snapshot, date), 0.0001)
    }

    @Test fun `starter whole foods produce valid bilingual source-attributed records`() {
        assertTrue(StarterFoods.all.size >= 25)
        assertEquals(StarterFoods.all.size, StarterFoods.all.map { it.id }.distinct().size)
        StarterFoods.all.forEach { entry ->
            val en = entry.toFood(arabic = false)
            val ar = entry.toFood(arabic = true)
            assertTrue(en.source.isNotBlank())
            assertTrue(ar.name.isNotBlank())
            val portion = en.portion(en.referenceAmount, date)
            assertEquals(en.nutrients.calories, portion.calories, 0.0001)
        }
    }

    @Test fun `warmup ramp rounds to 2_5kg steps and stays below working load`() {
        assertTrue(WarmupRamp.steps(20.0, 20.0).isEmpty())
        val steps = WarmupRamp.steps(100.0, 20.0)
        assertEquals(listOf(20.0, 50.0, 70.0, 85.0), steps.map { it.weightKg })
        assertTrue(steps.all { it.weightKg < 100.0 })
    }

    @Test fun `pr detection streak and macro calorie helper are deterministic`() {
        val sessions = listOf(
            Session("w1", date.minusDays(1), "A", completedAt = 10L),
            Session("w2", date.minusDays(8), "A", completedAt = 10L),
        )
        val sets = listOf(
            TrainingSet(sessionId = "w1", exercise = Exercise.CHEST_PRESS, reps = 8, weightKg = 60.0, kind = SetKind.WORKING),
            TrainingSet(sessionId = "w1", exercise = Exercise.CHEST_PRESS, reps = 5, weightKg = 90.0, kind = SetKind.WARM_UP),
        )
        val snap = FitnessSnapshot(sessions = sessions, sets = sets)
        assertTrue(TrainingHistory.isNewWeightPr(snap, Exercise.CHEST_PRESS, 62.5, date))
        assertFalse(TrainingHistory.isNewWeightPr(snap, Exercise.CHEST_PRESS, 60.0, date))
        assertEquals(2, TrainingHistory.activeWeekStreak(snap, date))
        assertEquals(470.0, TrainingHistory.macroCalories(30.0, 50.0, 16.6667), 0.1)
    }
}
