import XCTest
@testable import FormaCore

final class WorkoutInsightsTests: XCTestCase {
    func testPersonalRecordsExcludeWarmupsAndComputeE1rm() {
        let program = WatchProgram(id: "p1", name: "Upper", exercises: [.barbell_bench_press])
        let w1 = WatchWorkout(
            id: "w1",
            date: "2026-11-20",
            program: program,
            completedAtMillis: 1000,
            sets: [
                WatchSet(id: "s1", exercise: .barbell_bench_press, reps: 8, weightKg: 80, kind: .working),
                WatchSet(id: "s2", exercise: .barbell_bench_press, reps: 5, weightKg: 100, kind: .warmUp),
            ]
        )
        let prs = WorkoutInsights.personalRecords(workouts: [w1])
        XCTAssertEqual(prs.count, 1)
        XCTAssertEqual(prs[0].exercise, .barbell_bench_press)
        XCTAssertEqual(prs[0].weightKg, 80, accuracy: 0.001)
        XCTAssertEqual(prs[0].repsAtWeight, 8)
        XCTAssertEqual(prs[0].bestE1rmKg, 80 * (1.0 + 8.0 / 30.0), accuracy: 0.01)
        XCTAssertEqual(prs[0].date, "2026-11-20")
    }

    func testPersonalRecordsSortByRecencyThenWeight() {
        let p = WatchProgram(id: "p1", name: "Full", exercises: [.squat, .barbell_row])
        let w1 = WatchWorkout(
            id: "w1",
            date: "2026-11-20",
            program: p,
            completedAtMillis: 1000,
            sets: [WatchSet(id: "s1", exercise: .squat, reps: 5, weightKg: 120, kind: .working)]
        )
        let w2 = WatchWorkout(
            id: "w2",
            date: "2026-11-25",
            program: p,
            completedAtMillis: 2000,
            sets: [WatchSet(id: "s2", exercise: .barbell_row, reps: 8, weightKg: 70, kind: .working)]
        )
        let prs = WorkoutInsights.personalRecords(workouts: [w1, w2])
        XCTAssertEqual(prs.map(\.exercise), [.barbell_row, .squat])
    }

    func testFormatRecapExcludesWarmupsAndComputesVolume() {
        let p = WatchProgram(id: "p1", name: "Upper A", exercises: [.barbell_bench_press])
        let w = WatchWorkout(
            id: "w1",
            date: "2026-11-25",
            program: p,
            completedAtMillis: 2000,
            sets: [
                WatchSet(id: "s1", exercise: .barbell_bench_press, reps: 8, weightKg: 80, kind: .working),
                WatchSet(id: "s2", exercise: .barbell_bench_press, reps: 10, weightKg: 40, kind: .warmUp),
            ]
        )
        let recap = WorkoutInsights.formatRecap(workout: w, arabic: false, useLb: false)
        XCTAssertTrue(recap.contains("Forma · Upper A (2026-11-25)"))
        XCTAssertTrue(recap.contains("80 kg × 8"))
        XCTAssertFalse(recap.contains("40 kg"))
        XCTAssertTrue(recap.contains("1 sets · 640 kg"))
    }
}
