import XCTest
@testable import FormaCore

final class ExerciseDiscomfortTests: XCTestCase {
    func testDefaultPreferenceDoesNotHideAnyExercise() {
        for exercise in Exercise.allCases {
            XCTAssertTrue(ExerciseDiscomfortRegion.includes(exercise, excluding: []), exercise.rawValue)
        }
    }

    func testNeckUpperBackHidesPrimaryAndSupportingTargetsOnly() {
        let regions: Set<ExerciseDiscomfortRegion> = [.neckUpperBack]
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.row, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.lat_pulldown, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.shoulder_press, excluding: regions))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.dumbbell_curl, excluding: regions))
    }

    func testShouldersHidePrimaryAndSupportingTargetsOnly() {
        let regions: Set<ExerciseDiscomfortRegion> = [.shoulders]
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.face_pull, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.chest_press, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.push_up, excluding: regions))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.squat, excluding: regions))
    }

    func testLowerBackHidesPrimaryAndSupportingTargetsOnly() {
        let regions: Set<ExerciseDiscomfortRegion> = [.lowerBack]
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.hip_hinge, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.barbell_back_squat, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.barbell_row, excluding: regions))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.leg_extension, excluding: regions))
    }

    func testMultipleRegionsCombineAndSelectionRoundTripsDeterministically() {
        let regions: Set<ExerciseDiscomfortRegion> = [.lowerBack, .neckUpperBack, .shoulders]
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.barbell_row, excluding: regions))
        XCTAssertFalse(ExerciseDiscomfortRegion.includes(.push_up, excluding: regions))
        XCTAssertTrue(ExerciseDiscomfortRegion.includes(.leg_extension, excluding: regions))
        let encoded = ExerciseDiscomfortSelection.encode(regions)
        XCTAssertEqual(encoded, "LOWER_BACK,NECK_UPPER_BACK,SHOULDERS")
        XCTAssertEqual(ExerciseDiscomfortSelection.decode(encoded), regions)
        XCTAssertEqual(ExerciseDiscomfortSelection.decode("UNKNOWN"), [])
        XCTAssertEqual(ExerciseDiscomfortSelection.decode(""), [])
    }
}
