package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TrainingToolsTest {
    @Test fun exactBalancedLoadingHonorsAvailablePairsAndUsesFewestPlates() {
        val value=PlateCalculator.calculate(100.0,20.0,mapOf(25.0 to 1,20.0 to 2,15.0 to 1,10.0 to 4,5.0 to 4))!!
        assertEquals(40.0,value.perSide.sum(),0.0)
        assertEquals(2,value.perSide.size)
        assertNull(PlateCalculator.calculate(105.0,20.0,mapOf(20.0 to 2,10.0 to 2)))
        assertEquals(emptyList<Double>(),PlateCalculator.calculate(20.0,20.0,emptyMap())!!.perSide)
        assertNull(PlateCalculator.calculate(20.5,20.0,mapOf(0.5 to 2)))
        assertEquals(listOf(1.25),PlateCalculator.calculate(22.5,20.0,mapOf(1.25 to 1))!!.perSide)
        assertThrows(IllegalArgumentException::class.java) {PlateCalculator.calculate(10.0,20.0,emptyMap())}
    }
    @Test fun boundedSearchFindsACombinationWhenGreedyWouldFail() {
        assertEquals(listOf(15.0,15.0),PlateCalculator.calculate(80.0,20.0,mapOf(20.0 to 1,15.0 to 2))!!.perSide)
        assertNull(PlateCalculator.calculate(100.0,20.0,mapOf(20.0 to 1)))
        assertThrows(IllegalArgumentException::class.java) {PlateCalculator.calculate(80.0,20.0,mapOf(0.001 to 20))}
    }
    @Test fun csvEscapesMultilineFormulaNamesAndExcludesDraftsButKeepsWarmups() {
        val day=LocalDate.of(2026,11,4)
        val data=FitnessSnapshot(sessions=listOf(Session("done",day,"A",1,label="=HYPERLINK(\"x\")\nالعربية"),Session("draft",day,"A")),sets=listOf(
            TrainingSet("a","done",Exercise.GOBLET_SQUAT,8,12.5,SetKind.WARM_UP),TrainingSet("b","draft",Exercise.ROW,8,20.0)))
        val csv=TrainingCsv.export(data)
        assertTrue(csv.startsWith("\uFEFFsession_id,"))
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"x\"\")\nالعربية\""))
        assertTrue(csv.contains("\"GOBLET_SQUAT\",\"8\",\"12.5\",\"WARM_UP\""))
        assertFalse(csv.contains("draft"))
    }
    @Test fun similarFocusAndEquipmentPreserveCatalogIdentityAndAllowEmptySupportingRoles() {
        assertEquals(69,Exercise.entries.size)
        assertEquals(listOf(Exercise.SQUAT,Exercise.LEG_PRESS,Exercise.CHEST_PRESS,Exercise.ROW,Exercise.LAT_PULLDOWN,Exercise.HIP_HINGE,Exercise.SHOULDER_PRESS,Exercise.SPLIT_SQUAT),Exercise.entries.take(8))
        assertTrue(Equipment.BARBELL in ExerciseCatalog.of(Exercise.BARBELL_BACK_SQUAT).equipment)
        assertTrue(Exercise.PUSH_UP in ExerciseCatalog.similar(Exercise.DUMBBELL_BENCH_PRESS))
        assertFalse(Exercise.DUMBBELL_BENCH_PRESS in ExerciseCatalog.similar(Exercise.DUMBBELL_BENCH_PRESS))
        assertTrue(ExerciseMuscles.of(Exercise.DEAD_BUG).secondary.isEmpty())
        assertTrue(ExerciseCatalog.of(Exercise.REVERSE_LUNGE).perSide)
    }
    @Test fun nutritionAndMeasurementsCsvExportEscapeFormulasAndIncludeHydration() {
        val day = LocalDate.of(2026, 11, 24)
        val data = FitnessSnapshot(
            meals = listOf(Meal("m1", day, "=SUM(A1)", 320.0, 25.0, 30.0, 10.0, slot = MealSlot.IFTAR)),
            measurements = listOf(Measurement(day, weightKg = 78.5, waistCm = 84.0, chestCm = 101.0)),
            hydration = listOf(Hydration(day, 2500)),
        )
        val nCsv = NutritionCsv.export(data)
        assertTrue(nCsv.startsWith("\uFEFFmeal_id,"))
        assertTrue(nCsv.contains("\"'=SUM(A1)\""))
        assertTrue(nCsv.contains("\"IFTAR\""))
        val mCsv = MeasurementsCsv.export(data)
        assertTrue(mCsv.startsWith("\uFEFFdate,weight_kg,"))
        assertTrue(mCsv.contains("\"78.5\",\"84.0\",\"\",\"101.0\",\"\",\"\",\"\",\"2500\""))
    }
}
