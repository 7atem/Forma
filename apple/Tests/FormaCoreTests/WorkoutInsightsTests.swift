import XCTest
@testable import FormaCore

final class WorkoutInsightsTests: XCTestCase {
    private func recap(_ workout: WatchWorkout) -> String {
        WorkoutInsights.formatRecap(
            workout: workout,
            exerciseName: { $0.label(arabic: false) },
            formatLoad: { $0 > 0 ? "\(Int($0)) kg" : "bodyweight" },
            setsLabel: { "\($0) sets" },
            volumeLabel: { "\(Int($0)) kg" }
        )
    }

    func testRecapListsWorkingSetsAndExcludesWarmupsFromVolume() {
        let program = WatchProgram(id: "p1", name: "Upper A", exercises: [.barbell_bench_press])
        let workout = WatchWorkout(id: "w1", date: "2026-11-25", program: program, completedAtMillis: 2000, sets: [
            WatchSet(id: "s1", exercise: .barbell_bench_press, reps: 8, weightKg: 80, kind: .working),
            WatchSet(id: "s2", exercise: .barbell_bench_press, reps: 10, weightKg: 40, kind: .warmUp),
        ])
        let text = recap(workout)
        XCTAssertTrue(text.contains("Forma · Upper A (2026-11-25)"))
        XCTAssertTrue(text.contains("80 kg × 8"))
        XCTAssertFalse(text.contains("× 10"), "warm-up set must not be listed")
        XCTAssertTrue(text.hasSuffix("1 sets · 640 kg"))
    }

    func testBodyweightOnlyRecapHasNoVolume() {
        let program = WatchProgram(id: "p1", name: "Core", exercises: [.squat])
        let workout = WatchWorkout(id: "w2", date: "2026-11-25", program: program, completedAtMillis: 2000, sets: [
            WatchSet(id: "s1", exercise: .squat, reps: 15, weightKg: 0, kind: .working),
        ])
        let text = recap(workout)
        XCTAssertTrue(text.contains("bodyweight × 15"))
        XCTAssertTrue(text.hasSuffix("\n1 sets"))
    }
}
