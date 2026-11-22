import SwiftUI
import UniformTypeIdentifiers
import FormaCore

extension UTType {
    static let formaWorkouts = UTType(exportedAs: "com.formacoach.workouts", conformingTo: .json)
}

struct TransferDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.formaWorkouts, .json] }
    var data: Data
    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws {
        guard let bytes = configuration.file.regularFileContents else { throw TransferError.invalid }
        data = bytes
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

@main @MainActor struct FormaPhoneApp: App {
    @StateObject private var store: LocalStore
    @StateObject private var bridge: WatchBridge
    init() {
        let storage = LocalStore()
        _store = StateObject(wrappedValue: storage)
        _bridge = StateObject(wrappedValue: WatchBridge(store: storage))
    }
    var body: some Scene {
        WindowGroup {
            PhoneRoot(store: store, bridge: bridge)
                .preferredColorScheme(.dark)
                .tint(FormaTheme.primary)
        }
    }
}

@MainActor private struct PhoneRoot: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var bridge: WatchBridge
    @State private var importing = false
    @State private var pending: WatchTransfer?
    @State private var document: TransferDocument?
    @State private var exporting = false
    @State private var deleting: String?
    @State private var showBridge = false

    var body: some View {
        Group {
            if store.failedToLoad {
                ZStack {
                    FormaTheme.background.ignoresSafeArea()
                    FormaCard { Text("storage_failed").foregroundStyle(FormaTheme.textPrimary) }
                        .padding()
                }
            } else {
                TabView {
                    PhoneTrainTab(
                        store: store,
                        onOpenBridge: { showBridge = true },
                        onExport: { workout in
                            store.attempt {
                                document = TransferDocument(data: try WatchTransferCodec.encode(WatchTransfer(workouts: [workout])))
                                exporting = true
                            }
                        },
                        onDelete: { deleting = $0 }
                    )
                    .tabItem { Label("tab_train", systemImage: "dumbbell.fill") }

                    PhoneEatTab(store: store, onOpenBridge: { showBridge = true })
                        .tabItem { Label("tab_eat", systemImage: "fork.knife") }

                    PhoneProgressTab(store: store, onOpenBridge: { showBridge = true })
                        .tabItem { Label("tab_progress", systemImage: "chart.xyaxis.line") }

                    PhoneExercisesTab(onOpenBridge: { showBridge = true })
                        .tabItem { Label("tab_exercises", systemImage: "figure.strengthtraining.traditional") }
                }
                .tint(FormaTheme.primary)
            }
        }
        .sheet(isPresented: $showBridge) {
            PhoneBridgeTab(store: store, bridge: bridge, onImport: { showBridge = false; importing = true })
                .preferredColorScheme(.dark)
                .tint(FormaTheme.primary)
        }
        .fileImporter(isPresented: $importing, allowedContentTypes: [.formaWorkouts, .json, .data]) { result in
            store.attempt {
                let url = try result.get()
                let access = url.startAccessingSecurityScopedResource()
                defer { if access { url.stopAccessingSecurityScopedResource() } }
                let handle = try FileHandle(forReadingFrom: url)
                defer { try? handle.close() }
                let bytes = try handle.read(upToCount: WatchTransferCodec.maximumBytes + 1) ?? Data()
                let transfer = try WatchTransferCodec.decode(bytes)
                guard transfer.workouts.isEmpty, !transfer.programs.isEmpty else { throw TransferError.invalid }
                pending = transfer
            }
        }
        .confirmationDialog("replace_programs", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } }), titleVisibility: .visible) {
            Button("confirm") {
                if let transfer = pending { store.attempt { try store.library(transfer.programs) } }
                pending = nil
            }
            Button("cancel", role: .cancel) { pending = nil }
        } message: {
            Text("\(pending?.programs.count ?? 0) \(String(localized: "programs"))")
        }
        .fileExporter(isPresented: $exporting, document: document, contentType: .formaWorkouts, defaultFilename: "forma-workout.formaworkouts") { result in
            if case .failure = result { store.notice = String(localized: "operation_failed") }
            document = nil
        }
        .confirmationDialog("delete_workout_confirm", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            Button("delete", role: .destructive) {
                if let id = deleting { store.attempt { try store.deleteWorkout(id) } }
                deleting = nil
            }
        }
        .alert("attention", isPresented: Binding(get: { store.notice != nil }, set: { if !$0 { store.notice = nil } })) {
            Button("ok") { store.notice = nil }
        } message: {
            Text(store.notice ?? "")
        }
    }
}

private struct PRCardItem: Identifiable {
    let exercise: Exercise
    let weight: Double
    let reps: Int
    let e1rm: Double?
    var id: Exercise { exercise }
}

private struct MealSlotOption: Identifiable {
    let id: String
    let labelKey: String
}

@MainActor private struct PhoneTrainTab: View {
    @ObservedObject var store: LocalStore
    let onOpenBridge: () -> Void
    let onExport: (WatchWorkout) -> Void
    let onDelete: (String) -> Void
    @State private var creatingProgram = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let draft = store.state.draft {
                        PhoneWorkoutView(store: store, draft: draft)
                    } else {
                        let workoutDates = Set(store.state.workouts.map(\.date))
                        let recentCount = min(store.state.workouts.count, 3)
                        let totalVolume = store.state.workouts.suffix(7).reduce(0.0) { $0 + FormaTheme.workingTonnage($1.sets) }
                        FormaWeeklyStrip(workoutDates: workoutDates)
                        FormaCard {
                            HStack(spacing: 16) {
                                VStack(alignment: .leading, spacing: 6) {
                                    FormaPill(text: String(localized: "completed_workouts"))
                                    Text("\(store.state.workouts.count)")
                                        .font(.largeTitle.weight(.heavy))
                                        .foregroundStyle(FormaTheme.textPrimary)
                                    if totalVolume > 0 {
                                        Text("\(String(localized: "volume_kg")): \(Int(totalVolume.rounded())) kg")
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(FormaTheme.primary)
                                    }
                                }
                                Spacer()
                                FormaProgressRing(
                                    progress: Double(recentCount) / 3.0,
                                    valueText: "\(recentCount)/3",
                                    size: 76,
                                    lineWidth: 8,
                                    color: FormaTheme.primary
                                )
                            }
                        }

                        // 7-Day Body Muscle Heat Map
                        let muscleCounts: [Muscle: Int] = {
                            var counts: [Muscle: Int] = [:]
                            for workout in store.state.workouts.suffix(7) {
                                for set in workout.sets where set.kind == .working {
                                    let act = ExerciseMuscles.of(set.exercise)
                                    for m in act.primary + act.secondary { counts[m, default: 0] += 1 }
                                }
                            }
                            return counts
                        }()
                        if !muscleCounts.isEmpty {
                            FormaCard { WeeklyMuscleHeatMapView(setsByMuscle: muscleCounts) }
                        }

                        // Personal Records (PR) Trophy Strip
                        let prs: [PRCardItem] = {
                            var bestByExercise: [Exercise: WatchSet] = [:]
                            for workout in store.state.workouts {
                                for set in workout.sets where set.kind == .working && set.weightKg > 0 {
                                    if let cur = bestByExercise[set.exercise] {
                                        if set.weightKg > cur.weightKg || (set.weightKg == cur.weightKg && set.reps > cur.reps) {
                                            bestByExercise[set.exercise] = set
                                        }
                                    } else {
                                        bestByExercise[set.exercise] = set
                                    }
                                }
                            }
                            return bestByExercise.values
                                .sorted { $0.weightKg > $1.weightKg }
                                .prefix(8)
                                .map { PRCardItem(exercise: $0.exercise, weight: $0.weightKg, reps: $0.reps, e1rm: FormaTheme.estimatedOneRepMax(weightKg: $0.weightKg, reps: $0.reps)) }
                        }()
                        if !prs.isEmpty {
                            Text("personal_records_title")
                                .font(.title3.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 10) {
                                    ForEach(prs) { item in
                                        VStack(alignment: .leading, spacing: 6) {
                                            Text(item.exercise.label(arabic: arabic))
                                                .font(.caption.weight(.bold))
                                                .foregroundStyle(FormaTheme.textPrimary)
                                                .lineLimit(1)
                                            Text("\(item.weight.formatted()) kg × \(item.reps)")
                                                .font(.headline.weight(.heavy))
                                                .foregroundStyle(FormaTheme.primary)
                                            if let est = item.e1rm {
                                                FormaPill(
                                                    text: "\(String(localized: "best_e1rm")) \(Int(est.rounded()))",
                                                    background: FormaTheme.tertiary.opacity(0.2),
                                                    foreground: FormaTheme.tertiary
                                                )
                                            }
                                        }
                                        .padding(12)
                                        .frame(width: 154, alignment: .leading)
                                        .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(FormaTheme.border.opacity(0.75), lineWidth: 1))
                                    }
                                }
                            }
                        }

                        HStack {
                            Text("programs")
                                .font(.title2.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            Button { creatingProgram = true } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "plus")
                                    Text("new_program").fontWeight(.semibold)
                                }
                                .font(.caption)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .background(FormaTheme.primaryContainer, in: Capsule())
                                .foregroundStyle(FormaTheme.primary)
                            }
                        }
                        ForEach(store.state.programs) { program in
                            FormaCard {
                                HStack(spacing: 12) {
                                    if let first = program.exercises.first {
                                        Image(ExerciseGuides.of(first, arabic: false).asset)
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 84)
                                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    }
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(program.name)
                                            .font(.headline.weight(.bold))
                                            .foregroundStyle(FormaTheme.textPrimary)
                                        Text(program.exercises.map { $0.label(arabic: arabic) }.joined(separator: " · "))
                                            .font(.caption)
                                            .foregroundStyle(FormaTheme.textMuted)
                                            .lineLimit(2)
                                    }
                                }
                                Button {
                                    store.attempt { try store.start(program) }
                                } label: {
                                    HStack {
                                        Image(systemName: "play.fill")
                                        Text("start_workout").fontWeight(.bold)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .foregroundStyle(FormaTheme.onPrimary)
                                }
                            }
                        }

                        Text("completed_workouts")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(FormaTheme.textPrimary)
                            .padding(.top, 6)

                        if store.state.workouts.isEmpty {
                            FormaCard {
                                Text("no_workouts").foregroundStyle(FormaTheme.textMuted)
                            }
                        } else {
                            ForEach(store.state.workouts.reversed()) { workout in
                                let tonnage = FormaTheme.workingTonnage(workout.sets)
                                FormaCard {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(workout.programName)
                                                .font(.headline.weight(.bold))
                                                .foregroundStyle(FormaTheme.textPrimary)
                                            Text("\(workout.date) · \(workout.sets.count) \(String(localized: "sets"))")
                                                .font(.caption)
                                                .foregroundStyle(FormaTheme.textMuted)
                                        }
                                        Spacer()
                                        if tonnage > 0 {
                                            FormaPill(text: "\(Int(tonnage.rounded())) kg")
                                        }
                                    }
                                    HStack(spacing: 12) {
                                        Button("export_workout") { onExport(workout) }
                                            .buttonStyle(.bordered)
                                            .tint(FormaTheme.primary)
                                        Spacer()
                                        Button("delete", role: .destructive) { onDelete(workout.id) }
                                            .font(.subheadline)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("Forma")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { onOpenBridge() } label: { Image(systemName: "applewatch") }
                }
            }
            .sheet(isPresented: $creatingProgram) {
                PhoneProgramEditor { program in
                    store.attempt {
                        try store.saveProgram(program)
                        creatingProgram = false
                    }
                }
                .preferredColorScheme(.dark)
                .tint(FormaTheme.primary)
            }
        }
    }
}

@MainActor private struct PhoneProgramEditor: View {
    let onSave: (WatchProgram) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var name = ""
    @State private var restSeconds = 90
    @State private var selected: [Exercise] = []
    @State private var search = ""
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let filtered = Exercise.allCases.filter { ex in
            search.isEmpty || (ex.label(arabic: false) + " " + ex.label(arabic: true)).localizedStandardContains(search)
        }
        NavigationStack {
            Form {
                Section {
                    TextField("program_name", text: $name)
                    Stepper("\(String(localized: "rest_seconds")): \(restSeconds)s", value: $restSeconds, in: 15...600, step: 15)
                }
                Section("exercise_library") {
                    TextField("search_exercises", text: $search)
                    ForEach(filtered, id: \.self) { ex in
                        let chosen = selected.contains(ex)
                        Button {
                            if chosen { selected.removeAll { $0 == ex } }
                            else if selected.count < 20 { selected.append(ex) }
                        } label: {
                            HStack {
                                Text(ex.label(arabic: arabic)).foregroundStyle(FormaTheme.textPrimary)
                                Spacer()
                                if chosen { Image(systemName: "checkmark.circle.fill").foregroundStyle(FormaTheme.primary) }
                            }
                        }
                    }
                }
            }
            .navigationTitle("new_program")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmed.isEmpty, !selected.isEmpty else { return }
                        onSave(WatchProgram(id: UUID().uuidString, name: trimmed, exercises: selected, restSeconds: restSeconds))
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || selected.isEmpty)
                }
            }
        }
    }
}

@MainActor private struct PhoneWorkoutView: View {
    @ObservedObject var store: LocalStore
    let draft: WorkoutDraft
    @State private var exercise: Exercise = .squat
    @State private var initializedID: String?
    @State private var reps = 8
    @State private var kg = 0.0
    @State private var warmup = false
    @State private var hasRpe = false
    @State private var rpe = 7.0
    @State private var editing: WatchSet?
    @State private var confirmFinish = false
    @State private var confirmDiscard = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    private var workingCounts: [Exercise: Int] {
        var counts: [Exercise: Int] = [:]
        for item in draft.sets where item.kind == .working {
            counts[item.exercise, default: 0] += 1
        }
        return counts
    }

    var body: some View {
        let tonnage = FormaTheme.workingTonnage(draft.sets)
        VStack(alignment: .leading, spacing: 14) {
            // Active Session Header
            FormaCard {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        FormaPill(text: String(localized: "active_workout"))
                        Text(draft.program.name)
                            .font(.title2.weight(.bold))
                            .foregroundStyle(FormaTheme.textPrimary)
                        Text("\(draft.sets.count) \(String(localized: "sets"))")
                            .font(.caption)
                            .foregroundStyle(FormaTheme.textMuted)
                    }
                    Spacer()
                    if tonnage > 0 {
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("\(Int(tonnage.rounded())) kg")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(FormaTheme.primary)
                            Text("volume_kg")
                                .font(.caption2)
                                .foregroundStyle(FormaTheme.textMuted)
                        }
                    }
                }
            }

            // 1-Tap Horizontal Planned Exercise Strip
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(draft.program.exercises, id: \.self) { item in
                        let done = workingCounts[item] ?? 0
                        let plan = draft.program.prescriptions.first(where: { $0.exercise == item })
                        let target = plan?.workingSets
                        let selected = item == exercise
                        Button {
                            if item != exercise {
                                exercise = item
                                kg = 0.0
                                warmup = false
                            }
                        } label: {
                            HStack(spacing: 5) {
                                if let target, done >= target {
                                    Image(systemName: "checkmark.circle.fill")
                                } else if plan?.supersetGroup != nil {
                                    Image(systemName: "link")
                                }
                                Text(item.label(arabic: arabic))
                                if let target {
                                    Text("\(done)/\(target)").fontWeight(.bold)
                                } else if done > 0 {
                                    Text("(\(done))")
                                }
                            }
                            .font(.caption.weight(selected ? .bold : .medium))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? FormaTheme.primary : FormaTheme.surface, in: Capsule())
                            .foregroundStyle(selected ? FormaTheme.onPrimary : FormaTheme.textPrimary)
                            .overlay(Capsule().stroke(FormaTheme.border, lineWidth: selected ? 0 : 1))
                        }
                    }
                }
            }

            // Main Set Logging Card
            FormaCard {
                NavigationLink { ExerciseDetail(exercise: exercise) } label: {
                    HStack(spacing: 12) {
                        Image(ExerciseGuides.of(exercise, arabic: false).asset)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 80)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(exercise.label(arabic: arabic))
                                .font(.headline.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                            if let plan = draft.program.prescriptions.first(where: { $0.exercise == exercise }) {
                                if let min = plan.repMin, let max = plan.repMax {
                                    Text("\(String(localized: "planned_reps")) \(min)–\(max)")
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.primary)
                                }
                                if !plan.notes.isEmpty {
                                    Text(plan.notes).font(.caption2).foregroundStyle(FormaTheme.textMuted)
                                }
                            }
                        }
                        Spacer()
                        Image(systemName: "chevron.right").foregroundStyle(FormaTheme.textMuted)
                    }
                }

                Divider().overlay(FormaTheme.border)

                if let prev = store.state.workouts.reversed().flatMap(\.sets).first(where: { $0.exercise == exercise }) {
                    HStack {
                        Text("\(prev.reps) × \(prev.weightKg.formatted()) kg")
                            .font(.caption)
                            .foregroundStyle(FormaTheme.textMuted)
                        Spacer()
                        Button("copy_previous_set") {
                            reps = prev.reps
                            kg = prev.weightKg
                            warmup = prev.kind == .warmUp
                            if let r = prev.rpe { hasRpe = true; rpe = r }
                        }
                        .font(.caption.weight(.semibold))
                    }
                }

                if !warmup && FormaTheme.isNewWeightPr(workouts: store.state.workouts, exercise: exercise, weightKg: kg) {
                    FormaPill(
                        text: "★ \(String(localized: "new_pr_badge"))",
                        background: FormaTheme.secondary.opacity(0.22),
                        foreground: FormaTheme.secondary
                    )
                }

                Stepper(value: $reps, in: 1...1000) {
                    Text("\(String(localized: "reps")): \(reps)")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(FormaTheme.textPrimary)
                }
                Stepper(value: $kg, in: 0...1000, step: 0.5) {
                    Text("\(kg.formatted()) kg")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(FormaTheme.textPrimary)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach([5, 8, 10, 12], id: \.self) { preset in
                            Button { reps = preset } label: {
                                FormaPill(text: "\(preset)×", background: FormaTheme.surfaceHigh, foreground: FormaTheme.textPrimary)
                            }
                        }
                        ForEach([2.5, 5.0, 10.0], id: \.self) { bump in
                            Button { kg = min(1000.0, kg + bump) } label: {
                                FormaPill(text: "+\(bump.formatted()) kg")
                            }
                        }
                    }
                }
                if ExerciseCatalog.of(exercise).equipment.contains(.barbell) && kg >= 20.0 {
                    FormaBarbellView(totalKg: kg)
                    let ramp = FormaTheme.warmupRamp(workingKg: kg)
                    if !ramp.isEmpty {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("warmup_ramp_title")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(FormaTheme.textMuted)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 6) {
                                    ForEach(Array(ramp.enumerated()), id: \.offset) { _, step in
                                        FormaPill(
                                            text: "\(step.weightKg.formatted()) kg × \(step.reps)",
                                            background: FormaTheme.surfaceHigh,
                                            foreground: FormaTheme.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Toggle("warmup", isOn: $warmup).tint(FormaTheme.secondary)
                Toggle("record_rpe", isOn: $hasRpe).tint(FormaTheme.tertiary)
                if hasRpe {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach([7.0, 8.0, 9.0, 10.0], id: \.self) { val in
                                let rir = Int(10.0 - val)
                                let sel = abs(rpe - val) < 0.01
                                Button { rpe = val } label: {
                                    FormaPill(
                                        text: "RPE \(Int(val)) · \(rir) RIR",
                                        background: sel ? FormaTheme.tertiary : FormaTheme.surfaceHigh,
                                        foreground: sel ? FormaTheme.onPrimary : FormaTheme.textMuted
                                    )
                                }
                            }
                        }
                    }
                    Stepper(value: $rpe, in: 0...10, step: 0.5) {
                        Text("RPE \(rpe.formatted())").foregroundStyle(FormaTheme.tertiary)
                    }
                }

                Button {
                    let item = WatchSet(
                        id: UUID().uuidString,
                        exercise: exercise,
                        reps: reps,
                        weightKg: kg,
                        kind: warmup ? .warmUp : .working,
                        rpe: hasRpe ? rpe : nil
                    )
                    store.attempt {
                        try store.add(item)
                        let next = WorkoutFlow.afterSet(program: draft.program, sets: draft.sets + [item], current: item.exercise, kind: item.kind)
                        if let following = next.exercise, following != exercise {
                            exercise = following
                            kg = 0.0
                            warmup = false
                        }
                    }
                } label: {
                    HStack {
                        Image(systemName: "checkmark")
                        Text("add_set").fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(FormaTheme.onPrimary)
                }
                .disabled(draft.sets.count >= 300)

                if let deadline = draft.restUntil {
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        let remaining = max(0, Int(ceil(deadline.timeIntervalSince(context.date))))
                        let total = max(1, draft.program.restSeconds)
                        HStack(spacing: 14) {
                            FormaProgressRing(
                                progress: Double(remaining) / Double(total),
                                valueText: "\(remaining)s",
                                size: 68,
                                lineWidth: 7,
                                color: FormaTheme.primary
                            )
                            VStack(alignment: .leading, spacing: 8) {
                                Text("\(String(localized: "rest")) · \(remaining)s")
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(FormaTheme.primary)
                                HStack(spacing: 8) {
                                    Button("reduce_rest") { store.attempt { try store.extendRest(seconds: -15) } }
                                        .buttonStyle(.bordered)
                                    Button("extend_rest") { store.attempt { try store.extendRest(seconds: 15) } }
                                        .buttonStyle(.bordered)
                                    Button("stop_rest") { store.attempt { try store.stopRest() } }
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.textMuted)
                                }
                            }
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(FormaTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                }
            }

            // Logged Sets
            if !draft.sets.isEmpty {
                FormaCard {
                    ForEach(draft.sets.reversed()) { set in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(set.exercise.label(arabic: arabic))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(FormaTheme.textPrimary)
                                HStack(spacing: 6) {
                                    Text("\(set.reps) × \(set.weightKg.formatted()) kg")
                                        .font(.caption.weight(.bold))
                                        .foregroundStyle(FormaTheme.primary)
                                    FormaPill(
                                        text: set.kind == .warmUp ? String(localized: "warmup") : String(localized: "working"),
                                        background: set.kind == .warmUp ? FormaTheme.secondary.opacity(0.2) : FormaTheme.primaryContainer,
                                        foreground: set.kind == .warmUp ? FormaTheme.secondary : FormaTheme.primary
                                    )
                                    if let r = set.rpe {
                                        FormaPill(text: "RPE \(r.formatted())", background: FormaTheme.tertiary.opacity(0.2), foreground: FormaTheme.tertiary)
                                    }
                                }
                            }
                            Spacer()
                            Button { editing = set } label: {
                                Image(systemName: "pencil").foregroundStyle(FormaTheme.textMuted)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }

            Button {
                confirmFinish = true
            } label: {
                Text("finish_workout")
                    .fontWeight(.bold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(FormaTheme.tertiary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(FormaTheme.onPrimary)
            }
            .disabled(draft.sets.isEmpty)

            Button("discard_workout", role: .destructive) { confirmDiscard = true }
                .frame(maxWidth: .infinity)
        }
        .onAppear {
            if initializedID != draft.id {
                exercise = draft.sets.last.map {
                    WorkoutFlow.afterSet(program: draft.program, sets: draft.sets, current: $0.exercise, kind: $0.kind).exercise ?? $0.exercise
                } ?? draft.program.exercises.first ?? .squat
                initializedID = draft.id
            }
        }
        .confirmationDialog("finish_phone_confirm", isPresented: $confirmFinish, titleVisibility: .visible) {
            Button("finish_workout") { store.attempt { try store.finish(queueForPhone: false) } }
        }
        .confirmationDialog("discard_confirm", isPresented: $confirmDiscard, titleVisibility: .visible) {
            Button("discard_workout", role: .destructive) { store.attempt { try store.discardDraft() } }
        }
        .sheet(item: $editing) { item in
            PhoneSetEditor(set: item, onSave: { updated in
                store.attempt { try store.edit(updated); editing = nil }
            }, onDelete: {
                store.attempt { try store.removeSet(item.id); editing = nil }
            })
            .preferredColorScheme(.dark)
            .tint(FormaTheme.primary)
        }
    }
}

@MainActor private struct PhoneSetEditor: View {
    let set: WatchSet
    let onSave: (WatchSet) -> Void
    let onDelete: () -> Void
    @State private var reps: Int
    @State private var kg: Double
    @State private var warmup: Bool
    @State private var hasRpe: Bool
    @State private var rpe: Double

    init(set: WatchSet, onSave: @escaping (WatchSet) -> Void, onDelete: @escaping () -> Void) {
        self.set = set
        self.onSave = onSave
        self.onDelete = onDelete
        _reps = State(initialValue: set.reps)
        _kg = State(initialValue: set.weightKg)
        _warmup = State(initialValue: set.kind == .warmUp)
        _hasRpe = State(initialValue: set.rpe != nil)
        _rpe = State(initialValue: set.rpe ?? 7.0)
    }

    var body: some View {
        NavigationStack {
            Form {
                Stepper("\(String(localized: "reps")) \(reps)", value: $reps, in: 1...1000)
                Stepper("\(kg.formatted()) kg", value: $kg, in: 0...1000, step: 0.5)
                Toggle("warmup", isOn: $warmup)
                Toggle("record_rpe", isOn: $hasRpe)
                if hasRpe { Stepper("RPE \(rpe.formatted())", value: $rpe, in: 0...10, step: 0.5) }
                Button("save") {
                    onSave(WatchSet(id: set.id, exercise: set.exercise, reps: reps, weightKg: kg, kind: warmup ? .warmUp : .working, rpe: hasRpe ? rpe : nil))
                }
                Button("delete", role: .destructive, action: onDelete)
            }
            .navigationTitle("Forma")
        }
    }
}

@MainActor private struct PhoneExercisesTab: View {
    let onOpenBridge: () -> Void
    @State private var exerciseSearch = ""
    @State private var equipment: Equipment?
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }
    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        let filtered = Exercise.allCases.filter { exercise in
            (equipment == nil || ExerciseCatalog.of(exercise).equipment.contains(equipment!)) &&
                (exerciseSearch.isEmpty || (exercise.label(arabic: false) + " " + exercise.label(arabic: true)).localizedStandardContains(exerciseSearch))
        }
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    TextField("search_exercises", text: $exerciseSearch)
                        .padding(12)
                        .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border, lineWidth: 1))

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            Button { equipment = nil } label: {
                                FormaPill(
                                    text: String(localized: "all_equipment"),
                                    background: equipment == nil ? FormaTheme.primary : FormaTheme.surface,
                                    foreground: equipment == nil ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                            ForEach(Equipment.allCases, id: \.self) { item in
                                Button { equipment = item } label: {
                                    FormaPill(
                                        text: item.label(arabic: arabic),
                                        background: equipment == item ? FormaTheme.primary : FormaTheme.surface,
                                        foreground: equipment == item ? FormaTheme.onPrimary : FormaTheme.textMuted
                                    )
                                }
                            }
                        }
                    }

                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(filtered, id: \.self) { exercise in
                            NavigationLink { ExerciseDetail(exercise: exercise) } label: {
                                VStack(alignment: .leading, spacing: 8) {
                                    Image(ExerciseGuides.of(exercise, arabic: false).asset)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(maxWidth: .infinity)
                                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    Text(exercise.label(arabic: arabic))
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(FormaTheme.textPrimary)
                                        .lineLimit(2)
                                        .multilineTextAlignment(.leading)
                                    if let primary = ExerciseMuscles.of(exercise).primary.first {
                                        FormaPill(text: primary.label(arabic: arabic))
                                    }
                                }
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(FormaTheme.border.opacity(0.75), lineWidth: 1))
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("exercise_library")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { onOpenBridge() } label: { Image(systemName: "applewatch") }
                }
            }
        }
    }
}

@MainActor private struct PhoneEatTab: View {
    @ObservedObject var store: LocalStore
    let onOpenBridge: () -> Void
    @State private var selectedSlot = "BREAKFAST"
    @State private var addingMeal = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    private var starterQuickFoods: [(en: String, ar: String, kcal: Double, p: Double, c: Double, f: Double)] {
        [
            ("Chicken breast 150g", "صدر دجاج ١٥٠ جم", 248, 46.5, 0, 5.4),
            ("2 Whole eggs", "٢ بيضة كاملة", 144, 12.6, 0.8, 9.6),
            ("Greek yogurt 170g", "زبادي يوناني ١٧٠ جم", 124, 17, 6.6, 3.2),
            ("Whey scoop", "واي بروتين سكوب", 120, 24, 3, 1.5),
            ("White rice 200g", "أرز أبيض ٢٠٠ جم", 260, 5.4, 56.4, 0.6),
            ("Rolled oats 60g", "شوفان ٦٠ جم", 233, 10.1, 39.8, 4.1),
            ("Fava beans 150g", "فول مدمس ١٥٠ جم", 165, 11.4, 29.6, 0.6),
            ("Cottage cheese 150g", "جبنة قريش ١٥٠ جم", 123, 16.7, 5.1, 3.5),
            ("Banana", "موزة", 105, 1.3, 27, 0.4),
        ]
    }

    var body: some View {
        let today = LocalStore.todayString()
        let meals = store.state.meals.filter { $0.date == today }
        let totalKcal = meals.reduce(0.0) { $0 + $1.calories }
        let totalP = meals.reduce(0.0) { $0 + $1.protein }
        let totalC = meals.reduce(0.0) { $0 + $1.carbs }
        let totalF = meals.reduce(0.0) { $0 + $1.fat }
        let calGoal = Double(store.state.calorieTarget ?? 2200)
        let proGoal = store.state.proteinTarget ?? 150.0

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    FormaCard {
                        Text("food_today").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                        HStack(spacing: 18) {
                            Spacer()
                            VStack(spacing: 6) {
                                FormaProgressRing(
                                    progress: totalKcal / max(1, calGoal),
                                    valueText: "\(Int(totalKcal.rounded()))",
                                    labelText: "kcal",
                                    size: 92,
                                    lineWidth: 9,
                                    color: FormaTheme.primary
                                )
                                Text("calories_label").font(.caption).foregroundStyle(FormaTheme.textMuted)
                            }
                            Spacer()
                            VStack(spacing: 6) {
                                FormaProgressRing(
                                    progress: totalP / max(1, proGoal),
                                    valueText: "\(Int(totalP.rounded()))g",
                                    labelText: "/ \(Int(proGoal))g",
                                    size: 92,
                                    lineWidth: 9,
                                    color: FormaTheme.secondary
                                )
                                Text("protein_label").font(.caption).foregroundStyle(FormaTheme.textMuted)
                            }
                            Spacer()
                        }
                        HStack(spacing: 8) {
                            FormaPill(text: "\(String(localized: "protein_label")) \(Int(totalP.rounded()))g", background: FormaTheme.secondary.opacity(0.2), foreground: FormaTheme.secondary)
                            FormaPill(text: "\(String(localized: "carbs_label")) \(Int(totalC.rounded()))g", background: FormaTheme.tertiary.opacity(0.2), foreground: FormaTheme.tertiary)
                            FormaPill(text: "\(String(localized: "fat_label")) \(Int(totalF.rounded()))g")
                        }
                        Button { addingMeal = true } label: {
                            HStack {
                                Image(systemName: "plus")
                                Text("log_meal").fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .foregroundStyle(FormaTheme.onPrimary)
                        }
                    }

                    // Meal slot selector
                    let slotOptions = [
                        MealSlotOption(id: "BREAKFAST", labelKey: "meal_breakfast"),
                        MealSlotOption(id: "LUNCH", labelKey: "meal_lunch"),
                        MealSlotOption(id: "DINNER", labelKey: "meal_dinner"),
                        MealSlotOption(id: "SNACK", labelKey: "meal_snack"),
                    ]
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(slotOptions) { option in
                                let active = selectedSlot == option.id
                                Button { selectedSlot = option.id } label: {
                                    FormaPill(
                                        text: String(localized: String.LocalizationValue(option.labelKey)),
                                        background: active ? FormaTheme.primary : FormaTheme.surface,
                                        foreground: active ? FormaTheme.onPrimary : FormaTheme.textMuted
                                    )
                                }
                            }
                        }
                    }

                    // 1-Tap Quick-Add Whole Foods
                    Text("quick_add_foods").font(.subheadline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(Array(starterQuickFoods.enumerated()), id: \.offset) { _, item in
                                Button {
                                    store.attempt {
                                        try store.addMeal(PhoneMeal(
                                            date: today,
                                            name: arabic ? item.ar : item.en,
                                            calories: item.kcal,
                                            protein: item.p,
                                            carbs: item.c,
                                            fat: item.f,
                                            slot: selectedSlot
                                        ))
                                    }
                                } label: {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(arabic ? item.ar : item.en)
                                            .font(.caption.weight(.bold))
                                            .foregroundStyle(FormaTheme.textPrimary)
                                            .lineLimit(1)
                                        Text("\(Int(item.kcal)) kcal · \(Int(item.p))g P")
                                            .font(.caption2)
                                            .foregroundStyle(FormaTheme.primary)
                                    }
                                    .padding(10)
                                    .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border.opacity(0.75), lineWidth: 1))
                                }
                            }
                        }
                    }

                    if meals.isEmpty {
                        FormaCard { Text("no_meals_today").foregroundStyle(FormaTheme.textMuted) }
                    } else {
                        ForEach(meals.reversed()) { meal in
                            FormaCard {
                                HStack {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(meal.name).font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                                        Text("\(Int(meal.protein))g P · \(Int(meal.carbs))g C · \(Int(meal.fat))g F")
                                            .font(.caption)
                                            .foregroundStyle(FormaTheme.textMuted)
                                    }
                                    Spacer()
                                    FormaPill(text: "\(Int(meal.calories.rounded())) kcal")
                                    Button(role: .destructive) { store.attempt { try store.deleteMeal(meal.id) } } label: {
                                        Image(systemName: "trash").font(.caption)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("tab_eat")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { onOpenBridge() } label: { Image(systemName: "applewatch") }
                }
            }
            .sheet(isPresented: $addingMeal) {
                PhoneMealEditor(slot: selectedSlot) { meal in
                    store.attempt { try store.addMeal(meal); addingMeal = false }
                }
                .preferredColorScheme(.dark)
                .tint(FormaTheme.primary)
            }
        }
    }
}

@MainActor private struct PhoneMealEditor: View {
    let slot: String
    let onSave: (PhoneMeal) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var kcal = 350.0
    @State private var protein = 30.0
    @State private var carbs = 35.0
    @State private var fat = 10.0

    var body: some View {
        NavigationStack {
            Form {
                TextField("meal_name", text: $name)
                Stepper("\(String(localized: "calories_label")): \(Int(kcal))", value: $kcal, in: 0...5000, step: 25)
                Stepper("\(String(localized: "protein_label")): \(Int(protein))g", value: $protein, in: 0...500, step: 2)
                Stepper("\(String(localized: "carbs_label")): \(Int(carbs))g", value: $carbs, in: 0...500, step: 5)
                Stepper("\(String(localized: "fat_label")): \(Int(fat))g", value: $fat, in: 0...300, step: 2)
                Button("calc_from_macros") {
                    kcal = (protein * 4.0) + (carbs * 4.0) + (fat * 9.0)
                }
            }
            .navigationTitle("log_meal")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmed.isEmpty else { return }
                        onSave(PhoneMeal(date: LocalStore.todayString(), name: trimmed, calories: kcal, protein: protein, carbs: carbs, fat: fat, slot: slot))
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

@MainActor private struct PhoneProgressTab: View {
    @ObservedObject var store: LocalStore
    let onOpenBridge: () -> Void
    @State private var weightKg = 75.0
    @State private var initializedWeight = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Quick Weight Logger Card
                    FormaCard {
                        HStack {
                            Text("body_weight").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            if let latest = store.state.weights.last {
                                FormaPill(text: "\(latest.weightKg.formatted()) kg")
                            }
                        }
                        Stepper(value: $weightKg, in: 20...300, step: 0.5) {
                            Text("\(weightKg.formatted()) kg")
                                .font(.title2.weight(.heavy))
                                .foregroundStyle(FormaTheme.primary)
                        }
                        Button {
                            store.attempt { try store.saveWeight(weightKg) }
                        } label: {
                            Text("save_weight")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .foregroundStyle(FormaTheme.onPrimary)
                        }
                    }

                    // Gradient Weight Trend Chart
                    FormaCard {
                        Text("weight_trend").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                        let points = Array(store.state.weights.suffix(30))
                        if points.count < 2 {
                            Text("no_weights").font(.caption).foregroundStyle(FormaTheme.textMuted)
                        } else {
                            let minW = points.map(\.weightKg).min() ?? 0
                            let maxW = points.map(\.weightKg).max() ?? 1
                            let span = max(0.5, maxW - minW)
                            GeometryReader { geo in
                                let w = geo.size.width
                                let h = geo.size.height
                                let coords: [CGPoint] = points.enumerated().map { idx, item in
                                    let x = w * CGFloat(idx) / CGFloat(max(1, points.count - 1))
                                    let norm = CGFloat((item.weightKg - minW) / span)
                                    let y = h - norm * (h - 20) - 10
                                    return CGPoint(x: x, y: y)
                                }
                                ZStack {
                                    Path { p in
                                        guard let first = coords.first, let last = coords.last else { return }
                                        p.move(to: CGPoint(x: first.x, y: h))
                                        coords.forEach { p.addLine(to: $0) }
                                        p.addLine(to: CGPoint(x: last.x, y: h))
                                        p.closeSubpath()
                                    }
                                    .fill(LinearGradient(colors: [FormaTheme.primary.opacity(0.32), .clear], startPoint: .top, endPoint: .bottom))

                                    Path { p in
                                        coords.enumerated().forEach { idx, pt in
                                            if idx == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                                        }
                                    }
                                    .stroke(FormaTheme.primary, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))

                                    ForEach(Array(coords.enumerated()), id: \.offset) { _, pt in
                                        Circle()
                                            .fill(FormaTheme.primary)
                                            .frame(width: 7, height: 7)
                                            .position(pt)
                                    }
                                }
                            }
                            .frame(height: 150)
                        }
                    }

                    // 7-Day Body Muscle Heat Map
                    let muscleCounts: [Muscle: Int] = {
                        var counts: [Muscle: Int] = [:]
                        for workout in store.state.workouts.suffix(7) {
                            for set in workout.sets where set.kind == .working {
                                let act = ExerciseMuscles.of(set.exercise)
                                for m in act.primary + act.secondary { counts[m, default: 0] += 1 }
                            }
                        }
                        return counts
                    }()
                    FormaCard { WeeklyMuscleHeatMapView(setsByMuscle: muscleCounts) }

                    // 12-Week GitHub-Style Training Grid + Streak
                    let countsByDate: [String: Int] = {
                        var map: [String: Int] = [:]
                        for w in store.state.workouts { map[w.date, default: 0] += 1 }
                        return map
                    }()
                    let streak = FormaTheme.activeWeekStreak(workouts: store.state.workouts)
                    FormaCard {
                        HStack {
                            Text("activity_heatmap_title")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            if streak > 0 {
                                FormaPill(text: "\(streak) \(String(localized: "week_streak_label"))")
                            }
                        }
                        FormaActivityHeatmap(countsByDate: countsByDate)
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("tab_progress")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { onOpenBridge() } label: { Image(systemName: "applewatch") }
                }
            }
            .onAppear {
                if !initializedWeight, let latest = store.state.weights.last {
                    weightKg = latest.weightKg
                    initializedWeight = true
                }
            }
        }
    }
}

@MainActor private struct PhoneBridgeTab: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var bridge: WatchBridge
    let onImport: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    FormaCard {
                        FormaPill(
                            text: bridge.activated ? String(localized: "watch_connected") : String(localized: "watch_disconnected"),
                            background: bridge.activated ? FormaTheme.primaryContainer : FormaTheme.surfaceHigh,
                            foreground: bridge.activated ? FormaTheme.primary : FormaTheme.textMuted
                        )
                        Text("phone_bridge_note")
                            .font(.subheadline)
                            .foregroundStyle(FormaTheme.textPrimary)
                        Text("transfer_privacy")
                            .font(.caption)
                            .foregroundStyle(FormaTheme.textMuted)
                        Button {
                            onImport()
                        } label: {
                            Text("import_programs")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .foregroundStyle(FormaTheme.onPrimary)
                        }
                        Button {
                            store.attempt { try bridge.sendLibrary() }
                        } label: {
                            Text("send_watch")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .foregroundStyle(FormaTheme.textPrimary)
                        }
                        .disabled(!bridge.activated)
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("tab_bridge")
        }
    }
}
