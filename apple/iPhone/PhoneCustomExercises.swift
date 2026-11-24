import SwiftUI
import FormaCore

@MainActor struct PhoneCustomExercisesCard: View {
    @ObservedObject var store: LocalStore
    let useLb: Bool
    @State private var creating = false
    @State private var loggingExercise: PhoneCustomExercise?
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        FormaCard {
            HStack {
                Image(systemName: "figure.strengthtraining.functional")
                    .foregroundStyle(FormaTheme.primary)
                Text("custom_exercises_title")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(FormaTheme.textPrimary)
                Spacer()
                Button {
                    FormaHaptics.tap()
                    creating = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("custom_exercise_add").fontWeight(.semibold)
                    }
                    .font(.caption)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(FormaTheme.primaryContainer, in: Capsule())
                    .foregroundStyle(FormaTheme.primary)
                }
            }

            if store.state.customExercises.isEmpty {
                Text("custom_exercises_empty")
                    .font(.caption)
                    .foregroundStyle(FormaTheme.textMuted)
            } else {
                VStack(spacing: 8) {
                    ForEach(store.state.customExercises) { ex in
                        let exSets = store.state.customSets.filter { $0.exerciseId == ex.id }
                        let best = exSets.filter { !$0.warmUp }.max { $0.weightKg < $1.weightKg }
                        HStack(spacing: 10) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(ex.name)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(FormaTheme.textPrimary)
                                Text("\(ex.primaryMuscle.label(arabic: arabic)) · \(ex.equipment.label(arabic: arabic))")
                                    .font(.caption2)
                                    .foregroundStyle(FormaTheme.textMuted)
                                if let best {
                                    Text("\(String(localized: "custom_exercise_best")): \(FormaTheme.formatWeight(best.weightKg, useLb: useLb)) × \(best.reps)")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(FormaTheme.primary)
                                }
                            }
                            Spacer()
                            Button {
                                FormaHaptics.tap()
                                loggingExercise = ex
                            } label: {
                                Text("record_set")
                                    .font(.caption.weight(.bold))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(FormaTheme.primary, in: Capsule())
                                    .foregroundStyle(FormaTheme.onPrimary)
                            }
                            Button(role: .destructive) {
                                store.attempt { try store.deleteCustomExercise(ex.id) }
                            } label: {
                                Image(systemName: "trash").font(.caption)
                            }
                        }
                        .padding(10)
                        .background(FormaTheme.surfaceHigh.opacity(0.6), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
        }
        .sheet(isPresented: $creating) {
            PhoneCreateCustomExerciseSheet { name, equip, muscle, notes in
                store.attempt { try store.addCustomExercise(name: name, equipment: equip, primaryMuscle: muscle, notes: notes) }
            }
            .tint(FormaTheme.primary)
        }
        .sheet(item: $loggingExercise) { ex in
            PhoneLogCustomSetSheet(store: store, exercise: ex, useLb: useLb)
                .tint(FormaTheme.primary)
        }
    }
}

@MainActor private struct PhoneCreateCustomExerciseSheet: View {
    let onCreate: (String, Equipment, Muscle, String) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var name = ""
    @State private var equipment: Equipment = .dumbbells
    @State private var muscle: Muscle = .chest
    @State private var notes = ""
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        NavigationStack {
            Form {
                TextField("custom_exercise_name", text: $name)
                Picker("custom_exercise_muscle", selection: $muscle) {
                    ForEach(Muscle.allCases, id: \.self) { m in
                        Text(m.label(arabic: arabic)).tag(m)
                    }
                }
                Picker("custom_exercise_equipment", selection: $equipment) {
                    ForEach(Equipment.allCases, id: \.self) { eq in
                        Text(eq.label(arabic: arabic)).tag(eq)
                    }
                }
                TextField("custom_exercise_notes", text: $notes)
            }
            .navigationTitle("custom_exercise_add")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        onCreate(name, equipment, muscle, notes)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

@MainActor private struct PhoneLogCustomSetSheet: View {
    @ObservedObject var store: LocalStore
    let exercise: PhoneCustomExercise
    let useLb: Bool
    @Environment(\.dismiss) private var dismiss
    @State private var reps = 10
    @State private var weightKg = 20.0
    @State private var warmUp = false

    var body: some View {
        let recent = store.state.customSets.filter { $0.exerciseId == exercise.id }.suffix(5).reversed()
        NavigationStack {
            Form {
                Section {
                    Stepper("\(String(localized: "reps")): \(reps)", value: $reps, in: 1...1000)
                    Stepper(
                        FormaTheme.formatWeight(weightKg, useLb: useLb),
                        value: $weightKg,
                        in: 0...1000,
                        step: useLb ? (5.0 / 2.20462262185) : 0.5
                    )
                    Toggle("warmup", isOn: $warmUp)
                }
                if !recent.isEmpty {
                    Section("recent_workouts") {
                        ForEach(Array(recent)) { s in
                            Text("\(s.date) · \(FormaTheme.formatWeight(s.weightKg, useLb: useLb)) × \(s.reps)")
                                .font(.caption)
                                .foregroundStyle(FormaTheme.textMuted)
                        }
                    }
                }
            }
            .navigationTitle(exercise.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        FormaHaptics.success()
                        store.attempt { try store.logCustomSet(exerciseId: exercise.id, reps: reps, weightKg: weightKg, warmUp: warmUp) }
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
