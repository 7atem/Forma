import SwiftUI
import Foundation

struct OnlineFoodItem: Identifiable {
    let id: String
    let code: String
    let name: String
    let brand: String
    let kcalPer100g: Double
    let proteinPer100g: Double
    let carbsPer100g: Double
    let fatPer100g: Double
}

enum OpenFoodFactsService {
    private static let userAgent = "Forma/0.6 (iOS; +https://world.openfoodfacts.org)"

    static func search(query: String, preferArabic: Bool) async throws -> [OnlineFoodItem] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }
        let digits = trimmed.filter(\.isNumber)
        if digits.count == trimmed.count, (8...14).contains(digits.count) {
            if let direct = try await fetchBarcode(digits, preferArabic: preferArabic) {
                return [direct]
            }
        }
        guard let encoded = trimmed.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://world.openfoodfacts.org/cgi/search.pl?search_terms=\(encoded)&search_simple=1&action=process&json=1&page_size=15&fields=code,product_name,product_name_ar,brands,nutriments") else {
            return []
        }
        let data = try await get(url: url)
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let products = root["products"] as? [[String: Any]] else {
            return []
        }
        return products.compactMap { parseProduct($0, preferArabic: preferArabic) }
    }

    private static func fetchBarcode(_ code: String, preferArabic: Bool) async throws -> OnlineFoodItem? {
        guard let url = URL(string: "https://world.openfoodfacts.org/api/v2/product/\(code).json?fields=code,product_name,product_name_ar,brands,nutriments") else {
            return nil
        }
        let data = try await get(url: url)
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              (root["status"] as? Int) == 1,
              let product = root["product"] as? [String: Any] else {
            return nil
        }
        return parseProduct(product, preferArabic: preferArabic)
    }

    private static func get(url: URL) async throws -> Data {
        var request = URLRequest(url: url, timeoutInterval: 8.0)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode),
              data.count <= 512 * 1024 else {
            throw URLError(.badServerResponse)
        }
        return data
    }

    private static func parseProduct(_ dict: [String: Any], preferArabic: Bool) -> OnlineFoodItem? {
        let code = (dict["code"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let arName = (dict["product_name_ar"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let baseName = (dict["product_name"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let name = (preferArabic && !arName.isEmpty) ? arName : (baseName.isEmpty ? arName : baseName)
        guard !name.isEmpty, let nutriments = dict["nutriments"] as? [String: Any] else { return nil }
        let kcalRaw = number(nutriments["energy-kcal_100g"]) ?? number(nutriments["energy-kj_100g"]).map { $0 / 4.184 }
        guard let kcal = kcalRaw, kcal.isFinite, kcal >= 0, kcal <= 2000 else { return nil }
        let protein = clampMacro(number(nutriments["proteins_100g"]))
        let carbs = clampMacro(number(nutriments["carbohydrates_100g"]))
        let fat = clampMacro(number(nutriments["fat_100g"]))
        let brand = ((dict["brands"] as? String) ?? "").split(separator: ",").first.map { String($0).trimmingCharacters(in: .whitespaces) } ?? ""
        return OnlineFoodItem(
            id: code.isEmpty ? UUID().uuidString : code,
            code: code,
            name: name,
            brand: brand,
            kcalPer100g: kcal,
            proteinPer100g: protein,
            carbsPer100g: carbs,
            fatPer100g: fat
        )
    }

    private static func number(_ raw: Any?) -> Double? {
        if let d = raw as? Double { return d }
        if let i = raw as? Int { return Double(i) }
        if let s = raw as? String { return Double(s) }
        return nil
    }

    private static func clampMacro(_ value: Double?) -> Double {
        guard let value, value.isFinite, value >= 0, value <= 100 else { return 0 }
        return value
    }
}

@MainActor struct PhoneOnlineFoodSheet: View {
    let slot: String
    let onLog: (PhoneMeal) -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var query = ""
    @State private var grams = 100.0
    @State private var searching = false
    @State private var searched = false
    @State private var failed = false
    @State private var results: [OnlineFoodItem] = []

    private var arabic: Bool { locale.language.languageCode?.identifier == "ar" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("online_food_attribution")
                        .font(.caption)
                        .foregroundStyle(FormaTheme.textMuted)

                    HStack(spacing: 8) {
                        TextField("online_food_hint", text: $query)
                            .submitLabel(.search)
                            .onSubmit { runSearch() }
                            .padding(12)
                            .background(FormaTheme.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(FormaTheme.border, lineWidth: 1))
                        Button {
                            runSearch()
                        } label: {
                            Text("online_food_search_btn")
                                .font(.subheadline.weight(.bold))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .background(FormaTheme.primary, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .foregroundStyle(FormaTheme.onPrimary)
                        }
                        .disabled(query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || searching)
                    }

                    // Portion selector
                    FormaCard {
                        Stepper(value: $grams, in: 10...1000, step: 10) {
                            Text("\(Int(grams)) g")
                                .font(.headline.weight(.bold))
                                .monospacedDigit()
                                .foregroundStyle(FormaTheme.primary)
                        }
                        HStack(spacing: 8) {
                            ForEach([50.0, 100.0, 150.0, 200.0], id: \.self) { preset in
                                Button {
                                    FormaHaptics.tap()
                                    grams = preset
                                } label: {
                                    FormaPill(
                                        text: "\(Int(preset))g",
                                        background: grams == preset ? FormaTheme.primary : FormaTheme.surfaceHigh,
                                        foreground: grams == preset ? FormaTheme.onPrimary : FormaTheme.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    if searching {
                        ProgressView().frame(maxWidth: .infinity).padding(.vertical, 16)
                    } else if failed {
                        Text("online_food_error")
                            .font(.subheadline)
                            .foregroundStyle(.red)
                    } else if searched && results.isEmpty {
                        Text("online_food_empty")
                            .font(.subheadline)
                            .foregroundStyle(FormaTheme.textMuted)
                    }

                    ForEach(results) { item in
                        let factor = grams / 100.0
                        let kcal = item.kcalPer100g * factor
                        let p = item.proteinPer100g * factor
                        let c = item.carbsPer100g * factor
                        let f = item.fatPer100g * factor
                        FormaCard {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.name)
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(FormaTheme.textPrimary)
                                    if !item.brand.isEmpty {
                                        Text(item.brand)
                                            .font(.caption2)
                                            .foregroundStyle(FormaTheme.primary)
                                    }
                                }
                                Spacer()
                                FormaPill(text: "\(Int(kcal.rounded())) kcal")
                            }
                            Text("\(Int(p.rounded()))g P · \(Int(c.rounded()))g C · \(Int(f.rounded()))g F")
                                .font(.caption)
                                .foregroundStyle(FormaTheme.textMuted)
                            Button {
                                FormaHaptics.success()
                                let displayName = item.brand.isEmpty ? item.name : "\(item.name) (\(item.brand))"
                                onLog(PhoneMeal(
                                    date: LocalStore.todayString(),
                                    name: String(displayName.prefix(120)),
                                    calories: kcal,
                                    protein: p,
                                    carbs: c,
                                    fat: f,
                                    slot: slot
                                ))
                                dismiss()
                            } label: {
                                Label("log_meal", systemImage: "plus")
                                    .font(.subheadline.weight(.bold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(FormaTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .foregroundStyle(FormaTheme.primary)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(FormaTheme.background.ignoresSafeArea())
            .navigationTitle("online_food_search")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel") { dismiss() }
                }
            }
        }
    }

    private func runSearch() {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty, !searching else { return }
        searching = true
        failed = false
        Task {
            do {
                let items = try await OpenFoodFactsService.search(query: q, preferArabic: arabic)
                await MainActor.run {
                    results = items
                    searched = true
                    searching = false
                }
            } catch {
                await MainActor.run {
                    results = []
                    failed = true
                    searched = true
                    searching = false
                }
            }
        }
    }
}
