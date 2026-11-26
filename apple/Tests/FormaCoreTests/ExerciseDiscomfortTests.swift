import XCTest
@testable import FormaCore

final class ExerciseDiscomfortTests: XCTestCase {
    func testDefaultPreferenceDoesNotHideAnyExercise() {
        for exercise in Exercise.allCases {
            XCTAssertTrue(ExerciseDiscomfortRegion.includes(exercise, excluding: []), exercise.rawValue)
        }
    }

    func testNeckMatchesUpperTrapWorkAndLeavesRowsAlone() {
        let regions: Set<ExerciseDiscomfortRegion> = [.neck]
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.dumbbell_shrug, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.barbell_overhead_press, excluding: regions))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.row, excluding: regions))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.face_pull, excluding: regions))
    }

    func testUpperBackMatchesMidBack() {
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.barbell_row, excluding: [.upperBack]))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.dumbbell_shrug, excluding: [.upperBack]))
    }

    func testShouldersAndLowerBackKeepGroupRules() {
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.chest_press, excluding: [.shoulders]))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.squat, excluding: [.shoulders]))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.hip_hinge, excluding: [.lowerBack]))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.leg_extension, excluding: [.lowerBack]))
    }

    func testMarkingKeepsEverythingVisibleAndNamesAreas() {
        let chosen: Set<ExerciseDiscomfortRegion> = [.neck, .shoulders]
        XCTAssertTrue(Exercise.allCases.allSatisfy { ExerciseDiscomfortSelection.visible($0, chosen, mode: .mark) })
        XCTAssertFalse(ExerciseDiscomfortSelection.visible(.dumbbell_shrug, chosen, mode: .hide))
        XCTAssertEqual(ExerciseDiscomfortSelection.matches(.barbell_overhead_press, chosen), [.neck, .shoulders])
    }

    func testLegacyValueAndRoundTrip() {
        let legacy = ExerciseDiscomfortSelection.decode("NECK_UPPER_BACK,SHOULDERS,UNKNOWN,")
        XCTAssertEqual(legacy, [.neck, .upperBack, .shoulders])
        XCTAssertEqual(ExerciseDiscomfortSelection.encode(legacy), "NECK,SHOULDERS,UPPER_BACK")
        XCTAssertEqual(ExerciseDiscomfortSelection.decodeMode("", hasLegacySelection: true), .hide)
        XCTAssertEqual(ExerciseDiscomfortSelection.decodeMode("", hasLegacySelection: false), .mark)
        XCTAssertEqual(ExerciseDiscomfortSelection.decodeMode("MARK", hasLegacySelection: true), .mark)
        XCTAssertEqual(ExerciseDiscomfortSelection.decode(""), [])
    }

    func testFocusMusclesComeFirst() {
        let focus: Set<Muscle> = [.upper_back, .rear_delts]
        let ordered = ExerciseDiscomfortSelection.focusFirst(Exercise.allCases, focus)
        let split = ordered.firstIndex { !ExerciseDiscomfortSelection.isFocus($0, focus) } ?? ordered.count
        XCTAssertGreaterThan(split, 0)
        XCTAssertTrue(ordered[split...].allSatisfy { !ExerciseDiscomfortSelection.isFocus($0, focus) })
        XCTAssertEqual(ExerciseDiscomfortSelection.focusFirst(Exercise.allCases, []), Exercise.allCases)
        XCTAssertEqual(ExerciseDiscomfortSelection.decodeFocus(ExerciseDiscomfortSelection.encodeFocus(focus) + ",NOPE"), focus)
    }
}
