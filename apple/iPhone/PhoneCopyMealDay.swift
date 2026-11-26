import SwiftUI
import FormaCore

/// Opens a preview-then-confirm sheet that copies one recorded day of meals onto the selected day.
@MainActor struct PhoneCopyMealDayButton: View {
    @ObservedObject var store: LocalStore
    let targetDate: Date
    @State private var open = false

    var body: some View {
        Button {
            FormaHaptics.tap()
            open = true
        } label: {
            HStack(spacing: 4) {
                Image(systemName: "doc.on.doc")
                Text("copy_meal_day").fontWeight(.semibold)
            }
            .font(.caption)
            .padding(.horizontal, 10)
            .frame(minHeight: 44)
            .background(FormaTheme.surfaceHigh, in: Capsule())
            .foregroundStyle(FormaTheme.primary)
        }
        .sheet(isPresented: $open) {
            PhoneCopyMealDaySheet(store: store, target: targetDate)
                .tint(FormaTheme.primary)
        }
    }
}

@MainActor private struct PhoneCopyMealDaySheet: View {
    @ObservedObject var store: LocalStore
    let target: Date
    @Environment(\.dismiss) private var dismiss
    @State private var source: Date
    /// The meals shown at review time; confirm copies exactly these and fails if the source day changed meanwhile.
    @State private var reviewed: [PhoneMeal]?

    init(store: LocalStore, target: Date) {
        self.store = store
        self.target = target
        _source = State(initialValue: Calendar.current.date(byAdding: .day, value: -1, to: target) ?? target)
    }

    private var sourceKey: String { PhoneCardioFormat.dayString(source) }
    private var targetKey: String { PhoneCardioFormat.dayString(target) }
    private var meals: [PhoneMeal] { store.state.meals.filter { $0.date == sourceKey } }

    var body: some View {
        let shown = reviewed ?? meals
        NavigationStack {
            Form {
                Section {
                    Text(String(format: String(localized: "copy_day_note"), target.formatted(date: .abbreviated, time: .omitted)))
                        .font(.footnote)
                        .foregroundStyle(FormaTheme.textMuted)
                    DatePicker("date_label", selection: $source, in: ...Date(), displayedComponents: .date)
                        .onChange(of: source) { reviewed = nil }
                }
                Section(String(format: String(localized: "copy_meal_count"), shown.count.formatted())) {
                    ForEach(shown.prefix(200)) { meal in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(meal.name).font(.subheadline.weight(.semibold))
                            Text("\(Int(meal.calories.rounded())) kcal · P \(meal.protein.formatted(.number.precision(.fractionLength(0...1)))) · C \(meal.carbs.formatted(.number.precision(.fractionLength(0...1)))) · F \(meal.fat.formatted(.number.precision(.fractionLength(0...1))))")
                                .font(.caption)
                                .foregroundStyle(FormaTheme.textMuted)
                        }
                    }
                }
            }
            .navigationTitle("copy_meal_day")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(LocalizedStringKey(reviewed == nil ? "review_copy_day" : "confirm")) {
                        if let reviewed {
                            FormaHaptics.success()
                            store.attempt { try store.copyMeals(reviewed, from: sourceKey, to: targetKey) }
                            dismiss()
                        } else {
                            reviewed = meals
                        }
                    }
                    .disabled(sourceKey == targetKey || !(1...200).contains(meals.count))
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
