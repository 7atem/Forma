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
                            Text("\(Int((food.calories * scale).rounded())) kcal · P \(Int((food.protein * scale).rounded()))g · C \(Int((food.carbs * scale).rounded()))g · F \(Int((food.fat * scale).rounded()))g")
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
                                    Text("\(Int(item.referenceGrams)) \(item.unitLabel(arabic: arabic)) · \(Int(item.calories)) kcal · P \(Int(item.protein.rounded()))g")
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
                                    Text("\(Int(option.matchedAmount.rounded())) \(option.food.unitLabel(arabic: arabic)) · \(Int(option.calories.rounded())) kcal · P \(Int(option.protein.rounded()))g")
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
