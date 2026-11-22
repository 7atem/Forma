import XCTest
@testable import FormaCore

final class HealthImportTests: XCTestCase {
    private let idA = "1B6F3D2E-0C1A-4B7E-9A55-2F0D7C9E4A11"
    private let idB = "7C2A9E44-5D31-4F0B-8E6A-1A2B3C4D5E6F"
    private let idC = "0F1E2D3C-4B5A-4968-8776-655443322110"

    func testWorkoutsAreDeduplicatedBySampleIDAndInvalidSamplesSkipped() {
        let first = HealthWorkoutRecord(id: idA, date: "2026-11-20", activity: .running, durationSeconds: 1800, activeEnergyKcal: 250, averageHeartRateBpm: 148)
        let initial = HealthMerge.workouts(existing: [], incoming: [first])
        XCTAssertEqual(initial.added, 1)

        let second = HealthWorkoutRecord(id: idB, date: "2026-11-18", activity: .strength, durationSeconds: 3000)
        let invalid = HealthWorkoutRecord(id: "not-a-uuid", date: "2026-11-19", activity: .walking, durationSeconds: 600)
        let negative = HealthWorkoutRecord(id: idC, date: "2026-11-19", activity: .walking, durationSeconds: -5)
        let again = HealthMerge.workouts(existing: initial.records, incoming: [first, second, invalid, negative])
        XCTAssertEqual(again.added, 1)
        XCTAssertEqual(again.skippedInvalid, 2)
        XCTAssertEqual(again.records.map(\.id), [idB, idA], "sorted by date")
    }

    func testWorkoutCapacityDropsOldestImportedCopies() {
        let existing = (0..<HealthMerge.maximumWorkouts).map { index in
            HealthWorkoutRecord(id: UUID().uuidString, date: "2020-01-01", activity: .other, durationSeconds: Double(60 + index))
        }
        let newest = HealthWorkoutRecord(id: idA, date: "2026-11-20", activity: .cycling, durationSeconds: 900)
        let outcome = HealthMerge.workouts(existing: existing, incoming: [newest])
        XCTAssertEqual(outcome.records.count, HealthMerge.maximumWorkouts)
        XCTAssertEqual(outcome.records.last?.id, idA)
    }

    func testWeightsKeepLatestMeasurementPerDay() {
        let morning = HealthWeightRecord(id: idA, date: "2026-11-20", weightKg: 80.4, measuredAtMillis: 1_000)
        let evening = HealthWeightRecord(id: idB, date: "2026-11-20", weightKg: 81.0, measuredAtMillis: 2_000)
        let first = HealthMerge.weights(existing: [], incoming: [evening, morning])
        XCTAssertEqual(first.added, 1)
        XCTAssertEqual(first.records, [evening])
        let repeatImport = HealthMerge.weights(existing: first.records, incoming: [morning, evening])
        XCTAssertEqual(repeatImport.added, 0)
        XCTAssertEqual(repeatImport.records, [evening])
        let bad = HealthWeightRecord(id: idC, date: "2026-02-30", weightKg: 70, measuredAtMillis: 3_000)
        XCTAssertEqual(HealthMerge.weights(existing: [], incoming: [bad]).skippedInvalid, 1)
    }

    func testUserEnteredWeightAlwaysWinsForItsDay() {
        let health = [
            HealthWeightRecord(id: idA, date: "2026-11-19", weightKg: 80, measuredAtMillis: 1_000),
            HealthWeightRecord(id: idB, date: "2026-11-20", weightKg: 81, measuredAtMillis: 2_000),
        ]
        let points = HealthMerge.combinedWeights(user: [(date: "2026-11-20", weightKg: 79.5)], health: health)
        XCTAssertEqual(points, [
            WeightPoint(date: "2026-11-19", weightKg: 80, fromHealth: true),
            WeightPoint(date: "2026-11-20", weightKg: 79.5, fromHealth: false),
        ])
    }

    func testRecordsRoundTripThroughJSON() throws {
        let record = HealthWorkoutRecord(id: idA, date: "2026-11-20", activity: .hiit, durationSeconds: 1200, sourceName: "Apple Watch")
        let decoded = try JSONDecoder().decode(HealthWorkoutRecord.self, from: JSONEncoder().encode(record))
        XCTAssertEqual(decoded, record)
    }
}
