import SwiftUI

@MainActor struct PhoneFamilyPotSheet: View {
    let slot: String
    let onSave: (PhoneMeal) -> Void
    @Environment(\.dismiss) private var dismiss
    @AppStorage("last_pot_name") private var savedLeftoverName = ""
    @AppStorage("last_pot_kcal") private var savedLeftoverKcal = 0.0
    @AppStorage("last_pot_pro") private var savedLeftoverPro = 0.0
    @AppStorage("last_pot_carbs") private var savedLeftoverCarbs = 0.0
    @AppStorage("last_pot_fat") private var savedLeftoverFat = 0.0
    @AppStorage("last_pot_tg") private var savedLeftoverTg = 1200.0
    @AppStorage("last_pot_ts") private var savedLeftoverTs = 4.0

    @State private var mode = 0 // 0 = weight, 1 = shares, 2 = quick family plate
    @State private var dishName = ""
    @State private var potKcal = 1800.0
    @State private var potPro = 100.0
    @State private var potCarbs = 180.0
    @State private var potFat = 55.0
    @State private var totalGrams = 1200.0
    @State private var myGrams = 300.0
    @State private var totalShares = 4.0
    @State private var myShares = 1.0
    @State private var plateSize = 1 // 0 = light, 1 = medium, 2 = full
    @State private var addOilBuffer = false

    private var shareFraction: Double {
        if mode == 0 {
            guard totalGrams > 0 else { return 0 }
            return min(max(myGrams / totalGrams, 0), 1)
        } else {
            guard totalShares > 0 else { return 0 }
            return min(max(myShares / totalShares, 0), 1)
        }
    }

    private var portionValues: (kcal: Double, p: Double, c: Double, f: Double) {
        if mode == 2 {
            let base: (Double, Double, Double, Double) = switch plateSize {
            case 0: (450, 25, 50, 15)
            case 2: (950, 48, 105, 34)
            default: (700, 35, 78, 24)
            }
            return addOilBuffer ? (base.0 + 120, base.1, base.2, base.3 + 13.5) : base
        }
        let frac = shareFraction
        return (potKcal * frac, potPro * frac, potCarbs * frac, potFat * frac)
    }

    var body: some View {
        let vals = portionValues
        NavigationStack {
            Form {
                if !savedLeftoverName.isEmpty {
                    Section {
                        Button {
                            FormaHaptics.tap()
                            mode = 0
                            dishName = savedLeftoverName
                            potKcal = savedLeftoverKcal
                            potPro = savedLeftoverPro
                            potCarbs = savedLeftoverCarbs
                            potFat = savedLeftoverFat
                            totalGrams = savedLeftoverTg
                            totalShares = savedLeftoverTs
                        } label: {
                            Label("\(String(localized: "family_pot_leftovers_prefix")) \(savedLeftoverName)", systemImage: "arrow.counterclockwise")
                        }
                    }
                }

                Section {
                    Picker("family_pot_title", selection: $mode) {
                        Text("family_pot_by_weight").tag(0)
                        Text("family_pot_by_shares").tag(1)
                        Text("family_pot_ballpark").tag(2)
                    }
                    .pickerStyle(.segmented)
                    TextField("family_pot_name", text: $dishName)
                }

                if mode == 2 {
                    Section("family_pot_ballpark_note") {
                        Picker("family_pot_ballpark", selection: $plateSize) {
                            Text("plate_light").tag(0)
                            Text("plate_medium").tag(1)
                            Text("plate_full").tag(2)
                        }
                        .pickerStyle(.segmented)
                        Toggle("plate_oil_buffer", isOn: $addOilBuffer)
                    }
                } else {
                    Section("family_pot_intro") {
                        Stepper("\(String(localized: "family_pot_total_kcal")): \(Int(potKcal))", value: $potKcal, in: 50...15000, step: 50)
                        Stepper("\(String(localized: "family_pot_total_protein")): \(Int(potPro))g", value: $potPro, in: 0...1000, step: 5)
                        Stepper("\(String(localized: "family_pot_total_carbs")): \(Int(potCarbs))g", value: $potCarbs, in: 0...1500, step: 5)
                        Stepper("\(String(localized: "family_pot_total_fat")): \(Int(potFat))g", value: $potFat, in: 0...800, step: 5)
                    }
                    if mode == 0 {
                        Section {
                            Stepper("\(String(localized: "family_pot_total_grams")): \(Int(totalGrams))g", value: $totalGrams, in: 100...10000, step: 50)
                            Stepper("\(String(localized: "family_pot_my_grams")): \(Int(myGrams))g", value: $myGrams, in: 25...max(25, totalGrams), step: 25)
                        }
                    } else {
                        Section {
                            Stepper("\(String(localized: "family_pot_total_shares")): \(Int(totalShares))", value: $totalShares, in: 1...20, step: 1)
                            Stepper("\(String(localized: "family_pot_my_shares")): \(myShares.formatted())", value: $myShares, in: 0.5...max(0.5, totalShares), step: 0.5)
                        }
                    }
                }

                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        if mode != 2 {
                            Text("\(Int((shareFraction * 100).rounded()))%")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(FormaTheme.primary)
                            ProgressView(value: shareFraction)
                                .tint(FormaTheme.primary)
                        }
                        Text("\(Int(vals.kcal.rounded())) kcal · \(Int(vals.p.rounded()))g P · \(Int(vals.c.rounded()))g C · \(Int(vals.f.rounded()))g F")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(FormaTheme.textPrimary)
                    }
                    Button {
                        FormaHaptics.success()
                        let trimmed = dishName.trimmingCharacters(in: .whitespacesAndNewlines)
                        let finalName = trimmed.isEmpty ? String(localized: "family_plate_default_name") : trimmed
                        if mode != 2 && !trimmed.isEmpty {
                            savedLeftoverName = trimmed
                            savedLeftoverKcal = potKcal
                            savedLeftoverPro = potPro
                            savedLeftoverCarbs = potCarbs
                            savedLeftoverFat = potFat
                            savedLeftoverTg = totalGrams
                            savedLeftoverTs = totalShares
                        }
                        onSave(PhoneMeal(
                            date: LocalStore.todayString(),
                            name: String(finalName.prefix(120)),
                            calories: vals.kcal,
                            protein: vals.p,
                            carbs: vals.c,
                            fat: vals.f,
                            slot: slot
                        ))
                        dismiss()
                    } label: {
                        Text("save")
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                    }
                    .disabled(mode != 2 && dishName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .navigationTitle("family_pot_title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel") { dismiss() }
                }
            }
        }
    }
}
