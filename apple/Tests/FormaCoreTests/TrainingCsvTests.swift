import XCTest
@testable import FormaCore

final class TrainingCsvTests: XCTestCase {
    func testMatchesAndroidColumnsAndNeutralisesFormulaCells() {
        let program = WatchProgram(id: "p1", name: "=HYPERLINK(\"x\")", exercises: [.squat])
        let later = WatchWorkout(id: "w2", date: "2026-11-25", program: program, completedAtMillis: 3000, sets: [
            WatchSet(id: "s2", exercise: .squat, reps: 5, weightKg: 100, kind: .working, rpe: 8.5),
        ])
        let earlier = WatchWorkout(id: "w1", date: "2026-11-24", program: program, completedAtMillis: 2000, sets: [
            WatchSet(id: "s1", exercise: .squat, reps: 10, weightKg: 40, kind: .warmUp),
        ])
        let lines = TrainingCsv.export([later, earlier]).components(separatedBy: "\r\n")
        XCTAssertEqual(lines[0], "\u{FEFF}session_id,date,session_label,source,exercise_id,reps,weight_kg,set_kind,rpe")
        XCTAssertEqual(lines[1], "\"w1\",\"2026-11-24\",\"'=HYPERLINK(\"\"x\"\")\",\"LOCAL\",\"SQUAT\",\"10\",\"40.0\",\"WARM_UP\",\"\"")
        XCTAssertEqual(lines[2], "\"w2\",\"2026-11-25\",\"'=HYPERLINK(\"\"x\"\")\",\"LOCAL\",\"SQUAT\",\"5\",\"100.0\",\"WORKING\",\"8.5\"")
        XCTAssertEqual(lines.count, 4)
        XCTAssertEqual(TrainingCsv.cell("  -1"), "\"'  -1\"")
        XCTAssertEqual(TrainingCsv.cell("Plain"), "\"Plain\"")
    }
}
