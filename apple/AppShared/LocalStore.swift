import Foundation
import Combine
import FormaCore

struct WorkoutDraft: Codable {
    var id = UUID().uuidString
    var date: String
    var program: WatchProgram
    var sets: [WatchSet] = []
    var restUntil: Date?
}

struct PhoneMeal: Codable, Identifiable, Equatable {
    var id = UUID().uuidString
    var date: String
    var name: String
    var calories: Double
    var protein: Double
    var carbs: Double
    var fat: Double
    var slot: String = "UNSPECIFIED"
}

struct PhoneWeight: Codable, Identifiable, Equatable {
    var id: String { date }
    var date: String
    var weightKg: Double
}

struct SavedState: Codable {
    var programs: [WatchProgram] = [
        .init(id: "starter-A", name: "A", exercises: [.squat, .chest_press, .row]),
        .init(id: "starter-B", name: "B", exercises: [.leg_press, .lat_pulldown, .hip_hinge]),
    ]
    var workouts: [WatchWorkout] = []
    var pendingIDs: [String] = []
    var libraryRevision: Int64?
    var draft: WorkoutDraft?
    var meals: [PhoneMeal] = []
    var weights: [PhoneWeight] = []
    var calorieTarget: Int?
    var proteinTarget: Double?
    /// Read-only copies imported from Apple Health; kept apart from Forma-logged workouts and weights.
    var healthWorkouts: [HealthWorkoutRecord] = []
    var healthWeights: [HealthWeightRecord] = []
    var healthLastSync: Date?

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        programs = try c.decodeIfPresent([WatchProgram].self, forKey: .programs) ?? [
            .init(id: "starter-A", name: "A", exercises: [.squat, .chest_press, .row]),
            .init(id: "starter-B", name: "B", exercises: [.leg_press, .lat_pulldown, .hip_hinge]),
        ]
        workouts = try c.decodeIfPresent([WatchWorkout].self, forKey: .workouts) ?? []
        pendingIDs = try c.decodeIfPresent([String].self, forKey: .pendingIDs) ?? []
        libraryRevision = try c.decodeIfPresent(Int64.self, forKey: .libraryRevision)
        draft = try c.decodeIfPresent(WorkoutDraft.self, forKey: .draft)
        meals = try c.decodeIfPresent([PhoneMeal].self, forKey: .meals) ?? []
        weights = try c.decodeIfPresent([PhoneWeight].self, forKey: .weights) ?? []
        calorieTarget = try c.decodeIfPresent(Int.self, forKey: .calorieTarget)
        proteinTarget = try c.decodeIfPresent(Double.self, forKey: .proteinTarget)
        healthWorkouts = try c.decodeIfPresent([HealthWorkoutRecord].self, forKey: .healthWorkouts) ?? []
        healthWeights = try c.decodeIfPresent([HealthWeightRecord].self, forKey: .healthWeights) ?? []
        healthLastSync = try c.decodeIfPresent(Date.self, forKey: .healthLastSync)
    }
}

@MainActor final class LocalStore: ObservableObject {
    @Published private(set) var state = SavedState()
    @Published private(set) var failedToLoad = false
    @Published var notice: String?
    private let url: URL

    init() {
        url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("forma.json")
        do {
            if FileManager.default.fileExists(atPath: url.path) {
                let data = try Data(contentsOf: url)
                guard data.count <= 16_777_216 else { throw TransferError.capacity }
                let decoded = try JSONDecoder().decode(SavedState.self, from: data)
                try Self.validate(decoded)
                state = decoded
            }
        } catch { failedToLoad = true }
    }

    nonisolated static func todayString() -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }

    private static func validate(_ state: SavedState) throws {
        guard state.programs.count <= 20, state.workouts.count <= 500,
              state.meals.count <= 5000, state.weights.count <= 2000,
              Set(state.programs.map(\.id)).count == state.programs.count,
              Set(state.workouts.map(\.id)).count == state.workouts.count,
              Set(state.pendingIDs).isSubset(of: Set(state.workouts.map(\.id))),
              state.healthWorkouts.count <= HealthMerge.maximumWorkouts, state.healthWeights.count <= HealthMerge.maximumWeights,
              Set(state.healthWorkouts.map(\.id)).count == state.healthWorkouts.count,
              Set(state.healthWeights.map(\.date)).count == state.healthWeights.count else { throw TransferError.invalid }
        try state.healthWorkouts.forEach { try $0.validate() }
        try state.healthWeights.forEach { try $0.validate() }
        try state.programs.forEach { try $0.validate() }
        try state.workouts.forEach { try $0.validate() }
        if let draft = state.draft {
            try draft.program.validate()
            try draft.sets.forEach { try $0.validate() }
            guard draft.sets.count <= 300, Set(draft.sets.map(\.id)).count == draft.sets.count else { throw TransferError.invalid }
        }
    }

    private func commit(_ change: (inout SavedState) throws -> Void) throws {
        guard !failedToLoad else { throw TransferError.invalid }
        var candidate = state
        try change(&candidate)
        try Self.validate(candidate)
        let data = try JSONEncoder().encode(candidate)
        guard data.count <= 16_777_216 else { throw TransferError.capacity }
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
        state = candidate
    }

    func library(_ programs: [WatchProgram], revision: Int64? = nil) throws {
        guard !programs.isEmpty else { throw TransferError.invalid }
        if let revision, let previous = state.libraryRevision, revision < previous { return }
        try commit { $0.programs = programs; $0.libraryRevision = revision }
    }

    func saveProgram(_ program: WatchProgram) throws {
        try program.validate()
        try commit { state in
            if let idx = state.programs.firstIndex(where: { $0.id == program.id }) {
                state.programs[idx] = program
            } else {
                guard state.programs.count < 20 else { throw TransferError.capacity }
                state.programs.append(program)
            }
        }
    }

    func deleteProgram(_ id: String) throws {
        try commit { state in
            guard state.programs.count > 1 else { throw TransferError.invalid }
            state.programs.removeAll { $0.id == id }
        }
    }

    func receive(_ workouts: [WatchWorkout]) throws {
        try commit { state in
            for workout in workouts {
                try workout.validate()
                if let old = state.workouts.first(where: { $0.id == workout.id }) {
                    guard old == workout else { throw TransferError.conflict }
                } else {
                    let incoming = Set(workout.sets.map(\.id))
                    guard state.workouts.allSatisfy({ incoming.isDisjoint(with: $0.sets.map(\.id)) }) else { throw TransferError.conflict }
                    state.workouts.append(workout)
                }
            }
        }
    }

    func start(_ program: WatchProgram) throws {
        let date = Self.todayString()
        try commit {
            guard $0.draft == nil else { throw TransferError.conflict }
            $0.draft = WorkoutDraft(date: date, program: program)
        }
    }

    func add(_ set: WatchSet) throws {
        try set.validate()
        try commit { state in
            guard var draft = state.draft else { throw TransferError.invalid }
            draft.sets.append(set)
            let next = WorkoutFlow.afterSet(program: draft.program, sets: draft.sets, current: set.exercise, kind: set.kind)
            draft.restUntil = next.restSeconds > 0 ? Date().addingTimeInterval(Double(next.restSeconds)) : nil
            state.draft = draft
        }
    }

    func quickLog(_ set: WatchSet, label: String) throws {
        try set.validate()
        let date = Self.todayString()
        try commit { state in
            if var draft = state.draft {
                draft.sets.append(set)
                let next = WorkoutFlow.afterSet(program: draft.program, sets: draft.sets, current: set.exercise, kind: set.kind)
                draft.restUntil = next.restSeconds > 0 ? Date().addingTimeInterval(Double(next.restSeconds)) : nil
                state.draft = draft
            } else {
                let program = WatchProgram(
                    id: "quick",
                    name: label.isEmpty ? set.exercise.rawValue : label,
                    exercises: [set.exercise],
                    restSeconds: 90,
                    prescriptions: []
                )
                var draft = WorkoutDraft(date: date, program: program)
                draft.sets.append(set)
                draft.restUntil = Date().addingTimeInterval(90)
                state.draft = draft
            }
        }
    }

    func edit(_ set: WatchSet) throws {
        try set.validate()
        try commit {
            guard let index = $0.draft?.sets.firstIndex(where: { $0.id == set.id }) else { throw TransferError.invalid }
            $0.draft!.sets[index] = set
        }
    }

    func removeSet(_ id: String) throws { try commit { $0.draft?.sets.removeAll { $0.id == id } } }
    func stopRest() throws { try commit { $0.draft?.restUntil = nil } }

    func extendRest(seconds: Int) throws {
        try commit { state in
            guard var draft = state.draft else { throw TransferError.invalid }
            let base = max(Date(), draft.restUntil ?? Date())
            draft.restUntil = base.addingTimeInterval(Double(seconds))
            state.draft = draft
        }
    }

    func finish(queueForPhone: Bool = true) throws {
        guard let draft = state.draft, !draft.sets.isEmpty else { throw TransferError.invalid }
        let workout = WatchWorkout(id: draft.id, date: draft.date, program: draft.program, completedAtMillis: Int64(Date().timeIntervalSince1970 * 1000), sets: draft.sets)
        try workout.validate()
        try commit {
            $0.workouts.append(workout)
            if queueForPhone { $0.pendingIDs.append(workout.id) }
            $0.draft = nil
        }
    }

    func discardDraft() throws { try commit { $0.draft = nil } }
    func acknowledge(_ ids: [String]) throws { try commit { $0.pendingIDs.removeAll { ids.contains($0) } } }

    func deleteWorkout(_ id: String) throws {
        guard !state.pendingIDs.contains(id) else { throw TransferError.conflict }
        try commit { $0.workouts.removeAll { $0.id == id } }
    }

    func addMeal(_ meal: PhoneMeal) throws {
        guard !meal.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              meal.calories.isFinite, meal.calories >= 0, meal.calories <= 20000,
              meal.protein.isFinite, meal.protein >= 0,
              meal.carbs.isFinite, meal.carbs >= 0,
              meal.fat.isFinite, meal.fat >= 0 else { throw TransferError.invalid }
        try commit { $0.meals.append(meal) }
    }

    func deleteMeal(_ id: String) throws {
        try commit { $0.meals.removeAll { $0.id == id } }
    }

    func saveWeight(_ weightKg: Double, date: String = LocalStore.todayString()) throws {
        guard weightKg.isFinite, weightKg > 0, weightKg <= 1000 else { throw TransferError.invalid }
        try commit { state in
            state.weights.removeAll { $0.date == date }
            state.weights.append(PhoneWeight(date: date, weightKg: weightKg))
            state.weights.sort { $0.date < $1.date }
        }
    }

    func deleteWeight(_ date: String) throws {
        try commit { $0.weights.removeAll { $0.date == date } }
    }

    func saveTargets(calories: Int?, protein: Double?) throws {
        try commit {
            $0.calorieTarget = calories
            $0.proteinTarget = protein
        }
    }

    /// Body-weight trend from both sources; the user's own entry wins for its day.
    var combinedWeights: [WeightPoint] {
        HealthMerge.combinedWeights(user: state.weights.map { (date: $0.date, weightKg: $0.weightKg) }, health: state.healthWeights)
    }

    /// Stores newly read Apple Health samples. Never touches Forma workouts, sets, meals or user weights.
    @discardableResult
    func mergeHealth(workouts: [HealthWorkoutRecord], weights: [HealthWeightRecord], syncedAt: Date) throws -> (workouts: Int, weights: Int) {
        var added = (workouts: 0, weights: 0)
        try commit { state in
            let w = HealthMerge.workouts(existing: state.healthWorkouts, incoming: workouts)
            let b = HealthMerge.weights(existing: state.healthWeights, incoming: weights)
            state.healthWorkouts = w.records
            state.healthWeights = b.records
            state.healthLastSync = syncedAt
            added = (w.added, b.added)
        }
        return added
    }

    /// Removes only the imported copies; Apple Health itself is unchanged.
    func removeHealthData() throws {
        try commit {
            $0.healthWorkouts = []
            $0.healthWeights = []
            $0.healthLastSync = nil
        }
    }

    func attempt(_ action: () throws -> Void) {
        do { try action() } catch { notice = String(localized: "operation_failed") }
    }
}
