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
struct SavedState: Codable {
    var programs: [WatchProgram] = [
        .init(id: "starter-A", name: "A", exercises: [.squat,.chest_press,.row]),
        .init(id: "starter-B", name: "B", exercises: [.leg_press,.lat_pulldown,.hip_hinge])]
    var workouts: [WatchWorkout] = []
    var pendingIDs: [String] = []
    var libraryRevision: Int64?
    var draft: WorkoutDraft?
}
@MainActor final class LocalStore: ObservableObject {
    @Published private(set) var state = SavedState()
    @Published private(set) var failedToLoad = false
    @Published var notice: String?
    private let url: URL
    init() {
        url=FileManager.default.urls(for: .applicationSupportDirectory,in: .userDomainMask)[0].appendingPathComponent("forma.json")
        do {
            if FileManager.default.fileExists(atPath:url.path) {
                let data=try Data(contentsOf:url)
                guard data.count<=16_777_216 else {throw TransferError.capacity}
                let decoded=try JSONDecoder().decode(SavedState.self,from:data)
                try Self.validate(decoded);state=decoded
            }
        } catch {failedToLoad=true}
    }
    private static func validate(_ state: SavedState) throws {
        guard state.programs.count<=20, state.workouts.count<=500,
              Set(state.programs.map(\.id)).count==state.programs.count,
              Set(state.workouts.map(\.id)).count==state.workouts.count,
              Set(state.pendingIDs).isSubset(of:Set(state.workouts.map(\.id))) else {throw TransferError.invalid}
        try state.programs.forEach {try $0.validate()};try state.workouts.forEach {try $0.validate()}
        if let draft=state.draft {
            try draft.program.validate();try draft.sets.forEach {try $0.validate()}
            guard draft.sets.count<=300, Set(draft.sets.map(\.id)).count==draft.sets.count else {throw TransferError.invalid}
        }
    }
    private func commit(_ change: (inout SavedState) throws -> Void) throws {
        guard !failedToLoad else {throw TransferError.invalid}
        var candidate=state;try change(&candidate);try Self.validate(candidate)
        let data=try JSONEncoder().encode(candidate);guard data.count<=16_777_216 else {throw TransferError.capacity}
        try FileManager.default.createDirectory(at:url.deletingLastPathComponent(),withIntermediateDirectories:true)
        try data.write(to:url,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])
        state=candidate
    }
    func library(_ programs: [WatchProgram], revision: Int64? = nil) throws {
        guard !programs.isEmpty else {throw TransferError.invalid}
        if let revision, let previous=state.libraryRevision, revision<previous {return}
        try commit {$0.programs=programs;$0.libraryRevision=revision} // A live draft keeps its snapshot.
    }
    func receive(_ workouts: [WatchWorkout]) throws {
        try commit {state in
            for workout in workouts {
                try workout.validate()
                if let old=state.workouts.first(where:{$0.id==workout.id}) {
                    guard old==workout else {throw TransferError.conflict}
                } else {
                    let incoming=Set(workout.sets.map(\.id))
                    guard state.workouts.allSatisfy({incoming.isDisjoint(with:$0.sets.map(\.id))}) else {throw TransferError.conflict}
                    state.workouts.append(workout)
                }
            }
        }
    }
    func start(_ program: WatchProgram) throws {
        var formatter=DateFormatter();formatter.calendar=Calendar(identifier:.gregorian);formatter.locale=Locale(identifier:"en_US_POSIX");formatter.dateFormat="yyyy-MM-dd"
        let date=formatter.string(from:Date())
        try commit {guard $0.draft==nil else {throw TransferError.conflict};$0.draft=WorkoutDraft(date:date,program:program)}
    }
    func add(_ set: WatchSet) throws {
        try set.validate()
        try commit {state in
            guard var draft=state.draft else {throw TransferError.invalid}
            draft.sets.append(set)
            let next=WorkoutFlow.afterSet(program:draft.program,sets:draft.sets,current:set.exercise,kind:set.kind)
            draft.restUntil=next.restSeconds>0 ? Date().addingTimeInterval(Double(next.restSeconds)) : nil
            state.draft=draft
        }
    }
    func edit(_ set: WatchSet) throws {
        try set.validate();try commit {guard let index=$0.draft?.sets.firstIndex(where:{$0.id==set.id}) else {throw TransferError.invalid};$0.draft!.sets[index]=set}
    }
    func removeSet(_ id: String) throws {try commit {$0.draft?.sets.removeAll {$0.id==id}}}
    func stopRest() throws {try commit {$0.draft?.restUntil=nil}}
    func finish() throws {
        guard let draft=state.draft, !draft.sets.isEmpty else {throw TransferError.invalid}
        let workout=WatchWorkout(id:draft.id,date:draft.date,program:draft.program,completedAtMillis:Int64(Date().timeIntervalSince1970*1000),sets:draft.sets)
        try workout.validate()
        try commit {$0.workouts.append(workout);$0.pendingIDs.append(workout.id);$0.draft=nil}
    }
    func discardDraft() throws {try commit {$0.draft=nil}}
    func acknowledge(_ ids: [String]) throws {try commit {$0.pendingIDs.removeAll {ids.contains($0)}}}
    func deleteWorkout(_ id: String) throws {
        // No silent deletion of unsynced work. The user can export from phone, then explicitly delete there.
        guard !state.pendingIDs.contains(id) else {throw TransferError.conflict}
        try commit {$0.workouts.removeAll {$0.id==id}}
    }
    func attempt(_ action: () throws -> Void) {
        do {try action()} catch {notice=String(localized:"operation_failed")}
    }
}
