import SwiftUI
import FormaCore

@main @MainActor struct FormaPhoneApp: App {
    @StateObject private var store: LocalStore
    @StateObject private var bridge: WatchBridge
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @AppStorage("forma_lang") private var appLang = ""
    init() {
        let storage = LocalStore()
        _store = StateObject(wrappedValue: storage)
        _bridge = StateObject(wrappedValue: WatchBridge(store: storage))
    }
    var body: some Scene {
        let scheme = FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme
        WindowGroup {
            PhoneRoot(store: store, bridge: bridge)
                .preferredColorScheme(scheme)
                .environment(\.locale, appLang.isEmpty ? .current : Locale(identifier: appLang))
                .environment(\.layoutDirection, appLang == "ar" ? .rightToLeft : (appLang == "en" ? .leftToRight : (Locale.current.language.languageCode?.identifier == "ar" ? .rightToLeft : .leftToRight)))
                .tint(FormaTheme.primary)
        }
    }
}

@MainActor private struct PhoneRoot: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var bridge: WatchBridge
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @State private var deleting: String?
    @State private var showBridge = false
    @State private var showSettings = false
    @State private var selectedTab = 0
    @StateObject private var health = HealthImporter()

    var body: some View {
        Group {
            if store.failedToLoad {
                ZStack {
                    FormaTheme.background.ignoresSafeArea()
                    FormaCard { Text("storage_failed").foregroundStyle(FormaTheme.textPrimary) }
                        .padding()
                }
            } else {
                TabView(selection: $selectedTab) {
                    PhoneExercisesTab(
                        store: store,
                        onOpenBridge: { showSettings = true },
                        onOpenTrain: { selectedTab = 1 }
                    )
                    .tabItem { Label("tab_exercises", systemImage: "figure.strengthtraining.traditional") }
                    .tag(0)

                    PhoneTrainTab(
                        store: store,
                        onOpenBridge: { showSettings = true },
                        onDelete: { deleting = $0 },
                        onOpenProgress: { selectedTab = 3 }
                    )
                    .tabItem { Label("tab_train", systemImage: "dumbbell.fill") }
                    .tag(1)

                    PhoneEatTab(store: store, onOpenBridge: { showSettings = true })
                        .tabItem { Label("tab_eat", systemImage: "fork.knife") }
                        .tag(2)

                    PhoneProgressTab(store: store, health: health, onOpenBridge: { showSettings = true })
                        .tabItem { Label("tab_progress", systemImage: "chart.xyaxis.line") }
                        .tag(3)
                }
                .tint(FormaTheme.primary)
            }
        }
        .sheet(isPresented: $showSettings) {
            PhoneSettingsSheet(store: store, onOpenBridge: { showBridge = true })
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
        }
        .sheet(isPresented: $showBridge) {
            PhoneBridgeTab(store: store, bridge: bridge)
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
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
    let onDelete: (String) -> Void
    let onOpenProgress: () -> Void
    @State private var creatingProgram = false
    @State private var finishedSummary: WatchWorkout?
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @AppStorage("forma_use_lb") private var useLb = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let draft = store.state.draft {
                        PhoneWorkoutView(store: store, draft: draft) { completed in
                            finishedSummary = completed
                        }
                    } else {
                        // Warm Daily Narrative Reflection Card
                        let todayStr = LocalStore.todayString()
                        let todayWorkouts = store.state.workouts.filter { $0.date == todayStr }
                        let todayWorkingSets = todayWorkouts.flatMap(\.sets).filter { $0.kind == .working }.count
                        let todayKcal = store.state.meals.filter { $0.date == todayStr }.reduce(0.0) { $0 + $1.calories }
                        let storyKey: String = {
                            if todayWorkingSets > 0 && todayKcal > 0 { return "daily_story_complete" }
                            if todayWorkingSets > 0 { return "daily_story_trained" }
                            if todayKcal > 0 { return "daily_story_fueled" }
                            return "daily_story_fresh"
                        }()
                        FormaCard {
                            HStack(spacing: 10) {
                                Image(systemName: "sparkles")
                                    .foregroundStyle(FormaTheme.secondary)
                                Text(LocalizedStringKey(storyKey))
                                    .font(.subheadline)
                                    .foregroundStyle(FormaTheme.textPrimary)
                            }
                        }
                        if store.state.workouts.isEmpty && store.state.healthLastSync == nil && HealthImporter.isAvailable {
                            FormaCard {
                                HStack(spacing: 10) {
                                    Image(systemName: "heart.fill").foregroundStyle(.pink)
                                    Text("health_first_title").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                                }
                                Text("health_first_body").font(.caption).foregroundStyle(FormaTheme.textMuted)
                                Button { onOpenProgress() } label: {
                                    Text("health_connect").fontWeight(.bold)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(FormaTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                        .foregroundStyle(FormaTheme.primary)
                                }
                            }
                        }
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
                                        Text("\(String(localized: "volume_kg")): \(FormaTheme.formatWeight(totalVolume, useLb: useLb))")
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
                                            Text("\(FormaTheme.formatWeight(item.weight, useLb: useLb)) × \(item.reps)")
                                                .font(.headline.weight(.heavy))
                                                .foregroundStyle(FormaTheme.primary)
                                            if let est = item.e1rm {
                                                FormaPill(
                                                    text: "\(String(localized: "best_e1rm")) \(FormaTheme.formatWeight(est, useLb: useLb))",
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

                        Button {
                            FormaHaptics.success()
                            store.attempt { try store.startFreestyle(label: String(localized: "freestyle_workout_name")) }
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: "bolt.fill")
                                Text("start_freestyle_workout").fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(FormaTheme.secondary.opacity(0.18), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.secondary.opacity(0.45), lineWidth: 1))
                            .foregroundStyle(FormaTheme.secondary)
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
                                        FormaExerciseImage(asset: ExerciseGuides.of(first, arabic: false).asset, maxPixelSize: 320)
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
                                    FormaHaptics.success()
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
                                            FormaPill(text: FormaTheme.formatWeight(tonnage, useLb: useLb))
                                        }
                                    }
                                    HStack(spacing: 12) {
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
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        FormaHaptics.tap()
                        useLb.toggle()
                    } label: {
                        FormaPill(text: useLb ? String(localized: "unit_lb") : String(localized: "unit_kg"))
                    }
                    Button { onOpenBridge() } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel(Text("settings_title"))
                }
            }
            .sheet(isPresented: $creatingProgram) {
                PhoneProgramEditor { program in
                    store.attempt {
                        try store.saveProgram(program)
                        creatingProgram = false
                    }
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
            .sheet(item: $finishedSummary) { summary in
                PhoneWorkoutSummarySheet(workout: summary)
                    .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                    .tint(FormaTheme.primary)
            }
        }
    }
}

@MainActor private struct PhoneWorkoutSummarySheet: View {
    let workout: WatchWorkout
    @AppStorage("forma_use_lb") private var useLb = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let tonnage = FormaTheme.workingTonnage(workout.sets)
        let workingSets = workout.sets.filter { $0.kind == .working }.count
        let muscleMap: [Muscle: Int] = {
            var m: [Muscle: Int] = [:]
            for s in workout.sets where s.kind == .working {
                let act = ExerciseMuscles.of(s.exercise)
                for muscle in act.primary + act.secondary { m[muscle, default: 0] += 1 }
            }
            return m
        }()
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    FormaCard {
                        HStack(spacing: 12) {
                            Image(systemName: "trophy.fill")
                                .font(.title)
                                .foregroundStyle(FormaTheme.primary)
                            VStack(alignment: .leading, spacing: 3) {
                                Text("workout_summary").font(.title2.weight(.heavy)).foregroundStyle(FormaTheme.textPrimary)
                                Text(workout.programName).font(.subheadline).foregroundStyle(FormaTheme.textMuted)
                            }
                        }
                        HStack(spacing: 10) {
                            FormaPill(text: "\(workingSets) \(String(localized: "sets"))")
                            if tonnage > 0 {
                                FormaPill(text: FormaTheme.formatWeight(tonnage, useLb: useLb), background: FormaTheme.secondary.opacity(0.2), foreground: FormaTheme.secondary)
                            }
                        }
                    }
                    if !muscleMap.isEmpty {
                        FormaCard {
                            Text("session_muscles_hit").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                            WeeklyMuscleHeatMapView(setsByMuscle: muscleMap)
                        }
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("Forma")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("ok") { dismiss() }
                }
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
    var onFinished: (WatchWorkout) -> Void = { _ in }
    @State private var exercise: Exercise = .squat
    @State private var initializedID: String?
    @State private var reps = 8
    @State private var kg = 0.0
    @State private var warmup = false
    @State private var hasRpe = false
    @State private var rpe = 7.0
    @State private var editing: WatchSet?
    @State private var showSwap = false
    @State private var confirmFinish = false
    @State private var confirmDiscard = false
    @AppStorage("forma_use_lb") private var useLb = false
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
                        HStack(spacing: 6) {
                            FormaPulseDot()
                            FormaPill(text: String(localized: "active_workout"))
                        }
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
                            Text(FormaTheme.formatWeight(tonnage, useLb: useLb))
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
                HStack(spacing: 8) {
                    NavigationLink { ExerciseDetail(exercise: exercise) } label: {
                        HStack(spacing: 12) {
                            FormaExerciseImage(asset: ExerciseGuides.of(exercise, arabic: false).asset, maxPixelSize: 320)
                                .frame(width: 72)
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
                            Spacer(minLength: 4)
                        }
                    }
                    Button {
                        FormaHaptics.tap()
                        showSwap = true
                    } label: {
                        Image(systemName: "arrow.left.arrow.right")
                            .font(.subheadline.weight(.bold))
                            .padding(10)
                            .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .foregroundStyle(FormaTheme.primary)
                    }
                    .accessibilityLabel("swap_exercise")
                }

                // Current Exercise Set Checklist
                let exSets = draft.sets.filter { $0.exercise == exercise }
                let exTarget = draft.program.prescriptions.first(where: { $0.exercise == exercise })?.workingSets ?? 0
                let exWorkingDone = exSets.filter { $0.kind == .working }.count
                if !exSets.isEmpty || exTarget > 0 {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("current_exercise_sets")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(FormaTheme.textMuted)
                        ForEach(Array(exSets.enumerated()), id: \.element.id) { idx, logged in
                            HStack(spacing: 8) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.caption)
                                    .foregroundStyle(logged.kind == .warmUp ? FormaTheme.textMuted : FormaTheme.primary)
                                Text(logged.kind == .warmUp ? String(localized: "warmup") : "#\(idx + 1)")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(FormaTheme.textMuted)
                                Text("\(FormaTheme.formatWeight(logged.weightKg, useLb: useLb)) × \(logged.reps)")
                                    .font(.subheadline.weight(.bold))
                                    .monospacedDigit()
                                    .foregroundStyle(FormaTheme.textPrimary)
                                Spacer()
                                if let rpeVal = logged.rpe {
                                    FormaPill(text: "RPE \(rpeVal.formatted())")
                                }
                            }
                        }
                        if exTarget > exWorkingDone {
                            ForEach((exWorkingDone + 1)...min(exTarget, exWorkingDone + 4), id: \.self) { slotNum in
                                HStack(spacing: 8) {
                                    Image(systemName: "circle")
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.textMuted)
                                    Text("\(String(localized: "upcoming_set_prefix")) \(slotNum)")
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.textMuted)
                                }
                            }
                        }
                    }
                    .padding(10)
                    .background(FormaTheme.surfaceHigh.opacity(0.6), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }

                Divider().overlay(FormaTheme.border)

                if let prev = store.state.workouts.reversed().flatMap(\.sets).first(where: { $0.exercise == exercise }) {
                    HStack {
                        Text("\(prev.reps) × \(FormaTheme.formatWeight(prev.weightKg, useLb: useLb))")
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

                if !warmup && FormaTheme.isNewPr(workouts: store.state.workouts, exercise: exercise, weightKg: kg, reps: reps) {
                    FormaPill(
                        text: "★ \(String(localized: "new_pr_badge"))",
                        background: FormaTheme.secondary.opacity(0.22),
                        foreground: FormaTheme.secondary
                    )
                }

                FormaGymStepper(
                    title: String(localized: "reps"),
                    formattedValue: "\(reps)",
                    onDecrement: { reps = max(1, reps - 1) },
                    onIncrement: { reps = min(1000, reps + 1) },
                    onDirectCommit: { val in reps = min(1000, max(1, Int(val.rounded()))) }
                )
                let stepKg = useLb ? (2.5 / FormaTheme.lbPerKg) : 0.5
                FormaGymStepper(
                    title: useLb ? String(localized: "unit_lb") : String(localized: "unit_kg"),
                    formattedValue: FormaTheme.formatWeight(kg, useLb: useLb),
                    onDecrement: { kg = max(0.0, ((kg - stepKg) * 1000.0).rounded() / 1000.0) },
                    onIncrement: { kg = min(1000.0, ((kg + stepKg) * 1000.0).rounded() / 1000.0) },
                    onDirectCommit: { val in
                        let inKg = useLb ? (val / FormaTheme.lbPerKg) : val
                        kg = min(1000.0, max(0.0, (inKg * 1000.0).rounded() / 1000.0))
                    }
                )
                if ExerciseCatalog.of(exercise).equipment.contains(.dumbbells) {
                    let perSide = ExerciseCatalog.of(exercise).perSide
                    // One dumbbell held in both hands (e.g. goblet squat) is never doubled into a "pair".
                    if ExerciseCatalog.of(exercise).singleDumbbell {
                        Text("dumbbell_single_hint")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(FormaTheme.secondary)
                    } else if kg > 0 && !perSide {
                        Text("\(String(localized: "dumbbell_per_hand_hint")) · \(String(localized: "dumbbell_pair_total")) \(FormaTheme.formatWeight(kg * 2.0, useLb: useLb))")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(FormaTheme.secondary)
                    } else {
                        Text("dumbbell_per_hand_hint")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(FormaTheme.secondary)
                    }
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        if let lastSet = exSets.last {
                            Button {
                                FormaHaptics.tap()
                                reps = lastSet.reps
                                kg = lastSet.weightKg
                                warmup = lastSet.kind == .warmUp
                            } label: {
                                FormaPill(
                                    text: String(localized: "same_as_last_set"),
                                    background: FormaTheme.secondary.opacity(0.18),
                                    foreground: FormaTheme.secondary
                                )
                            }
                        }
                        ForEach([5, 8, 10, 12], id: \.self) { preset in
                            Button { FormaHaptics.tap(); reps = preset } label: {
                                FormaPill(text: "\(preset)×", background: FormaTheme.surfaceHigh, foreground: FormaTheme.textPrimary)
                            }
                        }
                        let minusBump = useLb ? 5.0 : 2.5
                        Button {
                            FormaHaptics.tap()
                            let deltaKg = useLb ? (minusBump / 2.20462262185) : minusBump
                            kg = max(0.0, ((kg - deltaKg) * 1000.0).rounded() / 1000.0)
                        } label: {
                            FormaPill(
                                text: "-\(minusBump.formatted()) \(useLb ? String(localized: "unit_lb") : String(localized: "unit_kg"))",
                                background: FormaTheme.surfaceHigh,
                                foreground: FormaTheme.textMuted
                            )
                        }
                        ForEach(useLb ? [5.0, 10.0, 25.0] : [2.5, 5.0, 10.0], id: \.self) { bump in
                            Button {
                                let deltaKg = useLb ? (bump / 2.20462262185) : bump
                                kg = min(1000.0, ((kg + deltaKg) * 1000.0).rounded() / 1000.0)
                            } label: {
                                FormaPill(text: "+\(bump.formatted()) \(useLb ? String(localized: "unit_lb") : String(localized: "unit_kg"))")
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
                                            text: "\(FormaTheme.formatWeight(step.weightKg, useLb: useLb)) × \(step.reps)",
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
                    FormaHaptics.success()
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
                                    .monospacedDigit()
                                    .foregroundStyle(FormaTheme.primary)
                                HStack(spacing: 8) {
                                    Button("reduce_rest") { FormaHaptics.tap(); store.attempt { try store.extendRest(seconds: -15) } }
                                        .buttonStyle(.bordered)
                                        .frame(minHeight: 44)
                                    Button("extend_rest") { FormaHaptics.tap(); store.attempt { try store.extendRest(seconds: 15) } }
                                        .buttonStyle(.bordered)
                                        .frame(minHeight: 44)
                                    Button("stop_rest") { FormaHaptics.tap(); store.attempt { try store.stopRest() } }
                                        .font(.subheadline.weight(.medium))
                                        .foregroundStyle(FormaTheme.textMuted)
                                        .frame(minHeight: 44)
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
            Button("finish_workout") {
                FormaHaptics.success()
                store.attempt {
                    try store.finish(queueForPhone: false)
                    if let saved = store.state.workouts.last { onFinished(saved) }
                }
            }
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
            .tint(FormaTheme.primary)
        }
        .sheet(isPresented: $showSwap) {
            let primaries = Set(ExerciseMuscles.of(exercise).primary)
            let alternatives = Exercise.allCases.filter { $0 != exercise && ExerciseMuscles.of($0).primary.contains(where: { primaries.contains($0) }) }
            NavigationStack {
                List(alternatives, id: \.self) { alt in
                    Button {
                        exercise = alt
                        kg = 0.0
                        warmup = false
                        showSwap = false
                    } label: {
                        HStack(spacing: 12) {
                            FormaExerciseImage(asset: ExerciseGuides.of(alt, arabic: false).asset, maxPixelSize: 320)
                                .frame(width: 64)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                            Text(alt.label(arabic: arabic))
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                        }
                    }
                }
                .navigationTitle("swap_exercise_title")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("cancel") { showSwap = false }
                    }
                }
            }
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
    @AppStorage("forma_use_lb") private var useLb = false

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
        let stepKg = useLb ? (2.5 / FormaTheme.lbPerKg) : 0.5
        NavigationStack {
            Form {
                FormaGymStepper(
                    title: String(localized: "reps"),
                    formattedValue: "\(reps)",
                    onDecrement: { reps = max(1, reps - 1) },
                    onIncrement: { reps = min(1000, reps + 1) },
                    onDirectCommit: { val in reps = min(1000, max(1, Int(val.rounded()))) }
                )
                FormaGymStepper(
                    title: useLb ? String(localized: "unit_lb") : String(localized: "unit_kg"),
                    formattedValue: FormaTheme.formatWeight(kg, useLb: useLb),
                    onDecrement: { kg = max(0.0, ((kg - stepKg) * 1000.0).rounded() / 1000.0) },
                    onIncrement: { kg = min(1000.0, ((kg + stepKg) * 1000.0).rounded() / 1000.0) },
                    onDirectCommit: { val in
                        let inKg = useLb ? (val / FormaTheme.lbPerKg) : val
                        kg = min(1000.0, max(0.0, (inKg * 1000.0).rounded() / 1000.0))
                    }
                )
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
    @ObservedObject var store: LocalStore
    let onOpenBridge: () -> Void
    let onOpenTrain: () -> Void
    @State private var exerciseSearch = ""
    @State private var equipment: Equipment?
    @State private var homeOnly = false
    @State private var favoritesOnly = false
    @State private var selectedMuscle: Muscle?
    @State private var recordingExercise: Exercise?
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @AppStorage("forma_use_lb") private var useLb = false
    @AppStorage("forma_favorite_exercises") private var favoriteExercisesRaw = ""
    @AppStorage(ExerciseDiscomfortSelection.storageKey) private var storedDiscomfortRegions = ""
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }
    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
    private let homeEquipment: Set<Equipment> = [.bodyweight, .dumbbells, .bench, .box, .pull_up_bar]

    private var favoriteSet: Set<String> {
        Set(favoriteExercisesRaw.split(separator: ",").map(String.init))
    }

    private func toggleFavorite(_ exercise: Exercise) {
        var next = favoriteSet
        if next.contains(exercise.rawValue) { next.remove(exercise.rawValue) } else { next.insert(exercise.rawValue) }
        favoriteExercisesRaw = next.sorted().joined(separator: ",")
    }

    private var mainMuscles: [Muscle] {
        Muscle.allCases.filter { m in
            Exercise.allCases.contains { ExerciseMuscles.of($0).primary.first == m }
        }
    }

    var body: some View {
        let favs = favoriteSet
        let discomfortRegions = ExerciseDiscomfortSelection.decode(storedDiscomfortRegions)
        let filtered = Exercise.allCases.filter { exercise in
            ExerciseDiscomfortRegion.includes(exercise, excluding: discomfortRegions) &&
                (!favoritesOnly || favs.contains(exercise.rawValue)) &&
                (!homeOnly || ExerciseCatalog.of(exercise).equipment.allSatisfy { homeEquipment.contains($0) }) &&
                (selectedMuscle == nil || ExerciseMuscles.of(exercise).primary.first == selectedMuscle) &&
                (equipment == nil || ExerciseCatalog.of(exercise).equipment.contains(equipment!)) &&
                (exerciseSearch.isEmpty || (exercise.label(arabic: false) + " " + exercise.label(arabic: true)).localizedStandardContains(exerciseSearch))
        }
        let groupedMuscles = mainMuscles.filter { m in
            filtered.contains { ExerciseMuscles.of($0).primary.first == m }
        }
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    if let draft = store.state.draft {
                        Button(action: onOpenTrain) {
                            HStack(spacing: 10) {
                                FormaPulseDot()
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(draft.program.name)
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(FormaTheme.textPrimary)
                                    Text("\(draft.sets.count) \(String(localized: "sets"))")
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.primary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(FormaTheme.primary)
                            }
                            .padding(12)
                            .background(FormaTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(FormaTheme.primary.opacity(0.45), lineWidth: 1))
                        }
                    }

                    TextField("search_exercises", text: $exerciseSearch)
                        .padding(12)
                        .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border, lineWidth: 1))

                    ExerciseDiscomfortFilterButton()

                    // Main Muscle Category Chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            Button { withAnimation(.spring(response: 0.32, dampingFraction: 0.78)) { selectedMuscle = nil }; FormaHaptics.tap() } label: {
                                FormaPill(
                                    text: String(localized: "all_muscles"),
                                    background: selectedMuscle == nil ? FormaTheme.primary : FormaTheme.surface,
                                    foreground: selectedMuscle == nil ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                            ForEach(mainMuscles, id: \.self) { m in
                                let count = Exercise.allCases.filter { ExerciseMuscles.of($0).primary.first == m }.count
                                Button { withAnimation(.spring(response: 0.32, dampingFraction: 0.78)) { selectedMuscle = m }; FormaHaptics.tap() } label: {
                                    FormaPill(
                                        text: "\(m.label(arabic: arabic)) (\(count))",
                                        background: selectedMuscle == m ? FormaTheme.primary : FormaTheme.surface,
                                        foreground: selectedMuscle == m ? FormaTheme.onPrimary : FormaTheme.textMuted
                                    )
                                }
                            }
                        }
                    }

                    // Equipment & Favorites Filter Chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            Button {
                                FormaHaptics.tap()
                                favoritesOnly.toggle()
                            } label: {
                                FormaPill(
                                    text: "★ \(String(localized: "favorites_filter"))",
                                    background: favoritesOnly ? FormaTheme.secondary : FormaTheme.surface,
                                    foreground: favoritesOnly ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                            Button {
                                FormaHaptics.tap()
                                homeOnly.toggle()
                                if homeOnly { equipment = nil }
                            } label: {
                                FormaPill(
                                    text: String(localized: "preset_home_filter"),
                                    background: homeOnly ? FormaTheme.primary : FormaTheme.surface,
                                    foreground: homeOnly ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                            Button {
                                FormaHaptics.tap()
                                homeOnly = false
                                equipment = nil
                            } label: {
                                FormaPill(
                                    text: String(localized: "preset_gym_filter"),
                                    background: (!homeOnly && equipment == nil) ? FormaTheme.secondary : FormaTheme.surface,
                                    foreground: (!homeOnly && equipment == nil) ? FormaTheme.onPrimary : FormaTheme.textMuted
                                )
                            }
                            ForEach(Equipment.allCases, id: \.self) { item in
                                Button {
                                    FormaHaptics.tap()
                                    homeOnly = false
                                    equipment = (equipment == item) ? nil : item
                                } label: {
                                    FormaPill(
                                        text: item.label(arabic: arabic),
                                        background: equipment == item ? FormaTheme.secondary : FormaTheme.surface,
                                        foreground: equipment == item ? FormaTheme.onPrimary : FormaTheme.textMuted
                                    )
                                }
                            }
                        }
                    }

                    if filtered.isEmpty {
                        FormaCard {
                            VStack(spacing: 10) {
                                Text("no_matching_exercises")
                                    .font(.headline)
                                    .foregroundStyle(FormaTheme.textPrimary)
                                Button("clear_filters") {
                                    FormaHaptics.tap()
                                    exerciseSearch = ""
                                    equipment = nil
                                    homeOnly = false
                                    favoritesOnly = false
                                    selectedMuscle = nil
                                    storedDiscomfortRegions = ""
                                }
                                .buttonStyle(.bordered)
                                .frame(minHeight: 44)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }

                    PhoneCustomExercisesCard(store: store, useLb: useLb)

                    // Categorized by Main Muscle
                    ForEach(groupedMuscles, id: \.self) { groupMuscle in
                        let items = filtered.filter { ExerciseMuscles.of($0).primary.first == groupMuscle }
                        HStack(spacing: 8) {
                            RoundedRectangle(cornerRadius: 2)
                                .fill(FormaTheme.primary)
                                .frame(width: 4, height: 18)
                            Text(groupMuscle.label(arabic: arabic))
                                .font(.title3.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            FormaPill(text: "\(items.count)")
                        }
                        .padding(.top, 4)

                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(items, id: \.self) { exercise in
                                VStack(alignment: .leading, spacing: 8) {
                                    NavigationLink {
                                        ExerciseDetail(
                                            exercise: exercise,
                                            onRecordSet: { recordingExercise = exercise }
                                        )
                                    } label: {
                                        VStack(alignment: .leading, spacing: 6) {
                                            ZStack(alignment: .topLeading) {
                                                FormaExerciseImage(asset: ExerciseGuides.of(exercise, arabic: false).asset, maxPixelSize: 320)
                                                    .frame(maxWidth: .infinity)
                                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                                let hasVideo = ExerciseVideoLibrary.url(exercise) != nil
                                                let hasMotion = ExerciseMotionCatalog.of(exercise) != nil
                                                if hasVideo || hasMotion {
                                                    Text(LocalizedStringKey(hasVideo ? "badge_video_short" : "badge_3d_short"))
                                                        .font(.caption2.weight(.bold))
                                                        .padding(.horizontal, 6)
                                                        .padding(.vertical, 3)
                                                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
                                                        .foregroundStyle(FormaTheme.primary)
                                                        .padding(6)
                                                }
                                            }
                                            .overlay(alignment: .topTrailing) {
                                                let starred = favs.contains(exercise.rawValue)
                                                Button {
                                                    FormaHaptics.tap()
                                                    toggleFavorite(exercise)
                                                } label: {
                                                    Image(systemName: starred ? "star.fill" : "star")
                                                        .font(.caption.weight(.bold))
                                                        .padding(6)
                                                        .background(.ultraThinMaterial, in: Circle())
                                                        .foregroundStyle(starred ? FormaTheme.secondary : FormaTheme.textMuted)
                                                }
                                                .buttonStyle(.plain)
                                                .padding(6)
                                            }
                                            Text(exercise.label(arabic: arabic))
                                                .font(.subheadline.weight(.bold))
                                                .foregroundStyle(FormaTheme.textPrimary)
                                                .lineLimit(2)
                                                .multilineTextAlignment(.leading)
                                            Text(ExerciseCatalog.of(exercise).equipment.map { $0.label(arabic: arabic) }.joined(separator: " · "))
                                                .font(.caption2)
                                                .foregroundStyle(FormaTheme.textMuted)
                                                .lineLimit(1)
                                        }
                                    }
                                    Button {
                                        FormaHaptics.tap()
                                        recordingExercise = exercise
                                    } label: {
                                        Label("record_exercise", systemImage: "plus")
                                            .font(.caption.weight(.bold))
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 6)
                                            .background(FormaTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                            .foregroundStyle(FormaTheme.primary)
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
                    Button { onOpenBridge() } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel(Text("settings_title"))
                }
            }
            .sheet(isPresented: Binding(get: { recordingExercise != nil }, set: { if !$0 { recordingExercise = nil } })) {
                if let ex = recordingExercise {
                    PhoneQuickRecordSheet(
                        exercise: ex,
                        store: store,
                        onDismiss: { recordingExercise = nil }
                    )
                    .tint(FormaTheme.primary)
                }
            }
        }
    }
}

@MainActor private struct PhoneQuickRecordSheet: View {
    let exercise: Exercise
    @ObservedObject var store: LocalStore
    let onDismiss: () -> Void
    @AppStorage("forma_use_lb") private var useLb = false
    @Environment(\.locale) private var locale
    @State private var reps = 8
    @State private var kg = 0.0
    @State private var warmup = false
    @State private var initialized = false
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let previous = store.state.workouts.reversed().flatMap(\.sets).first(where: { $0.exercise == exercise })
            ?? store.state.draft?.sets.reversed().first(where: { $0.exercise == exercise })
        NavigationStack {
            Form {
                if let prev = previous {
                    Section {
                        HStack {
                            Text("\(prev.reps) × \(FormaTheme.formatWeight(prev.weightKg, useLb: useLb))")
                                .font(.caption)
                                .foregroundStyle(FormaTheme.textMuted)
                            Spacer()
                            Button("copy_previous_set") {
                                reps = prev.reps
                                kg = prev.weightKg
                                warmup = prev.kind == .warmUp
                            }
                            .font(.caption.weight(.semibold))
                        }
                    }
                }
                Section {
                    Stepper("\(String(localized: "reps")): \(reps)", value: $reps, in: 1...1000)
                    Stepper(FormaTheme.formatWeight(kg, useLb: useLb), value: $kg, in: 0...1000, step: useLb ? (5.0 / 2.20462262185) : 2.5)
                    Toggle("warmup", isOn: $warmup)
                }
                Section {
                    Button {
                        FormaHaptics.success()
                        let set = WatchSet(
                            exercise: exercise,
                            reps: reps,
                            weightKg: kg,
                            kind: warmup ? .warmUp : .working,
                            rpe: nil
                        )
                        store.attempt {
                            try store.quickLog(set, label: exercise.label(arabic: arabic))
                        }
                        onDismiss()
                    } label: {
                        Text("record_set")
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .navigationTitle(exercise.label(arabic: arabic))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel", action: onDismiss)
                }
            }
            .onAppear {
                if !initialized {
                    initialized = true
                    if let prev = previous {
                        reps = prev.reps
                        kg = prev.weightKg
                    }
                }
            }
        }
    }
}

@MainActor private struct PhoneEatTab: View {
    @ObservedObject var store: LocalStore
    let onOpenBridge: () -> Void
    @State private var selectedDate = Date()
    @State private var selectedSlot = "BREAKFAST"
    @State private var addingMeal = false
    @State private var editingMeal: PhoneMeal?
    @State private var showGoals = false
    @State private var showOnlineFood = false
    @State private var showStarterFoods = false
    @State private var showFoodSwap = false
    @State private var showBarcodeScanner = false
    @State private var scannedBarcode = ""
    @State private var showRemaining = false
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @AppStorage("forma_use_lb") private var useLb = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    private static let isoFormatter: DateFormatter = {
        let fmt = DateFormatter()
        fmt.calendar = Calendar(identifier: .gregorian)
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "yyyy-MM-dd"
        return fmt
    }()

    private var dateKey: String { Self.isoFormatter.string(from: selectedDate) }
    private var isToday: Bool { dateKey == LocalStore.todayString() }

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
        let dayKey = dateKey
        let meals = store.state.meals.filter { $0.date == dayKey }
        let totalKcal = meals.reduce(0.0) { $0 + $1.calories }
        let totalP = meals.reduce(0.0) { $0 + $1.protein }
        let totalC = meals.reduce(0.0) { $0 + $1.carbs }
        let totalF = meals.reduce(0.0) { $0 + $1.fat }
        let calGoal = store.state.calorieTarget.map(Double.init)
        let proGoal = store.state.proteinTarget
        let carbsGoal = store.state.carbsTarget
        let fatGoal = store.state.fatTarget
        let waterGoal = store.state.waterGoalMl

        let slotOptions = [
            MealSlotOption(id: "BREAKFAST", labelKey: "meal_breakfast"),
            MealSlotOption(id: "LUNCH", labelKey: "meal_lunch"),
            MealSlotOption(id: "DINNER", labelKey: "meal_dinner"),
            MealSlotOption(id: "SNACK", labelKey: "meal_snack"),
            MealSlotOption(id: "SUHOOR", labelKey: "meal_suhoor"),
            MealSlotOption(id: "IFTAR", labelKey: "meal_iftar"),
        ]
        let allGroupingSlots = slotOptions + [MealSlotOption(id: "UNSPECIFIED", labelKey: "meal_unspecified")]

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // Date Switcher Bar
                    HStack(spacing: 8) {
                        Button {
                            FormaHaptics.tap()
                            selectedDate = Calendar.current.date(byAdding: .day, value: -1, to: selectedDate) ?? selectedDate
                        } label: {
                            Image(systemName: arabic ? "chevron.right" : "chevron.left")
                                .font(.subheadline.weight(.bold))
                                .frame(width: 36, height: 36)
                                .background(FormaTheme.surface, in: Circle())
                                .foregroundStyle(FormaTheme.textPrimary)
                        }
                        DatePicker("", selection: $selectedDate, displayedComponents: .date)
                            .labelsHidden()
                        if !isToday {
                            Button("date_today_btn") {
                                FormaHaptics.tap()
                                selectedDate = Date()
                            }
                            .font(.caption.weight(.bold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(FormaTheme.primaryContainer, in: Capsule())
                            .foregroundStyle(FormaTheme.primary)
                        }
                        Spacer()
                        Button {
                            FormaHaptics.tap()
                            selectedDate = Calendar.current.date(byAdding: .day, value: 1, to: selectedDate) ?? selectedDate
                        } label: {
                            Image(systemName: arabic ? "chevron.left" : "chevron.right")
                                .font(.subheadline.weight(.bold))
                                .frame(width: 36, height: 36)
                                .background(FormaTheme.surface, in: Circle())
                                .foregroundStyle(FormaTheme.textPrimary)
                        }
                    }

                    // Compact Macro Summary Card (explicit user goals, no silent defaults)
                    FormaCard {
                        HStack {
                            Text(isToday ? LocalizedStringKey("food_today") : LocalizedStringKey(dayKey))
                                .font(.headline.weight(.bold))
                                .foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            Button {
                                FormaHaptics.tap()
                                showGoals = true
                            } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "target")
                                    Text("goals_btn_short").fontWeight(.semibold)
                                }
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(FormaTheme.surfaceHigh, in: Capsule())
                                .foregroundStyle(FormaTheme.primary)
                            }
                        }
                        HStack(spacing: 18) {
                            Spacer()
                            VStack(spacing: 6) {
                                let calDelta = calGoal.map { $0 - totalKcal }
                                let ringValue: String = {
                                    if let d = calDelta, showRemaining { return "\(Int(abs(d).rounded()))" }
                                    return "\(Int(totalKcal.rounded()))"
                                }()
                                let ringSub: String = {
                                    if let d = calDelta, showRemaining { return d >= 0 ? String(localized: "kcal_left_short") : String(localized: "kcal_over_short") }
                                    if let g = calGoal { return "/ \(Int(g))" }
                                    return "kcal"
                                }()
                                let ringProgress = calGoal.map { totalKcal / max(1, $0) } ?? (totalKcal > 0 ? 1.0 : 0.0)
                                Button {
                                    if calGoal != nil {
                                        FormaHaptics.tap()
                                        showRemaining.toggle()
                                    }
                                } label: {
                                    FormaProgressRing(
                                        progress: ringProgress,
                                        valueText: ringValue,
                                        labelText: ringSub,
                                        size: 90,
                                        lineWidth: 9,
                                        color: (calGoal != nil && totalKcal > calGoal!) ? .red : FormaTheme.primary
                                    )
                                }
                                Text("calories_label").font(.caption).foregroundStyle(FormaTheme.textMuted)
                            }
                            Spacer()
                            VStack(spacing: 6) {
                                let proSub = proGoal.map { "/ \(Int($0.rounded()))g" } ?? "g"
                                let proProg = proGoal.map { totalP / max(1, $0) } ?? (totalP > 0 ? 1.0 : 0.0)
                                FormaProgressRing(
                                    progress: proProg,
                                    valueText: "\(Int(totalP.rounded()))g",
                                    labelText: proSub,
                                    size: 90,
                                    lineWidth: 9,
                                    color: FormaTheme.secondary
                                )
                                Text("protein_label").font(.caption).foregroundStyle(FormaTheme.textMuted)
                            }
                            Spacer()
                        }
                        HStack(spacing: 6) {
                            let cLabel = carbsGoal.map { "\(Int(totalC.rounded()))/\(Int($0.rounded()))g" } ?? "\(Int(totalC.rounded()))g"
                            let fLabel = fatGoal.map { "\(Int(totalF.rounded()))/\(Int($0.rounded()))g" } ?? "\(Int(totalF.rounded()))g"
                            FormaPill(text: "\(String(localized: "protein_label")) \(Int(totalP.rounded()))g", background: FormaTheme.secondary.opacity(0.2), foreground: FormaTheme.secondary)
                            FormaPill(text: "\(String(localized: "carbs_label")) \(cLabel)", background: FormaTheme.tertiary.opacity(0.2), foreground: FormaTheme.tertiary)
                            FormaPill(text: "\(String(localized: "fat_label")) \(fLabel)")
                        }
                        HStack(spacing: 8) {
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
                            Button {
                                FormaHaptics.tap()
                                scannedBarcode = ""
                                showOnlineFood = true
                            } label: {
                                Image(systemName: "globe")
                                    .font(.headline)
                                    .padding(12)
                                    .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .foregroundStyle(FormaTheme.primary)
                            }
                            .accessibilityLabel(Text("online_food_search"))
                            Button {
                                FormaHaptics.tap()
                                showBarcodeScanner = true
                            } label: {
                                Image(systemName: "barcode.viewfinder")
                                    .font(.headline)
                                    .padding(12)
                                    .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .foregroundStyle(FormaTheme.primary)
                            }
                            .accessibilityLabel(Text("scan_barcode_btn"))
                        }
                        HStack(spacing: 8) {
                            Button {
                                FormaHaptics.tap()
                                showStarterFoods = true
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "leaf.fill")
                                    Text("starter_foods_title").font(.caption.weight(.bold))
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .foregroundStyle(FormaTheme.textPrimary)
                            }
                            Button {
                                FormaHaptics.tap()
                                showFoodSwap = true
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "arrow.left.arrow.right")
                                    Text("food_swap_title").font(.caption.weight(.bold))
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .foregroundStyle(FormaTheme.secondary)
                            }
                        }
                    }

                    // Compact Single-Row Daily Hydration Bar
                    let waterMl = store.state.hydration.first(where: { $0.date == dayKey })?.milliliters ?? 0
                    FormaCard {
                        HStack(spacing: 8) {
                            Image(systemName: "drop.fill").foregroundStyle(FormaTheme.tertiary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("hydration_title").font(.subheadline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                                Text(waterGoal.map { "\(waterMl) / \($0) ml" } ?? "\(waterMl) ml")
                                    .font(.caption2.weight(.semibold))
                                    .foregroundStyle(FormaTheme.tertiary)
                            }
                            Spacer()
                            Button {
                                FormaHaptics.tap()
                                store.attempt { try store.saveHydration(min(20000, waterMl + 250), date: dayKey) }
                            } label: {
                                Text("+250")
                                    .font(.caption.weight(.bold))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 7)
                                    .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .foregroundStyle(FormaTheme.primary)
                            }
                            Button {
                                FormaHaptics.tap()
                                store.attempt { try store.saveHydration(min(20000, waterMl + 500), date: dayKey) }
                            } label: {
                                Text("+500")
                                    .font(.caption.weight(.bold))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 7)
                                    .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    .foregroundStyle(FormaTheme.primary)
                            }
                            if waterMl > 0 {
                                Button {
                                    FormaHaptics.tap()
                                    store.attempt { try store.saveHydration(max(0, waterMl - 250), date: dayKey) }
                                } label: {
                                    Image(systemName: "minus")
                                        .font(.caption.weight(.bold))
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 8)
                                        .background(FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                        .foregroundStyle(FormaTheme.textMuted)
                                }
                            }
                        }
                    }

                    // Compact Daily Supplements & Vitamins Bar
                    PhoneSupplementsCard(store: store, today: dayKey)

                    // Meal slot selector (including Ramadan Suhoor & Iftar)
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
                                    FormaHaptics.success()
                                    store.attempt {
                                        try store.addMeal(PhoneMeal(
                                            date: dayKey,
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

                    if !store.state.mealCombos.isEmpty {
                        Text("saved_combos_title").font(.subheadline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 10) {
                                ForEach(store.state.mealCombos) { combo in
                                    Button {
                                        FormaHaptics.success()
                                        store.attempt {
                                            try store.addMeal(PhoneMeal(
                                                date: dayKey,
                                                name: combo.name,
                                                calories: combo.calories,
                                                protein: combo.protein,
                                                carbs: combo.carbs,
                                                fat: combo.fat,
                                                slot: selectedSlot
                                            ))
                                        }
                                    } label: {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(combo.name)
                                                .font(.caption.weight(.bold))
                                                .foregroundStyle(FormaTheme.textPrimary)
                                                .lineLimit(1)
                                            Text("\(Int(combo.calories.rounded())) kcal · \(Int(combo.protein.rounded()))g P")
                                                .font(.caption2)
                                                .foregroundStyle(FormaTheme.secondary)
                                        }
                                        .padding(10)
                                        .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.secondary.opacity(0.4), lineWidth: 1))
                                    }
                                    .contextMenu {
                                        Button("delete", role: .destructive) {
                                            store.attempt { try store.deleteMealCombo(combo.id) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Grouped Meal Diary by Slot + Tap-to-Edit
                    if meals.isEmpty {
                        FormaCard { Text("no_meals_today").foregroundStyle(FormaTheme.textMuted) }
                    } else {
                        ForEach(allGroupingSlots) { group in
                            let groupMeals = meals.filter { $0.slot == group.id }
                            if !groupMeals.isEmpty {
                                let subKcal = groupMeals.reduce(0.0) { $0 + $1.calories }
                                let subP = groupMeals.reduce(0.0) { $0 + $1.protein }
                                FormaCard {
                                    HStack {
                                        Text(LocalizedStringKey(group.labelKey))
                                            .font(.subheadline.weight(.bold))
                                            .foregroundStyle(FormaTheme.primary)
                                        Spacer()
                                        Text("\(Int(subKcal.rounded())) kcal · \(Int(subP.rounded()))g P")
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(FormaTheme.textMuted)
                                        if groupMeals.count >= 2 {
                                            Button {
                                                FormaHaptics.success()
                                                let comboName = "\(String(localized: String.LocalizationValue(group.labelKey))) (\(groupMeals.count))"
                                                store.attempt {
                                                    try store.saveMealCombo(PhoneMealCombo(
                                                        name: comboName,
                                                        calories: subKcal,
                                                        protein: subP,
                                                        carbs: groupMeals.reduce(0) { $0 + $1.carbs },
                                                        fat: groupMeals.reduce(0) { $0 + $1.fat }
                                                    ))
                                                }
                                            } label: {
                                                Image(systemName: "bookmark.fill")
                                                    .font(.caption)
                                                    .foregroundStyle(FormaTheme.secondary)
                                            }
                                            .accessibilityLabel(Text("save_meal_combo"))
                                        }
                                    }
                                    Divider().overlay(FormaTheme.border)
                                    ForEach(groupMeals) { meal in
                                        HStack {
                                            Button {
                                                FormaHaptics.tap()
                                                editingMeal = meal
                                            } label: {
                                                VStack(alignment: .leading, spacing: 2) {
                                                    Text(meal.name)
                                                        .font(.subheadline.weight(.bold))
                                                        .foregroundStyle(FormaTheme.textPrimary)
                                                    Text("\(Int(meal.protein))g P · \(Int(meal.carbs))g C · \(Int(meal.fat))g F")
                                                        .font(.caption2)
                                                        .foregroundStyle(FormaTheme.textMuted)
                                                }
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                            }
                                            .buttonStyle(.plain)
                                            FormaPill(text: "\(Int(meal.calories.rounded())) kcal")
                                            Button(role: .destructive) { store.attempt { try store.deleteMeal(meal.id) } } label: {
                                                Image(systemName: "trash").font(.caption)
                                            }
                                        }
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
                    Button { onOpenBridge() } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel(Text("settings_title"))
                }
            }
            .sheet(isPresented: $addingMeal) {
                PhoneMealEditor(date: dayKey, slot: selectedSlot) { meal in
                    store.attempt { try store.addMeal(meal); addingMeal = false }
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
            .sheet(item: $editingMeal) { existing in
                PhoneMealEditor(existing: existing, date: existing.date, slot: existing.slot) { updated in
                    store.attempt { try store.updateMeal(updated); editingMeal = nil }
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
            .sheet(isPresented: $showGoals) {
                PhoneGoalsSheet(store: store)
                    .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                    .tint(FormaTheme.primary)
            }
            .sheet(isPresented: $showOnlineFood) {
                PhoneOnlineFoodSheet(slot: selectedSlot, initialQuery: scannedBarcode) { meal in
                    var dated = meal; dated.date = dayKey
                    store.attempt { try store.addMeal(dated) }
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
            .sheet(isPresented: $showStarterFoods) {
                PhoneStarterFoodsSheet(slot: selectedSlot) { meal in
                    var dated = meal; dated.date = dayKey
                    store.attempt { try store.addMeal(dated) }
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
            .sheet(isPresented: $showFoodSwap) {
                PhoneFoodSwapSheet(slot: selectedSlot) { meal in
                    var dated = meal; dated.date = dayKey
                    store.attempt { try store.addMeal(dated) }
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
            .sheet(isPresented: $showBarcodeScanner) {
                BarcodeScannerSheet { code in
                    scannedBarcode = code
                    showOnlineFood = true
                }
                .preferredColorScheme(FormaThemeMode.resolved(rawValue: themeModeRaw).colorScheme)
                .tint(FormaTheme.primary)
            }
        }
    }
}

@MainActor private struct PhoneMealEditor: View {
    var existing: PhoneMeal? = nil
    let date: String
    let slot: String
    let onSave: (PhoneMeal) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var kcal = 350.0
    @State private var protein = 30.0
    @State private var carbs = 35.0
    @State private var fat = 10.0
    @State private var chosenSlot = "BREAKFAST"
    @State private var initialized = false

    private let slotOptions = [
        MealSlotOption(id: "BREAKFAST", labelKey: "meal_breakfast"),
        MealSlotOption(id: "LUNCH", labelKey: "meal_lunch"),
        MealSlotOption(id: "DINNER", labelKey: "meal_dinner"),
        MealSlotOption(id: "SNACK", labelKey: "meal_snack"),
        MealSlotOption(id: "SUHOOR", labelKey: "meal_suhoor"),
        MealSlotOption(id: "IFTAR", labelKey: "meal_iftar"),
        MealSlotOption(id: "UNSPECIFIED", labelKey: "meal_unspecified"),
    ]

    var body: some View {
        NavigationStack {
            Form {
                TextField("meal_name", text: $name)
                Picker("meal_slot_label", selection: $chosenSlot) {
                    ForEach(slotOptions) { opt in
                        Text(LocalizedStringKey(opt.labelKey)).tag(opt.id)
                    }
                }
                Stepper("\(String(localized: "calories_label")): \(Int(kcal))", value: $kcal, in: 0...5000, step: 25)
                Stepper("\(String(localized: "protein_label")): \(Int(protein))g", value: $protein, in: 0...500, step: 2)
                Stepper("\(String(localized: "carbs_label")): \(Int(carbs))g", value: $carbs, in: 0...500, step: 5)
                Stepper("\(String(localized: "fat_label")): \(Int(fat))g", value: $fat, in: 0...300, step: 2)
                Button("calc_from_macros") {
                    kcal = (protein * 4.0) + (carbs * 4.0) + (fat * 9.0)
                }
            }
            .navigationTitle(existing == nil ? "log_meal" : "edit_meal_title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmed.isEmpty else { return }
                        onSave(PhoneMeal(
                            id: existing?.id ?? UUID().uuidString,
                            date: date,
                            name: trimmed,
                            calories: kcal,
                            protein: protein,
                            carbs: carbs,
                            fat: fat,
                            slot: chosenSlot
                        ))
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .onAppear {
                if !initialized {
                    initialized = true
                    chosenSlot = existing?.slot ?? slot
                    if let existing {
                        name = existing.name
                        kcal = existing.calories
                        protein = existing.protein
                        carbs = existing.carbs
                        fat = existing.fat
                    }
                }
            }
        }
    }
}

@MainActor private struct PhoneProgressTab: View {
    @ObservedObject var store: LocalStore
    @ObservedObject var health: HealthImporter
    let onOpenBridge: () -> Void
    @State private var weightKg = 75.0
    @State private var initializedWeight = false
    @State private var showMoreMeasurements = false
    @State private var waistText = ""
    @State private var neckText = ""
    @State private var chestText = ""
    @State private var armsText = ""
    @State private var hipsText = ""
    @State private var thighsText = ""
    @State private var scrubbedIndex: Int?
    @AppStorage(FormaThemeMode.storageKey) private var themeModeRaw = ""
    @AppStorage("forma_use_lb") private var useLb = false
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    private func parseCm(_ raw: String) -> Double? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let normalized = trimmed
            .applyingTransform(.toLatin, reverse: false) ?? trimmed
        let cleaned = normalized
            .replacingOccurrences(of: "٫", with: ".")
            .replacingOccurrences(of: ",", with: ".")
        guard let v = Double(cleaned), v.isFinite, v > 0, v <= 300 else { return nil }
        return v
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    PhoneHealthCard(store: store, importer: health)
                    PhoneHealthActivityCard(records: store.state.healthWorkouts)

                    // Quick Weight & Body Tape Logger Card
                    FormaCard {
                        HStack {
                            Text("body_weight").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            if let latest = store.combinedWeights.last {
                                FormaPill(text: FormaTheme.formatWeight(latest.weightKg, useLb: useLb))
                            }
                        }
                        Stepper(value: $weightKg, in: 20...300, step: 0.5) {
                            Text(FormaTheme.formatWeight(weightKg, useLb: useLb))
                                .font(.title2.weight(.heavy))
                                .monospacedDigit()
                                .foregroundStyle(FormaTheme.primary)
                        }
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) { showMoreMeasurements.toggle() }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: showMoreMeasurements ? "chevron.up" : "chevron.down")
                                Text("more_measurements").font(.caption.weight(.semibold))
                                Spacer()
                            }
                            .foregroundStyle(FormaTheme.secondary)
                        }
                        if showMoreMeasurements {
                            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                                TextField("measure_waist_cm", text: $waistText).keyboardType(.decimalPad)
                                    .padding(10).background(FormaTheme.background, in: RoundedRectangle(cornerRadius: 10))
                                TextField("measure_neck_cm", text: $neckText).keyboardType(.decimalPad)
                                    .padding(10).background(FormaTheme.background, in: RoundedRectangle(cornerRadius: 10))
                                TextField("measure_chest_cm", text: $chestText).keyboardType(.decimalPad)
                                    .padding(10).background(FormaTheme.background, in: RoundedRectangle(cornerRadius: 10))
                                TextField("measure_arms_cm", text: $armsText).keyboardType(.decimalPad)
                                    .padding(10).background(FormaTheme.background, in: RoundedRectangle(cornerRadius: 10))
                                TextField("measure_hips_cm", text: $hipsText).keyboardType(.decimalPad)
                                    .padding(10).background(FormaTheme.background, in: RoundedRectangle(cornerRadius: 10))
                                TextField("measure_thighs_cm", text: $thighsText).keyboardType(.decimalPad)
                                    .padding(10).background(FormaTheme.background, in: RoundedRectangle(cornerRadius: 10))
                            }
                            .font(.subheadline)
                        }
                        Button {
                            FormaHaptics.success()
                            store.attempt {
                                try store.saveWeight(
                                    weightKg,
                                    waistCm: parseCm(waistText),
                                    neckCm: parseCm(neckText),
                                    chestCm: parseCm(chestText),
                                    armsCm: parseCm(armsText),
                                    hipsCm: parseCm(hipsText),
                                    thighsCm: parseCm(thighsText)
                                )
                            }
                        } label: {
                            Text("save_weight")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .foregroundStyle(FormaTheme.onPrimary)
                        }
                        if let latestTape = store.state.weights.last(where: {
                            $0.waistCm != nil || $0.neckCm != nil || $0.chestCm != nil || $0.armsCm != nil || $0.hipsCm != nil || $0.thighsCm != nil
                        }) {
                            let parts: [String] = [
                                latestTape.waistCm.map { "\(String(localized: "measure_waist_short")) \(String(format: "%.1f", $0))" },
                                latestTape.neckCm.map { "\(String(localized: "measure_neck_short")) \(String(format: "%.1f", $0))" },
                                latestTape.chestCm.map { "\(String(localized: "measure_chest_short")) \(String(format: "%.1f", $0))" },
                                latestTape.armsCm.map { "\(String(localized: "measure_arms_short")) \(String(format: "%.1f", $0))" },
                                latestTape.hipsCm.map { "\(String(localized: "measure_hips_short")) \(String(format: "%.1f", $0))" },
                                latestTape.thighsCm.map { "\(String(localized: "measure_thighs_short")) \(String(format: "%.1f", $0))" },
                            ].compactMap { $0 }
                            if !parts.isEmpty {
                                Text("\(latestTape.date) · \(parts.joined(separator: " · ")) cm")
                                    .font(.caption)
                                    .foregroundStyle(FormaTheme.textMuted)
                            }
                        }
                    }

                    // Interactive Scrubbable Gradient Weight Trend Chart
                    FormaCard {
                        let points = Array(store.combinedWeights.suffix(30))
                        let trendValues = FormaTheme.rollingWeightTrend(points.map { ($0.date, $0.weightKg) }, windowDays: 7)
                        let activePoint = scrubbedIndex.flatMap { $0 < points.count ? points[$0] : nil }
                        HStack {
                            Text("weight_trend").font(.headline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                            Spacer()
                            if trendValues.count >= 2 && activePoint == nil {
                                FormaPill(text: String(localized: "weight_trend_7d"), background: FormaTheme.tertiary.opacity(0.2), foreground: FormaTheme.tertiary)
                            }
                            if let active = activePoint {
                                FormaPill(text: "\(FormaTheme.formatWeight(active.weightKg, useLb: useLb)) · \(active.date)" + (active.fromHealth ? " · \(String(localized: "health_source_short"))" : ""))
                            }
                        }
                        if points.count < 2 {
                            Text("no_weights").font(.caption).foregroundStyle(FormaTheme.textMuted)
                        } else {
                            let allW = points.map(\.weightKg) + trendValues
                            let minW = allW.min() ?? 0
                            let maxW = allW.max() ?? 1
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
                                let trendCoords: [CGPoint] = trendValues.enumerated().map { idx, val in
                                    let x = w * CGFloat(idx) / CGFloat(max(1, points.count - 1))
                                    let norm = CGFloat((val - minW) / span)
                                    let y = h - norm * (h - 20) - 10
                                    return CGPoint(x: x, y: y)
                                }
                                let highlight = scrubbedIndex ?? (coords.count - 1)
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

                                    if trendCoords.count >= 2 {
                                        Path { p in
                                            trendCoords.enumerated().forEach { idx, pt in
                                                if idx == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                                            }
                                        }
                                        .stroke(FormaTheme.tertiary, style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round, dash: [6, 4]))
                                    }

                                    if let selIdx = scrubbedIndex, selIdx < coords.count {
                                        Path { p in
                                            p.move(to: CGPoint(x: coords[selIdx].x, y: 4))
                                            p.addLine(to: CGPoint(x: coords[selIdx].x, y: h - 4))
                                        }
                                        .stroke(FormaTheme.primary.opacity(0.45), lineWidth: 1.5)
                                    }

                                    ForEach(0..<coords.count, id: \.self) { idx in
                                        let isSel = idx == highlight
                                        // Hollow dots mark days that came from Apple Health.
                                        Circle()
                                            .fill(points[idx].fromHealth ? FormaTheme.surface : FormaTheme.primary)
                                            .overlay(Circle().stroke(FormaTheme.primary, lineWidth: 2))
                                            .frame(width: isSel ? 10 : 6, height: isSel ? 10 : 6)
                                            .position(coords[idx])
                                    }
                                }
                                .contentShape(Rectangle())
                                .gesture(
                                    DragGesture(minimumDistance: 0)
                                        .onChanged { value in
                                            let clamped = min(max(0, value.location.x), max(1, w))
                                            let idx = Int(((clamped / max(1, w)) * CGFloat(points.count - 1)).rounded())
                                            scrubbedIndex = min(max(0, idx), points.count - 1)
                                        }
                                        .onEnded { _ in scrubbedIndex = nil }
                                )
                            }
                            .frame(height: 150)
                        }
                    }

                    // 7-Day Body Muscle Heat Map + Primary vs Supporting Breakdown
                    let recentWorkouts = Array(store.state.workouts.suffix(7))
                    let muscleCounts: [Muscle: Int] = {
                        var counts: [Muscle: Int] = [:]
                        for workout in recentWorkouts {
                            for set in workout.sets where set.kind == .working {
                                let act = ExerciseMuscles.of(set.exercise)
                                for m in act.primary + act.secondary { counts[m, default: 0] += 1 }
                            }
                        }
                        return counts
                    }()
                    let muscleSplit: [(muscle: Muscle, primary: Int, supporting: Int)] = {
                        var prim: [Muscle: Int] = [:]
                        var supp: [Muscle: Int] = [:]
                        for workout in recentWorkouts {
                            for set in workout.sets where set.kind == .working {
                                let act = ExerciseMuscles.of(set.exercise)
                                for m in act.primary { prim[m, default: 0] += 1 }
                                for m in act.secondary { supp[m, default: 0] += 1 }
                            }
                        }
                        return Muscle.allCases
                            .map { ($0, prim[$0] ?? 0, supp[$0] ?? 0) }
                            .filter { $0.1 > 0 || $0.2 > 0 }
                            .sorted { ($0.1, $0.2) > ($1.1, $1.2) }
                    }()
                    FormaCard {
                        WeeklyMuscleHeatMapView(setsByMuscle: muscleCounts)
                        if !muscleSplit.isEmpty {
                            Divider().overlay(FormaTheme.border)
                            VStack(alignment: .leading, spacing: 6) {
                                Text("weekly_muscle_split_title")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(FormaTheme.textMuted)
                                ForEach(muscleSplit.prefix(8), id: \.muscle) { row in
                                    HStack {
                                        Text(row.muscle.label(arabic: arabic))
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(FormaTheme.textPrimary)
                                        Spacer()
                                        if row.primary > 0 {
                                            FormaPill(text: "\(row.primary) \(String(localized: "primary_short"))", background: FormaTheme.primaryContainer, foreground: FormaTheme.primary)
                                        }
                                        if row.supporting > 0 {
                                            FormaPill(text: "\(row.supporting) \(String(localized: "supporting_short"))", background: FormaTheme.surfaceHigh, foreground: FormaTheme.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                    Button { onOpenBridge() } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel(Text("settings_title"))
                }
            }
            .task { await health.refreshIfConnected(store) }
            .onAppear {
                if !initializedWeight, let latest = store.combinedWeights.last {
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

    static func statusKey(_ state: WatchLinkState) -> String {
        switch state {
        case .ready: return "watch_connected"
        case .notPaired: return "watch_not_paired"
        case .appNotInstalled: return "watch_app_missing"
        case .inactive, .unsupported: return "watch_disconnected"
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    FormaCard {
                        let ready = bridge.linkState == .ready
                        FormaPill(
                            text: String(localized: String.LocalizationValue(Self.statusKey(bridge.linkState))),
                            background: ready ? FormaTheme.primaryContainer : FormaTheme.surfaceHigh,
                            foreground: ready ? FormaTheme.primary : FormaTheme.textMuted
                        )
                        if bridge.linkState == .appNotInstalled {
                            Text("watch_app_missing_help")
                                .font(.caption)
                                .foregroundStyle(FormaTheme.textMuted)
                        }
                        Text("phone_bridge_note")
                            .font(.subheadline)
                            .foregroundStyle(FormaTheme.textPrimary)
                        Button {
                            store.attempt { try bridge.sendLibrary() }
                        } label: {
                            Text("send_watch")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(ready ? FormaTheme.primary : FormaTheme.surfaceHigh, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .foregroundStyle(ready ? FormaTheme.onPrimary : FormaTheme.textPrimary)
                        }
                        .disabled(bridge.linkState != .ready)
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("tab_bridge")
        }
    }
}
