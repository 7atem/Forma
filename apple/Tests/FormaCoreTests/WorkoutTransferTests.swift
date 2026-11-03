import XCTest
@testable import FormaCore

final class WorkoutTransferTests: XCTestCase {
    private func fixture() throws -> Data {try Data(contentsOf:Bundle.module.url(forResource:"transfer-v1",withExtension:"json",subdirectory:"Fixtures")!)}
    func testCrossPlatformFixtureRoundTrip() throws {
        let packet=try WatchTransferCodec.decode(fixture())
        XCTAssertEqual(packet,try WatchTransferCodec.decode(WatchTransferCodec.encode(packet)))
        XCTAssertEqual(packet.workouts[0].sets[0].kind,.warmUp)
        XCTAssertEqual(packet.workouts[0].sets[0].rpe,4.5)
        XCTAssertNil(packet.workouts[0].activeEnergyKcal)
    }
    func testRejectsUnsupportedVersionExerciseAndUnits() throws {
        let raw=String(data:try fixture(),encoding:.utf8)!
        for changed in [raw.replacingOccurrences(of:"SQUAT",with:"UNKNOWN"),raw.replacingOccurrences(of:"\"KG\"",with:"\"LB\""),raw.replacingOccurrences(of:"\"version\": 1",with:"\"version\": 2")] {
            XCTAssertThrowsError(try WatchTransferCodec.decode(Data(changed.utf8)))
        }
    }
    func testRejectsDuplicatesLimitsAndInvalidNumbers() throws {
        var packet=try WatchTransferCodec.decode(fixture())
        packet.workouts[0].sets.append(packet.workouts[0].sets[0])
        XCTAssertThrowsError(try packet.validate())
        XCTAssertThrowsError(try WatchTransferCodec.decode(Data(count:WatchTransferCodec.maximumBytes+1)))
        XCTAssertThrowsError(try WatchSet(exercise:.row,reps:8,weightKg:.nan).validate())
        XCTAssertThrowsError(try WatchSet(exercise:.row,reps:8,weightKg:10,rpe:11).validate())
    }
    func testCatalogCoversEveryExerciseWithoutOverlappingRoles() {
        XCTAssertEqual(Exercise.allCases.count,8)
        for exercise in Exercise.allCases {
            let mapping=ExerciseMuscles.of(exercise)
            XCTAssertFalse(mapping.primary.isEmpty)
            XCTAssertTrue(Set(mapping.primary).isDisjoint(with:mapping.secondary))
            for muscle in mapping.primary+mapping.secondary {XCTAssertTrue(ExerciseMuscles.regions.contains {$0.muscle==muscle})}
        }
    }
}
