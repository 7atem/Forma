import XCTest
@testable import FormaCore

final class TrendsTests: XCTestCase {
    func testUnloggedDaysAreUnknownAndTargetsCountOnlyLoggedDays() {
        let entries = [
            NutritionEntry(date: "2026-11-25", calories: 1000, protein: 60, carbs: 10, fat: 5),
            NutritionEntry(date: "2026-11-25", calories: 1050, protein: 70, carbs: 10, fat: 5),
            NutritionEntry(date: "2026-11-23", calories: 1500, protein: 100, carbs: 10, fat: 5),
            NutritionEntry(date: "2026-11-18", calories: 9999, protein: 999, carbs: 0, fat: 0),
            NutritionEntry(date: "2026-11-26", calories: 9999, protein: 999, carbs: 0, fat: 0),
        ]
        let trend = NutritionTrends.range(entries: entries, end: "2026-11-25", length: 7, calorieTarget: 2000, proteinTarget: 120)
        XCTAssertEqual(trend.days.count, 7)
        XCTAssertEqual(trend.days.first?.date, "2026-11-19")
        XCTAssertEqual(trend.loggedDays, 2)
        XCTAssertEqual(trend.meanCalories!, (2050 + 1500) / 2, accuracy: 0.001)
        XCTAssertEqual(trend.daysCaloriesNearTarget, 1)
        XCTAssertEqual(trend.daysProteinAtTarget, 1)
        let empty = NutritionTrends.range(entries: [], end: "2026-11-25", length: 30, calorieTarget: nil, proteinTarget: nil)
        XCTAssertEqual(empty.days.count, 30)
        XCTAssertNil(empty.meanCalories)
        XCTAssertNil(empty.daysCaloriesNearTarget)
    }

    func testRollingWeeksExcludeWarmupsAndFutureWorkouts() {
        let program = WatchProgram(id: "p", name: "P", exercises: [.squat])
        let done = WatchWorkout(id: "a", date: "2026-11-24", program: program, completedAtMillis: 1, sets: [
            WatchSet(exercise: .squat, reps: 10, weightKg: 40),
            WatchSet(exercise: .squat, reps: 5, weightKg: 20, kind: .warmUp),
        ])
        let lastWeek = WatchWorkout(id: "b", date: "2026-11-17", program: program, completedAtMillis: 1, sets: [WatchSet(exercise: .row, reps: 8, weightKg: 30)])
        let future = WatchWorkout(id: "c", date: "2026-11-27", program: program, completedAtMillis: 1, sets: [WatchSet(exercise: .row, reps: 8, weightKg: 30)])
        let weeks = TrainingTrends.weekly(workouts: [done, lastWeek, future], end: "2026-11-25", weeks: 3)
        XCTAssertEqual(weeks.count, 3)
        XCTAssertEqual(weeks.last?.start, "2026-11-19")
        XCTAssertEqual(weeks.last?.workingSets, 1)
        XCTAssertEqual(weeks.last?.volumeKg, 400)
        XCTAssertEqual(weeks[1].volumeKg, 240)
        XCTAssertEqual(weeks.first?.workingSets, 0)
    }
}
