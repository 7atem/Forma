import Foundation
import HealthKit
import FormaCore

/// Reads (never writes) workouts and body mass from Apple Health, only after the user taps Connect.
/// Apple does not reveal whether read access was denied, so "nothing found" is reported neutrally.
@MainActor final class HealthImporter: ObservableObject {
    struct Result: Equatable { let workouts: Int; let weights: Int }

    @Published private(set) var syncing = false
    @Published private(set) var lastResult: Result?
    @Published private(set) var failed = false

    private let healthStore = HKHealthStore()

    static var isAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    private var readTypes: Set<HKObjectType> {
        [HKObjectType.workoutType(), HKQuantityType(.bodyMass), HKQuantityType(.heartRate), HKQuantityType(.activeEnergyBurned)]
    }

    /// First sync looks back one year; later syncs re-read the last week so late-arriving samples are caught.
    func sync(into store: LocalStore, now: Date = Date()) async {
        guard Self.isAvailable, !syncing else { return }
        syncing = true
        failed = false
        defer { syncing = false }
        do {
            try await healthStore.requestAuthorization(toShare: [], read: readTypes)
            let calendar = Calendar.current
            let start = store.state.healthLastSync.flatMap { calendar.date(byAdding: .day, value: -7, to: $0) }
                ?? calendar.date(byAdding: .year, value: -1, to: now) ?? now
            let workouts = try await readWorkouts(from: start, to: now)
            let weights = try await readWeights(from: start, to: now)
            let added = try store.mergeHealth(workouts: workouts, weights: weights, syncedAt: now)
            lastResult = Result(workouts: added.workouts, weights: added.weights)
        } catch {
            failed = true
        }
    }

    /// Refreshes quietly when the user connected before and the last sync is over an hour old.
    func refreshIfConnected(_ store: LocalStore, now: Date = Date()) async {
        guard let last = store.state.healthLastSync, now.timeIntervalSince(last) > 3600 else { return }
        await sync(into: store, now: now)
    }

    private func readWorkouts(from start: Date, to end: Date) async throws -> [HealthWorkoutRecord] {
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end)
        let descriptor = HKSampleQueryDescriptor(
            predicates: [.workout(predicate)],
            sortDescriptors: [SortDescriptor(\.startDate, order: .reverse)],
            limit: HealthMerge.maximumWorkouts
        )
        let perMinute = HKUnit.count().unitDivided(by: .minute())
        return try await descriptor.result(for: healthStore).map { workout in
            HealthWorkoutRecord(
                id: workout.uuid.uuidString,
                date: Self.dayString(workout.startDate),
                activity: Self.activity(workout.workoutActivityType),
                durationSeconds: workout.duration,
                activeEnergyKcal: workout.statistics(for: HKQuantityType(.activeEnergyBurned))?.sumQuantity()?.doubleValue(for: .kilocalorie()),
                averageHeartRateBpm: workout.statistics(for: HKQuantityType(.heartRate))?.averageQuantity()?.doubleValue(for: perMinute),
                sourceName: String(workout.sourceRevision.source.name.prefix(120))
            )
        }
    }

    private func readWeights(from start: Date, to end: Date) async throws -> [HealthWeightRecord] {
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end)
        let descriptor = HKSampleQueryDescriptor(
            predicates: [.quantitySample(type: HKQuantityType(.bodyMass), predicate: predicate)],
            sortDescriptors: [SortDescriptor(\.startDate, order: .reverse)],
            limit: HealthMerge.maximumWeights
        )
        return try await descriptor.result(for: healthStore).map { sample in
            HealthWeightRecord(
                id: sample.uuid.uuidString,
                date: Self.dayString(sample.startDate),
                weightKg: sample.quantity.doubleValue(for: .gramUnit(with: .kilo)),
                measuredAtMillis: Int64(sample.startDate.timeIntervalSince1970 * 1000)
            )
        }
    }

    /// Local calendar day, matching how Forma stores its own dates.
    nonisolated static func dayString(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    nonisolated static func activity(_ type: HKWorkoutActivityType) -> HealthActivity {
        switch type {
        case .traditionalStrengthTraining: return .strength
        case .functionalStrengthTraining, .crossTraining, .coreTraining: return .functional
        case .highIntensityIntervalTraining: return .hiit
        case .running: return .running
        case .walking, .hiking: return .walking
        case .cycling: return .cycling
        case .swimming: return .swimming
        case .yoga, .pilates, .mindAndBody, .flexibility: return .mindAndBody
        default: return .other
        }
    }
}
