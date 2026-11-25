import SwiftUI
import FormaCore

/// Offline searchable picker for Forma's 42 curated whole foods and Middle Eastern / Arabic staples.
@MainActor struct PhoneStarterFoodsSheet: View {
    let slot: String
    let onLog: (PhoneMeal) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var query = ""
    @State private var regionalOnly = false
    @State private var selectedFood: StarterFoodItem?
    @State private var amount = 100.0
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    private var matches: [StarterFoodItem] {
        StarterFoodsCatalog.all.filter { item in
            (!regionalOnly || item.isRegionalStaple) &&
                (query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                    (item.nameEn + " " + item.nameAr).localizedStandardContains(query))
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    TextField("starter_foods_search", text: $query)
                        .padding(12)
                        .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border, lineWidth: 1))

                    HStack(spacing: 8) {
                        Button {
                            FormaHaptics.tap()
                            regionalOnly = false
                        } label: {
                            FormaPill(
                                text: String(localized: "starter_foods_all"),
                                background: !regionalOnly ? FormaTheme.primary : FormaTheme.surface,
                                foreground: !regionalOnly ? FormaTheme.onPrimary : FormaTheme.textMuted
                            )
                        }
                        Button {
                            FormaHaptics.tap()
                            regionalOnly = true
                        } label: {
                            FormaPill(
                                text: String(localized: "starter_foods_arabic"),
                                background: regionalOnly ? FormaTheme.secondary : FormaTheme.surface,
                                foreground: regionalOnly ? FormaTheme.onPrimary : FormaTheme.textMuted
                            )
                        }
                        Spacer()
                    }

                    if let food = selectedFood {
                        let scale = amount / food.referenceGrams
                        FormaCard {
                            HStack {
                                Text(food.name(arabic: arabic))
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(FormaTheme.textPrimary)
                                Spacer()
                                Button("cancel") { selectedFood = nil }
                                    .font(.caption.weight(.semibold))
                            }
                            let presets: [Double] = food.referenceGrams == 1 ? [1, 2, 3] : [50, 100, 150, 200]
                            HStack(spacing: 8) {
                                ForEach(presets, id: \.self) { p in
                                    Button {
                                        FormaHaptics.tap()
                                        amount = p
                                    } label: {
                                        FormaPill(
                                            text: "\(Int(p)) \(food.unitLabel(arabic: arabic))",
                                            background: amount == p ? FormaTheme.primary : FormaTheme.surfaceHigh,
                                            foreground: amount == p ? FormaTheme.onPrimary : FormaTheme.textPrimary
                                        )
                                    }
                                }
                            }
                            Text("\(FormaUnits.kcal(Double((food.calories * scale).rounded()))) · \(FormaUnits.protein(Double((food.protein * scale).rounded()))) · \(FormaUnits.carbs(Double((food.carbs * scale).rounded()))) · \(FormaUnits.fat(Double((food.fat * scale).rounded())))")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(FormaTheme.primary)
                            Button {
                                FormaHaptics.success()
                                onLog(PhoneMeal(
                                    date: LocalStore.todayString(),
                                    name: "\(food.name(arabic: arabic)) (\(Int(amount)) \(food.unitLabel(arabic: arabic)))",
                                    calories: (food.calories * scale).rounded(),
                                    protein: (food.protein * scale * 10).rounded() / 10,
                                    carbs: (food.carbs * scale * 10).rounded() / 10,
                                    fat: (food.fat * scale * 10).rounded() / 10,
                                    slot: slot
                                ))
                                dismiss()
                            } label: {
                                Text("log_meal")
                                    .fontWeight(.bold)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .foregroundStyle(FormaTheme.onPrimary)
                            }
                        }
                    }

                    ForEach(matches) { item in
                        Button {
                            FormaHaptics.tap()
                            selectedFood = item
                            amount = item.referenceGrams
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.name(arabic: arabic))
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(FormaTheme.textPrimary)
                                    Text("\(Int(item.referenceGrams)) \(item.unitLabel(arabic: arabic)) · \(FormaUnits.kcal(Double(item.calories))) · \(FormaUnits.protein(Double(item.protein.rounded())))")
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.textMuted)
                                }
                                Spacer()
                                Image(systemName: "plus.circle.fill")
                                    .foregroundStyle(FormaTheme.primary)
                            }
                            .padding(12)
                            .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(FormaTheme.border.opacity(0.7), lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("starter_foods_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("done") { dismiss() }
                }
            }
        }
    }
}

/// Pure deterministic offline food-swap calculator matching protein or calories.
@MainActor struct PhoneFoodSwapSheet: View {
    let slot: String
    let onLog: (PhoneMeal) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var targetFood: StarterFoodItem = StarterFoodsCatalog.all[0]
    @State private var targetAmount = 150.0
    @State private var metric: FoodSwapMetric = .protein
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        let swaps = StarterFoodsCatalog.findSwaps(target: targetFood, targetAmount: targetAmount, matchBy: metric)
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    FormaCard {
                        Picker("food_swap_source", selection: $targetFood) {
                            ForEach(StarterFoodsCatalog.all, id: \.self) { item in
                                Text(item.name(arabic: arabic)).tag(item)
                            }
                        }
                        .pickerStyle(.menu)

                        Stepper(value: $targetAmount, in: 10...1000, step: targetFood.referenceGrams == 1 ? 1 : 25) {
                            Text("\(Int(targetAmount)) \(targetFood.unitLabel(arabic: arabic))")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(FormaTheme.primary)
                        }

                        Picker("food_swap_match_by", selection: $metric) {
                            Text("protein_label").tag(FoodSwapMetric.protein)
                            Text("calories_label").tag(FoodSwapMetric.calories)
                        }
                        .pickerStyle(.segmented)
                    }

                    ForEach(swaps) { option in
                        FormaCard {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(option.food.name(arabic: arabic))
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(FormaTheme.textPrimary)
                                    Text("\(Int(option.matchedAmount.rounded())) \(option.food.unitLabel(arabic: arabic)) · \(FormaUnits.kcal(Double(option.calories.rounded()))) · \(FormaUnits.protein(Double(option.protein.rounded())))")
                                        .font(.caption)
                                        .foregroundStyle(FormaTheme.primary)
                                }
                                Spacer()
                                Button {
                                    FormaHaptics.success()
                                    onLog(PhoneMeal(
                                        date: LocalStore.todayString(),
                                        name: "\(option.food.name(arabic: arabic)) (\(Int(option.matchedAmount.rounded())) \(option.food.unitLabel(arabic: arabic)))",
                                        calories: option.calories.rounded(),
                                        protein: (option.protein * 10).rounded() / 10,
                                        carbs: (option.carbs * 10).rounded() / 10,
                                        fat: (option.fat * 10).rounded() / 10,
                                        slot: slot
                                    ))
                                    dismiss()
                                } label: {
                                    Text("log_meal")
                                        .font(.caption.weight(.bold))
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(FormaTheme.primary, in: Capsule())
                                        .foregroundStyle(FormaTheme.onPrimary)
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("food_swap_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("done") { dismiss() }
                }
            }
        }
    }
}

/// Personal daily Supplements & Vitamins checklist card inside the Eat tab (no medical/dosage prescription).
@MainActor struct PhoneSupplementsCard: View {
    @ObservedObject var store: LocalStore
    let today: String
    @State private var managing = false
    @State private var addingCustom = false
    @State private var customName = ""
    @State private var customDose = ""
    @Environment(\.locale) private var locale
    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    private let presets: [(en: String, ar: String, dose: String)] = [
        ("Creatine", "كرياتين", "5 g"),
        ("Vitamin D3", "فيتامين د٣", "2000 IU"),
        ("Omega-3", "أوميجا ٣", "1000 mg"),
        ("Magnesium", "ماغنيسيوم", "300 mg"),
        ("Multivitamin", "فيتامينات متعددة", "1 cap"),
        ("Electrolytes", "إلكترولايت", "1 scoop"),
    ]

    var body: some View {
        let items = store.state.supplements
        let takenSet = Set(store.state.supplementLogs[today] ?? [])
        let takenCount = items.filter { takenSet.contains($0.id) }.count

        FormaCard {
            HStack(spacing: 8) {
                Image(systemName: "pills.fill").foregroundStyle(FormaTheme.secondary)
                Text("supplements_title").font(.subheadline.weight(.bold)).foregroundStyle(FormaTheme.textPrimary)
                Spacer()
                if !items.isEmpty {
                    FormaPill(
                        text: "\(takenCount)/\(items.count)",
                        background: FormaTheme.secondary.opacity(0.2),
                        foreground: FormaTheme.secondary
                    )
                }
                Button(managing ? "supplements_hide" : "supplements_manage") {
                    FormaHaptics.tap()
                    withAnimation(.easeInOut(duration: 0.2)) { managing.toggle() }
                }
                .font(.caption.weight(.semibold))
            }

            if !managing && !items.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(items) { item in
                            let checked = takenSet.contains(item.id)
                            Button {
                                FormaHaptics.tap()
                                store.attempt { try store.toggleSupplementTaken(item.id, date: today) }
                            } label: {
                                HStack(spacing: 5) {
                                    Image(systemName: checked ? "checkmark.circle.fill" : "circle")
                                        .font(.caption)
                                    Text(item.dose.isEmpty ? item.name : "\(item.name) · \(item.dose)")
                                        .font(.caption.weight(.semibold))
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(checked ? FormaTheme.secondary.opacity(0.18) : FormaTheme.surfaceHigh, in: Capsule())
                                .foregroundStyle(checked ? FormaTheme.secondary : FormaTheme.textPrimary)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            } else if managing && !items.isEmpty {
                VStack(spacing: 8) {
                    ForEach(items) { item in
                        let checked = takenSet.contains(item.id)
                        HStack(spacing: 10) {
                            Button {
                                FormaHaptics.tap()
                                store.attempt { try store.toggleSupplementTaken(item.id, date: today) }
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: checked ? "checkmark.circle.fill" : "circle")
                                        .foregroundStyle(checked ? FormaTheme.secondary : FormaTheme.textMuted)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(item.name)
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundStyle(FormaTheme.textPrimary)
                                        if !item.dose.isEmpty {
                                            Text(item.dose)
                                                .font(.caption2)
                                                .foregroundStyle(FormaTheme.textMuted)
                                        }
                                    }
                                    Spacer()
                                }
                            }
                            .buttonStyle(.plain)

                            Button(role: .destructive) {
                                store.attempt { try store.deleteSupplement(item.id) }
                            } label: {
                                Image(systemName: "trash").font(.caption)
                            }
                        }
                        .padding(10)
                        .background(checked ? FormaTheme.secondary.opacity(0.12) : FormaTheme.surfaceHigh.opacity(0.6), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }

            if managing {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(Array(presets.enumerated()), id: \.offset) { _, preset in
                            let label = arabic ? preset.ar : preset.en
                            if !items.contains(where: { $0.name.caseInsensitiveCompare(label) == .orderedSame }) {
                                Button {
                                    FormaHaptics.tap()
                                    store.attempt { try store.addSupplement(name: label, dose: preset.dose) }
                                } label: {
                                    FormaPill(text: "+ \(label)", background: FormaTheme.surfaceHigh, foreground: FormaTheme.textPrimary)
                                }
                            }
                        }
                        Button {
                            FormaHaptics.tap()
                            customName = ""
                            customDose = ""
                            addingCustom = true
                        } label: {
                            FormaPill(text: "+ \(String(localized: "supplements_add_custom"))", background: FormaTheme.primaryContainer, foreground: FormaTheme.primary)
                        }
                    }
                }
                Text("supplements_disclaimer")
                    .font(.caption2)
                    .foregroundStyle(FormaTheme.textMuted)
            }
        }
        .sheet(isPresented: $addingCustom) {
            NavigationStack {
                Form {
                    TextField("supplements_name_label", text: $customName)
                    TextField("supplements_dose_label", text: $customDose)
                }
                .navigationTitle("supplements_add_custom")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("cancel") { addingCustom = false } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("save") {
                            store.attempt { try store.addSupplement(name: customName, dose: customDose) }
                            addingCustom = false
                        }
                        .disabled(customName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }
}

/// Explicit user-entered personal nutrition & hydration targets sheet (no automatic prescription).
@MainActor struct PhoneGoalsSheet: View {
    @ObservedObject var store: LocalStore
    @Environment(\.dismiss) private var dismiss
    @State private var caloriesText = ""
    @State private var proteinText = ""
    @State private var carbsText = ""
    @State private var fatText = ""
    @State private var waterText = ""
    @State private var heightText = ""
    @State private var initialized = false

    private func parseOptionalDouble(_ raw: String) -> Double? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let latin = trimmed.applyingTransform(.toLatin, reverse: false) ?? trimmed
        let cleaned = latin.replacingOccurrences(of: "٫", with: ".").replacingOccurrences(of: ",", with: ".")
        return Double(cleaned)
    }

    private func parseOptionalInt(_ raw: String) -> Int? {
        parseOptionalDouble(raw).map { Int($0.rounded()) }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("personal_goals_note")
                        .font(.caption)
                        .foregroundStyle(FormaTheme.textMuted)
                }
                Section("personal_goals_title") {
                    TextField("goal_calories_placeholder", text: $caloriesText)
                        .keyboardType(.numberPad)
                    TextField("goal_protein_placeholder", text: $proteinText)
                        .keyboardType(.decimalPad)
                    TextField("goal_carbs_placeholder", text: $carbsText)
                        .keyboardType(.decimalPad)
                    TextField("goal_fat_placeholder", text: $fatText)
                        .keyboardType(.decimalPad)
                    TextField("goal_water_placeholder", text: $waterText)
                        .keyboardType(.numberPad)
                }
                Section {
                    TextField("height_placeholder", text: $heightText)
                        .keyboardType(.decimalPad)
                } footer: {
                    Text("height_note")
                }
                if store.state.calorieTarget != nil || store.state.proteinTarget != nil || store.state.carbsTarget != nil || store.state.fatTarget != nil || store.state.waterGoalMl != nil {
                    Section {
                        Button("clear_personal_goals", role: .destructive) {
                            store.attempt {
                                try store.saveTargets(calories: nil, protein: nil, carbs: nil, fat: nil, waterMl: nil)
                            }
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle("personal_goals_title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("save") {
                        FormaHaptics.success()
                        store.attempt {
                            try store.saveTargets(
                                calories: parseOptionalInt(caloriesText),
                                protein: parseOptionalDouble(proteinText),
                                carbs: parseOptionalDouble(carbsText),
                                fat: parseOptionalDouble(fatText),
                                waterMl: parseOptionalInt(waterText)
                            )
                            try store.saveHeight(parseOptionalDouble(heightText))
                        }
                        dismiss()
                    }
                }
            }
            .onAppear {
                if !initialized {
                    initialized = true
                    caloriesText = store.state.calorieTarget.map(String.init) ?? ""
                    proteinText = store.state.proteinTarget.map { String(Int($0.rounded())) } ?? ""
                    carbsText = store.state.carbsTarget.map { String(Int($0.rounded())) } ?? ""
                    fatText = store.state.fatTarget.map { String(Int($0.rounded())) } ?? ""
                    waterText = store.state.waterGoalMl.map(String.init) ?? ""
                    heightText = store.state.heightCm.map { String(Int($0.rounded())) } ?? ""
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
