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
        assertFalse(TrainingHistory.isNewPr(FitnessSnapshot(), Exercise.CHEST_PRESS, 60.0, 8, date))
        assertTrue(TrainingHistory.isNewPr(snap, Exercise.CHEST_PRESS, 62.5, 8, date))
        assertTrue(TrainingHistory.isNewPr(snap, Exercise.CHEST_PRESS, 60.0, 10, date))
        assertFalse(TrainingHistory.isNewPr(snap, Exercise.CHEST_PRESS, 60.0, 8, date))
        assertEquals(2, TrainingHistory.activeWeekStreak(snap, date))
        assertEquals(470.0, TrainingHistory.macroCalories(30.0, 50.0, 16.6667), 0.1)
        assertEquals(220.5, WeightUnits.fromKg(100.0, WeightUnit.LB), 0.1)
        assertEquals(100.0, WeightUnits.toKg(220.46226, WeightUnit.LB), 0.01)
        assertEquals(100.0, WeightUnits.fromKg(100.0, WeightUnit.KG), 0.0001)
    }

    @Test fun `weight unit lb round-trip repeat and mid-entry switch preserve canonical kg`() {
        // User logs 225 lb -> stored as canonical kg (~102.058 kg)
        val firstSavedKg = WeightUnits.toKg(225.0, WeightUnit.LB)
        assertEquals(102.058, firstSavedKg, 0.01)

        // "Use last set" in lb mode converts stored kg back to lb, then saves again -> must stay ~102.058 kg (never 46.3 kg)
        val prefilledLb = WeightUnits.fromKg(firstSavedKg, WeightUnit.LB)
        assertEquals(225.0, prefilledLb, 0.1)
        val repeatedSavedKg = WeightUnits.toKg(prefilledLb, WeightUnit.LB)
        assertEquals(firstSavedKg, repeatedSavedKg, 0.05)

        // Switching kg -> lb -> kg mid-entry preserves the physical load
        val switchedToLb = WeightUnits.convert(100.0, WeightUnit.KG, WeightUnit.LB)
        assertEquals(220.5, switchedToLb, 0.1)
        val switchedBackToKg = WeightUnits.convert(switchedToLb, WeightUnit.LB, WeightUnit.KG)
        assertEquals(100.0, switchedBackToKg, 0.05)

        // Rep-max ladder ignores warm-ups and ranks working sets accurately
        val s1 = Session("r1", date, "A", completedAt = 100L)
        val ladderSnap = FitnessSnapshot(
            sessions = listOf(s1),
            sets = listOf(
                TrainingSet(sessionId = "r1", exercise = Exercise.SQUAT, reps = 5, weightKg = 100.0, kind = SetKind.WORKING),
                TrainingSet(sessionId = "r1", exercise = Exercise.SQUAT, reps = 8, weightKg = 85.0, kind = SetKind.WORKING),
                TrainingSet(sessionId = "r1", exercise = Exercise.SQUAT, reps = 1, weightKg = 140.0, kind = SetKind.WARM_UP),
            ),
        )
        val ladder = TrainingHistory.repMaxLadder(ladderSnap, Exercise.SQUAT).toMap()
        assertEquals(100.0, ladder[1]!!, 0.01)
        assertEquals(100.0, ladder[5]!!, 0.01)
        assertEquals(85.0, ladder[8]!!, 0.01)
        assertFalse(10 in ladder)
    }

    @Test fun `profile name is optional but still bounded`() {
        assertEquals("", Profile("", Goal.BUILD_HABIT, 3).name)
        assertThrows(IllegalArgumentException::class.java) { Profile("x".repeat(81), Goal.BUILD_HABIT, 3) }
    }

    @Test fun `rolling weight trend averages recorded days in window without inventing missing days`() {
        assertTrue(TrainingHistory.rollingWeightTrend(emptyList()).isEmpty())
        val single = listOf(HistoryPoint(date, 80.0))
        assertEquals(single, TrainingHistory.rollingWeightTrend(single))
        val sparse = listOf(
            HistoryPoint(date.minusDays(10), 84.0),
            HistoryPoint(date.minusDays(4), 82.0),
            HistoryPoint(date.minusDays(2), 80.0),
            HistoryPoint(date, 81.0),
        )
        val trend = TrainingHistory.rollingWeightTrend(sparse, windowDays = 7)
        assertEquals(4, trend.size)
        assertEquals(84.0, trend[0].value, 0.0001)
        assertEquals(83.0, trend[1].value, 0.0001)
        assertEquals(81.0, trend[2].value, 0.0001)
        assertEquals(81.0, trend[3].value, 0.0001)
    }

    @Test fun `ramadan meal slots and combo recipe preserve immutable snapshots`() {
        val suhoor = food.portion(100.0, date).copy(slot = MealSlot.SUHOOR)
        val iftar = food.portion(200.0, date).copy(slot = MealSlot.IFTAR)
        assertEquals(MealSlot.SUHOOR, suhoor.slot)
        assertEquals(MealSlot.IFTAR, iftar.slot)
        val combo = Food.recipe(
            name = "Suhoor combo",
            servings = 1.0,
            source = "Manual",
            ingredients = listOf(suhoor, iftar).map { m ->
                IngredientSnapshot(m.name, m.quantity ?: 1.0, m.unit ?: NutritionUnit.SERVING, Nutrients(m.calories, m.protein, m.carbs, m.fat), m.source)
            },
        )
        assertEquals(600.0, combo.nutrients.calories, 0.0001)
        assertEquals(30.0, combo.nutrients.protein, 0.0001)
        assertEquals(2, combo.ingredients.size)
    }

    @Test fun `food swap calculator matches protein and calories accurately`() {
        val chicken = Food(id = "f1", name = "Chicken", referenceAmount = 100.0, unit = NutritionUnit.GRAMS, nutrients = Nutrients(165.0, 31.0, 0.0, 3.6), source = "USDA")
        val tuna = Food(id = "f2", name = "Tuna", referenceAmount = 100.0, unit = NutritionUnit.GRAMS, nutrients = Nutrients(116.0, 25.5, 0.0, 0.8), source = "USDA")
        val rice = Food(id = "f3", name = "Rice", referenceAmount = 100.0, unit = NutritionUnit.GRAMS, nutrients = Nutrients(130.0, 2.7, 28.2, 0.3), source = "USDA")
        val proteinSwaps = FoodSwapCalculator.findSwaps(chicken, 100.0, listOf(chicken, tuna, rice), SwapMatchBy.PROTEIN)
        assertEquals("f2", proteinSwaps.first().food.id)
        assertEquals(31.0, proteinSwaps.first().matchedNutrients.protein, 0.05)
        val calSwaps = FoodSwapCalculator.findSwaps(chicken, 100.0, listOf(chicken, tuna, rice), SwapMatchBy.CALORIES)
        assertEquals(2, calSwaps.size)
        assertEquals(165.0, calSwaps.first().matchedNutrients.calories, 0.05)
    }

    @Test fun `hydration expanded measurements and profile macro targets validate bounds`() {
        val h = Hydration(date, 2250)
        assertEquals(2250, h.milliliters)
        assertThrows(IllegalArgumentException::class.java) { Hydration(date, -10) }
        assertThrows(IllegalArgumentException::class.java) { Hydration(date, 25000) }
        val m = Measurement(date, weightKg = null, waistCm = null, chestCm = 101.5, armsCm = 36.0)
        assertEquals(101.5, m.chestCm!!, 0.001)
        assertThrows(IllegalArgumentException::class.java) { Measurement(date, null, null) }
        val p = Profile("Tester", Goal.BUILD_STRENGTH, 3, 2400, 160.0, carbsTarget = 250.0, fatTarget = 70.0, waterGoalMl = 3000)
        assertEquals(3000, p.waterGoalMl)
    }
}
