import XCTest
@testable import FormaCore

final class WorkoutTransferTests: XCTestCase {
    func testVersionTwoFixturePreservesTargetsGroupsAndRejectsDowngrade() throws {
        let url=Bundle.module.url(forResource:"transfer-v2",withExtension:"json",subdirectory:"Fixtures")!
        let packet=try WatchTransferCodec.decode(Data(contentsOf:url))
        XCTAssertEqual(packet.version,2)
        XCTAssertEqual(packet.programs[0].prescriptions,packet.workouts[0].prescriptions)
        XCTAssertEqual(packet,try WatchTransferCodec.decode(WatchTransferCodec.encode(packet)))
        var downgraded=packet;downgraded.version=1
        XCTAssertThrowsError(try downgraded.validate())
        let legacy=try WatchTransferCodec.decode(fixture())
        XCTAssertFalse(String(data:try WatchTransferCodec.encode(legacy),encoding:.utf8)!.contains("prescriptions"))
    }
    func testLinkedFlowExcludesWarmupsAndUsesRestBetweenRounds() throws {
        let plans=[ExercisePrescription(exercise:.squat,workingSets:2,repMin:8,repMax:12,restSeconds:60,supersetGroup:1),ExercisePrescription(exercise:.row,workingSets:2,repMin:8,repMax:12,restSeconds:90,supersetGroup:1)]
        let program=WatchProgram(id:"guided",name:"Guided",exercises:[.squat,.row],prescriptions:plans)
        try program.validate()
        let one=WatchSet(exercise:.squat,reps:8,weightKg:20)
        let first=WorkoutFlow.afterSet(program:program,sets:[one],current:.squat,kind:.working)
        XCTAssertEqual(first.exercise,.row);XCTAssertEqual(first.restSeconds,0)
        let next=WorkoutFlow.afterSet(program:program,sets:[one,WatchSet(exercise:.row,reps:8,weightKg:20)],current:.row,kind:.working)
        XCTAssertEqual(next.exercise,.squat);XCTAssertEqual(next.restSeconds,90)
        XCTAssertEqual(WorkoutFlow.afterSet(program:program,sets:[],current:.squat,kind:.warmUp).exercise,.squat)
    }
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
        for changed in [raw.replacingOccurrences(of:"SQUAT",with:"UNKNOWN"),raw.replacingOccurrences(of:"\"KG\"",with:"\"LB\""),raw.replacingOccurrences(of:"\"version\": 1",with:"\"version\": 3")] {
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
        XCTAssertEqual(Exercise.allCases.count,74)
        for exercise in Exercise.allCases {
            let mapping=ExerciseMuscles.of(exercise)
            XCTAssertFalse(mapping.primary.isEmpty)
            XCTAssertTrue(Set(mapping.primary).isDisjoint(with:mapping.secondary))
            for muscle in mapping.primary+mapping.secondary {XCTAssertTrue(ExerciseMuscles.regions.contains {$0.muscle==muscle})}
        }
    }
    func testDetailedRegionsRollUpToBroadGroups() {
        XCTAssertEqual(MuscleDetail.allCases.count,21)
        for detail in MuscleDetail.allCases {XCTAssertTrue(ExerciseMuscles.regions.contains {$0.detail==detail})}
        for exercise in Exercise.allCases {
            let mapping=ExerciseMuscles.of(exercise)
            XCTAssertFalse(mapping.primaryDetail.isEmpty)
            XCTAssertTrue(Set(mapping.primaryDetail).isDisjoint(with:mapping.secondaryDetail))
            XCTAssertEqual(Set(mapping.primaryDetail.map {$0.group}),Set(mapping.primary))
            XCTAssertEqual(Set(mapping.secondaryDetail.map {$0.group}).subtracting(mapping.primary),Set(mapping.secondary))
        }
        XCTAssertEqual(ExerciseMuscles.of(.incline_barbell_press).primaryDetail,[.upper_chest])
        XCTAssertEqual(ExerciseMuscles.of(.seated_calf_raise).primaryDetail,[.soleus])
    }
}
