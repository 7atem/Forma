import SwiftUI
import WatchKit
import FormaCore

@main @MainActor struct FormaWatchApp: App {
    @StateObject private var store: LocalStore
    @StateObject private var bridge: WatchBridge
    init() {
        let storage = LocalStore()
        _store = StateObject(wrappedValue: storage)
        _bridge = StateObject(wrappedValue: WatchBridge(store: storage))
    }
    var body: some Scene {
        WindowGroup {
            WatchHome(store: store, bridge: bridge)
                .tint(FormaTheme.primary)
        }
    }
}

@MainActor private struct WatchHome: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var bridge: WatchBridge
    var body: some View {
        NavigationStack {
            if store.failedToLoad {
                Text("storage_failed")
            } else if let draft = store.state.draft {
                WorkoutView(store: store, bridge: bridge, draft: draft)
            } else {
                List {
                    Section("programs") {
                        ForEach(store.state.programs) { program in
                            Button {
                                store.attempt { try store.start(program) }
                            } label: {
                                HStack {
                                    Text(program.name).font(.headline).foregroundStyle(FormaTheme.textPrimary)
                                    Spacer()
                                    Image(systemName: "play.fill").foregroundStyle(FormaTheme.primary)
                                }
                            }
                        }
                    }
                    Section {
                        Text("\(store.state.pendingIDs.count) \(String(localized: "pending_sync"))")
                            .font(.caption2)
                            .foregroundStyle(FormaTheme.textMuted)
                        Button("retry_sync") { bridge.flush() }
                            .foregroundStyle(FormaTheme.primary)
                        NavigationLink("completed_workouts") { WatchArchive(store: store) }
                        Text("watch_local_note")
                            .font(.caption2)
                            .foregroundStyle(FormaTheme.textMuted)
                    }
                }
                .navigationTitle("Forma")
            }
        }
        .onAppear { bridge.flush() }
        .alert("attention", isPresented: Binding(get: { store.notice != nil }, set: { if !$0 { store.notice = nil } })) {
            Button("ok") { store.notice = nil }
        } message: {
            Text(store.notice ?? "")
        }
    }
}

@MainActor private struct WatchArchive: View {
    @ObservedObject var store: LocalStore
    @State private var deleting: String?
    var body: some View {
        List {
            Text("archive_note").font(.caption2).foregroundStyle(FormaTheme.textMuted)
            ForEach(store.state.workouts.reversed()) { workout in
                let tonnage = FormaTheme.workingTonnage(workout.sets)
                VStack(alignment: .leading, spacing: 4) {
                    Text(workout.programName).font(.headline)
                    Text("\(workout.date) · \(workout.sets.count) \(String(localized: "sets"))")
                        .font(.caption2)
                        .foregroundStyle(FormaTheme.textMuted)
                    if tonnage > 0 {
                        FormaPill(text: "\(Int(tonnage.rounded())) kg")
                    }
                    Button("delete", role: .destructive) { deleting = workout.id }
                        .disabled(store.state.pendingIDs.contains(workout.id))
                }
            }
        }
        .confirmationDialog("delete_workout_confirm", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            Button("delete", role: .destructive) {
                if let id = deleting { store.attempt { try store.deleteWorkout(id) } }
                deleting = nil
            }
        }
    }
}

@MainActor private struct WorkoutView: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var bridge: WatchBridge
    let draft: WorkoutDraft
    @State private var exercise = Exercise.squat
    @State private var shownDraftID: String?
    @State private var editing: WatchSet?
    @State private var adding = false
    @State private var cancel = false
    @State private var finishing = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let tonnage = FormaTheme.workingTonnage(draft.sets)
        List {
            VStack(alignment: .leading, spacing: 4) {
                Text(draft.program.name).font(.headline).foregroundStyle(FormaTheme.primary)
                HStack(spacing: 6) {
                    Text("\(draft.sets.count) \(String(localized: "sets"))").font(.caption2).foregroundStyle(FormaTheme.textMuted)
                    if tonnage > 0 {
                        FormaPill(text: "\(Int(tonnage.rounded())) kg")
                    }
                }
            }

            if let deadline = draft.restUntil {
                VStack(spacing: 8) {
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        let remaining = max(0, Int(ceil(deadline.timeIntervalSince(context.date))))
                        let total = max(1, draft.program.restSeconds)
                        FormaProgressRing(
                            progress: Double(remaining) / Double(total),
                            valueText: "\(remaining)s",
                            labelText: String(localized: "rest"),
                            size: 72,
                            lineWidth: 7,
                            color: FormaTheme.primary,
                        )
                        .onChange(of: remaining) { _, value in
                            if value == 0 { WKInterfaceDevice.current().play(.notification) }
                        }
                    }
                    HStack(spacing: 6) {
                        Button("extend_rest") { store.attempt { try store.extendRest(seconds: 15) } }
                            .buttonStyle(.bordered)
                        Button("stop_rest") { store.attempt { try store.stopRest() } }
                            .font(.caption2)
                    }
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(FormaTheme.primaryContainer)
            }

            Picker("exercise", selection: $exercise) {
                ForEach((draft.program.exercises + Exercise.allCases).reduce(into: [Exercise]()) { if !$0.contains($1) { $0.append($1) } }, id: \.self) {
                    Text($0.label(arabic: arabic)).tag($0)
                }
            }

            if let plan = draft.program.prescriptions.first(where: { $0.exercise == exercise }) {
                HStack(spacing: 10) {
                    if let target = plan.workingSets {
                        let count = draft.sets.filter { $0.exercise == exercise && $0.kind == .working }.count
                        FormaProgressRing(
                            progress: Double(count) / Double(max(1, target)),
                            valueText: "\(count)/\(target)",
                            size: 48,
                            lineWidth: 5,
                            color: FormaTheme.primary,
                        )
                    }
                    VStack(alignment: .leading, spacing: 3) {
                        if let group = plan.supersetGroup {
                            FormaPill(text: "\(String(localized: "linked_group")) \(group)")
                        }
                        if let min = plan.repMin, let max = plan.repMax {
                            Text("\(String(localized: "planned_reps")) \(min)–\(max)").font(.caption2)
                        }
                        if !plan.notes.isEmpty {
                            Text(plan.notes).font(.caption2).foregroundStyle(FormaTheme.textMuted)
                        }
                    }
                }
            }

            Button {
                adding = true
            } label: {
                HStack {
                    Image(systemName: "plus.circle.fill")
                    Text("add_set").fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(FormaTheme.primary)
            .foregroundStyle(FormaTheme.onPrimary)
            .disabled(draft.sets.count >= 300)

            NavigationLink("exercise_library") { ExerciseDetail(exercise: exercise) }

            ForEach(draft.sets.reversed()) { set in
                Button { editing = set } label: {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(set.exercise.label(arabic: arabic)).font(.caption.weight(.semibold))
                        Text("\(set.reps) × \(set.weightKg.formatted()) kg")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(FormaTheme.primary)
                        HStack(spacing: 4) {
                            Text(set.kind == .warmUp ? String(localized: "warmup") : String(localized: "working"))
                                .font(.caption2)
                                .foregroundStyle(set.kind == .warmUp ? FormaTheme.secondary : FormaTheme.textMuted)
                            if let rpe = set.rpe {
                                Text("· RPE \(rpe.formatted())").font(.caption2).foregroundStyle(FormaTheme.tertiary)
                            }
                        }
                    }
                }
            }

            Button("finish_workout") { finishing = true }
                .foregroundStyle(FormaTheme.tertiary)
                .disabled(draft.sets.isEmpty)
            Button("discard_workout", role: .destructive) { cancel = true }
        }
        .onAppear {
            if shownDraftID != draft.id {
                exercise = draft.sets.last.map {
                    WorkoutFlow.afterSet(program: draft.program, sets: draft.sets, current: $0.exercise, kind: $0.kind).exercise ?? $0.exercise
                } ?? draft.program.exercises.first ?? .squat
                shownDraftID = draft.id
            }
        }
        .sheet(isPresented: $adding) {
            SetEditor(exercise: exercise, previous: draft.sets.last(where: { $0.exercise == exercise })) { set in
                store.attempt {
                    try store.add(set)
                    let next = WorkoutFlow.afterSet(program: draft.program, sets: draft.sets + [set], current: set.exercise, kind: set.kind)
                    exercise = next.exercise ?? exercise
                    adding = false
                    WKInterfaceDevice.current().play(.success)
                }
            }
        }
        .sheet(item: $editing) { set in
            SetEditor(exercise: set.exercise, previous: set, editing: true, onDelete: {
                store.attempt { try store.removeSet(set.id); editing = nil }
            }) { corrected in
                store.attempt { try store.edit(corrected); editing = nil }
            }
        }
        .confirmationDialog("discard_confirm", isPresented: $cancel, titleVisibility: .visible) {
            Button("discard_workout", role: .destructive) { store.attempt { try store.discardDraft() } }
        }
        .confirmationDialog("finish_confirm", isPresented: $finishing, titleVisibility: .visible) {
            Button("finish_workout") { store.attempt { try store.finish(); bridge.flush() } }
        }
    }
}

@MainActor private struct SetEditor: View {
    let exercise: Exercise
    let previous: WatchSet?
    var editing = false
    var onDelete: (() -> Void)?
    let save: (WatchSet) -> Void
    @State private var reps = 8
    @State private var kg = 0.0
    @State private var warmup = false
    @State private var hasRpe = false
    @State private var rpe = 5.0
    @State private var deleting = false

    var body: some View {
        Form {
            Stepper("\(String(localized: "reps")) \(reps)", value: $reps, in: 1...1000)
            HStack(spacing: 4) {
                ForEach([5, 8, 10, 12], id: \.self) { preset in
                    Button("\(preset)") { reps = preset }.buttonStyle(.bordered)
                }
            }
            Stepper("\(kg.formatted()) kg", value: $kg, in: 0...1000, step: 0.5)
            HStack(spacing: 4) {
                Button("+2.5") { kg = min(1000.0, kg + 2.5) }.buttonStyle(.bordered)
                Button("+5") { kg = min(1000.0, kg + 5.0) }.buttonStyle(.bordered)
            }
            Toggle("warmup", isOn: $warmup)
            Toggle("record_rpe", isOn: $hasRpe)
            if hasRpe { Stepper("RPE \(rpe.formatted())", value: $rpe, in: 0...10, step: 0.5) }
            Text("rpe_note").font(.caption)
            Button("save") {
                save(WatchSet(
                    id: editing ? previous!.id : UUID().uuidString,
                    exercise: exercise,
                    reps: reps,
                    weightKg: kg,
                    kind: warmup ? .warmUp : .working,
                    rpe: hasRpe ? rpe : nil,
                ))
            }
            if onDelete != nil { Button("delete", role: .destructive) { deleting = true } }
        }
        .onAppear {
            if let previous {
                reps = previous.reps
                kg = previous.weightKg
                warmup = previous.kind == .warmUp
                hasRpe = previous.rpe != nil
                rpe = previous.rpe ?? 5
            }
        }
        .confirmationDialog("delete_set_confirm", isPresented: $deleting, titleVisibility: .visible) {
            Button("delete", role: .destructive) { onDelete?() }
        }
    }
}
