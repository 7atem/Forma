import XCTest
@testable import FormaCore

final class ExerciseMotionTests: XCTestCase {
    private let pilot: [Exercise] = [
        .squat, .barbell_bench_press, .barbell_row, .conventional_deadlift, .barbell_overhead_press,
        .barbell_curl, .skull_crusher, .dumbbell_curl, .hammer_curl, .dumbbell_shrug,
        .barbell_back_squat, .barbell_romanian_deadlift,
    ]
    private let sk = ExerciseMotionCatalog.skeleton

    func testPilotClipsExistAndOthersFallBackToPhotos() {
        for exercise in pilot { XCTAssertNotNil(ExerciseMotionCatalog.of(exercise), exercise.rawValue) }
        XCTAssertNil(ExerciseMotionCatalog.of(.leg_press))
    }

    func testBonesKeepLengthFeetStayPlantedAndNothingSinks() throws {
        for exercise in pilot {
            let clip = try XCTUnwrap(ExerciseMotionCatalog.of(exercise))
            for step in 0...200 {
                let t = Double(step) / 200
                let pose = MotionEngine.pose(clip, MotionEngine.sample(clip, t))
                let r = pose.right
                XCTAssertEqual((r.knee - r.hip).length, sk.thigh, accuracy: 1e-6)
                XCTAssertEqual((r.ankle - r.knee).length, sk.shin, accuracy: 1e-6)
                XCTAssertEqual((r.elbow - r.shoulder).length, sk.upperArm, accuracy: 1e-6)
                XCTAssertEqual((r.hand - r.elbow).length, sk.forearm, accuracy: 1e-6)
                XCTAssertEqual(r.ankle.x, clip.ankleHalf, accuracy: 1e-6)
                XCTAssertEqual(r.ankle.z, 0, accuracy: 1e-6)
                XCTAssertTrue(pose.joints.allSatisfy { $0.y >= -1e-3 }, "\(exercise) @ \(t)")
                XCTAssertEqual(r.hand.x, -pose.left.hand.x, accuracy: 1e-9)
                XCTAssertEqual(r.hand.y, pose.left.hand.y, accuracy: 1e-9)
                if clip.prop == .barbell { XCTAssertGreaterThanOrEqual(r.hand.y - clip.plateRadius, -0.005) }
            }
        }
    }

    func testLoopsAreSeamless() throws {
        for exercise in pilot {
            let clip = try XCTUnwrap(ExerciseMotionCatalog.of(exercise))
            XCTAssertEqual(MotionEngine.sample(clip, 0).pelvisY, MotionEngine.sample(clip, 0.999999).pelvisY, accuracy: 1e-4)
            XCTAssertEqual(MotionEngine.sample(clip, 0).hand.y, MotionEngine.sample(clip, 0.999999).hand.y, accuracy: 1e-4)
        }
    }

    func testHighlightsAndEffortMatchKotlin() throws {
        let roles = MotionEngine.highlights(.squat)
        XCTAssertEqual(roles[MusclePart(part: .thigh, side: .front)], 2)
        XCTAssertEqual(roles[MusclePart(part: .pelvis, side: .back)], 2)
        XCTAssertEqual(roles[MusclePart(part: .lower_torso, side: .front)], 1)
        let clip = try XCTUnwrap(ExerciseMotionCatalog.of(.squat))
        XCTAssertGreaterThan(MotionEngine.effort(clip, (clip.workingStart + clip.workingEnd) / 2), 0.95)
        XCTAssertEqual(MotionEngine.effort(clip, clip.workingStart / 2), 0.35, accuracy: 1e-9)
    }
}
