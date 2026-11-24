import Foundation

/// Curated offline starter whole-food & Middle Eastern staples catalog grounded in public USDA FoodData Central (CC0)
/// and FAO Near East reference values. Logged meals always store immutable value snapshots.
public struct StarterFoodItem: Identifiable, Equatable, Hashable, Sendable {
    public let id: String
    public let nameEn: String
    public let nameAr: String
    public let referenceGrams: Double
    public let unitLabelEn: String
    public let unitLabelAr: String
    public let calories: Double
    public let protein: Double
    public let carbs: Double
    public let fat: Double
    public let isRegionalStaple: Bool

    public func name(arabic: Bool) -> String { arabic ? nameAr : nameEn }
    public func unitLabel(arabic: Bool) -> String { arabic ? unitLabelAr : unitLabelEn }
}

public enum FoodSwapMetric: String, CaseIterable, Sendable {
    case protein
    case calories
}

public struct FoodSwapMatch: Identifiable, Equatable, Sendable {
    public let food: StarterFoodItem
    public let matchedAmount: Double
    public let calories: Double
    public let protein: Double
    public let carbs: Double
    public let fat: Double
    public var id: String { food.id }
}

public enum StarterFoodsCatalog {
    public static let all: [StarterFoodItem] = [
        StarterFoodItem(id: "starter_chicken_breast", nameEn: "Chicken breast (cooked)", nameAr: "صدور دجاج مشوية", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 165, protein: 31.0, carbs: 0.0, fat: 3.6, isRegionalStaple: false),
        StarterFoodItem(id: "starter_whole_egg", nameEn: "Whole egg (large)", nameAr: "بيضة كاملة كبيرة", referenceGrams: 1, unitLabelEn: "egg", unitLabelAr: "بيضة", calories: 72, protein: 6.3, carbs: 0.4, fat: 4.8, isRegionalStaple: false),
        StarterFoodItem(id: "starter_egg_whites", nameEn: "Egg whites", nameAr: "بياض بيض", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 52, protein: 10.9, carbs: 0.7, fat: 0.2, isRegionalStaple: false),
        StarterFoodItem(id: "starter_beef_lean", nameEn: "Lean beef sirloin (cooked)", nameAr: "لحم بقري صافي مطهو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 201, protein: 30.0, carbs: 0.0, fat: 8.0, isRegionalStaple: false),
        StarterFoodItem(id: "starter_salmon", nameEn: "Atlantic salmon (cooked)", nameAr: "سلمون مطهو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 206, protein: 22.1, carbs: 0.0, fat: 12.4, isRegionalStaple: false),
        StarterFoodItem(id: "starter_tuna_water", nameEn: "Tuna in water (drained)", nameAr: "تونة في الماء مصفاة", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 116, protein: 25.5, carbs: 0.0, fat: 0.8, isRegionalStaple: false),
        StarterFoodItem(id: "starter_shrimp", nameEn: "Shrimp (cooked)", nameAr: "جمبري مطهو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 99, protein: 24.0, carbs: 0.2, fat: 0.3, isRegionalStaple: false),
        StarterFoodItem(id: "starter_greek_yogurt", nameEn: "Greek yogurt (low-fat)", nameAr: "زبادي يوناني قليل الدسم", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 73, protein: 10.0, carbs: 3.9, fat: 1.9, isRegionalStaple: false),
        StarterFoodItem(id: "starter_cottage_cheese", nameEn: "Cottage / Areesh cheese", nameAr: "جبنة قريش قليلة الدسم", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 82, protein: 11.1, carbs: 3.4, fat: 2.3, isRegionalStaple: true),
        StarterFoodItem(id: "starter_whey_scoop", nameEn: "Whey protein (1 scoop)", nameAr: "واي بروتين (سكوب)", referenceGrams: 1, unitLabelEn: "scoop", unitLabelAr: "سكوب", calories: 120, protein: 24.0, carbs: 3.0, fat: 1.5, isRegionalStaple: false),
        StarterFoodItem(id: "starter_white_rice", nameEn: "White rice (cooked)", nameAr: "أرز أبيض مطهو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 130, protein: 2.7, carbs: 28.2, fat: 0.3, isRegionalStaple: false),
        StarterFoodItem(id: "starter_brown_rice", nameEn: "Brown rice (cooked)", nameAr: "أرز بني مطهو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 112, protein: 2.3, carbs: 23.5, fat: 0.8, isRegionalStaple: false),
        StarterFoodItem(id: "starter_oats", nameEn: "Rolled oats (dry)", nameAr: "شوفان كامل جاف", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 389, protein: 16.9, carbs: 66.3, fat: 6.9, isRegionalStaple: false),
        StarterFoodItem(id: "starter_sweet_potato", nameEn: "Sweet potato (baked)", nameAr: "بطاطا حلوة مشوية", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 90, protein: 2.0, carbs: 20.7, fat: 0.2, isRegionalStaple: false),
        StarterFoodItem(id: "starter_potato", nameEn: "White potato (boiled)", nameAr: "بطاطس مسلوقة", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 87, protein: 1.9, carbs: 20.1, fat: 0.1, isRegionalStaple: false),
        StarterFoodItem(id: "starter_pasta", nameEn: "Pasta (cooked)", nameAr: "مكرونة مسلوقة", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 131, protein: 5.0, carbs: 25.0, fat: 1.1, isRegionalStaple: false),
        StarterFoodItem(id: "starter_whole_wheat_bread", nameEn: "Whole wheat / Baladi bread", nameAr: "خبز بلدي / قمح كامل", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 247, protein: 13.0, carbs: 41.3, fat: 3.4, isRegionalStaple: true),
        StarterFoodItem(id: "starter_fava_beans", nameEn: "Fava beans / Foul (cooked)", nameAr: "فول مدمس مطهو بدون زيت", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 110, protein: 7.6, carbs: 19.7, fat: 0.4, isRegionalStaple: true),
        StarterFoodItem(id: "starter_lentils", nameEn: "Lentils (cooked)", nameAr: "عدس مطهو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 116, protein: 9.0, carbs: 20.1, fat: 0.4, isRegionalStaple: true),
        StarterFoodItem(id: "starter_chickpeas", nameEn: "Chickpeas (cooked)", nameAr: "حمص مسلوق", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 164, protein: 8.9, carbs: 27.4, fat: 2.6, isRegionalStaple: true),
        StarterFoodItem(id: "starter_banana", nameEn: "Banana", nameAr: "موز", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 89, protein: 1.1, carbs: 22.8, fat: 0.3, isRegionalStaple: false),
        StarterFoodItem(id: "starter_apple", nameEn: "Apple (raw)", nameAr: "تفاح طازج", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 52, protein: 0.3, carbs: 13.8, fat: 0.2, isRegionalStaple: false),
        StarterFoodItem(id: "starter_dates", nameEn: "Medjool dates", nameAr: "تمر مجدول", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 277, protein: 1.8, carbs: 75.0, fat: 0.2, isRegionalStaple: true),
        StarterFoodItem(id: "starter_olive_oil", nameEn: "Extra virgin olive oil", nameAr: "زيت زيتون بكر", referenceGrams: 10, unitLabelEn: "g", unitLabelAr: "جم", calories: 88, protein: 0.0, carbs: 0.0, fat: 10.0, isRegionalStaple: true),
        StarterFoodItem(id: "starter_peanut_butter", nameEn: "Peanut butter (natural)", nameAr: "زبدة فول سوداني طبيعية", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 588, protein: 25.1, carbs: 20.0, fat: 50.4, isRegionalStaple: false),
        StarterFoodItem(id: "starter_almonds", nameEn: "Almonds (raw)", nameAr: "لوز نيء", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 579, protein: 21.2, carbs: 21.6, fat: 49.9, isRegionalStaple: false),
        StarterFoodItem(id: "starter_tahini", nameEn: "Sesame tahini", nameAr: "طحينة سمسم", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 595, protein: 17.0, carbs: 21.2, fat: 53.8, isRegionalStaple: true),
        StarterFoodItem(id: "starter_avocado", nameEn: "Avocado", nameAr: "أفوكادو", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 160, protein: 2.0, carbs: 8.5, fat: 14.7, isRegionalStaple: false),
        StarterFoodItem(id: "starter_milk_whole", nameEn: "Whole milk", nameAr: "حليب كامل الدسم", referenceGrams: 100, unitLabelEn: "ml", unitLabelAr: "مل", calories: 61, protein: 3.2, carbs: 4.8, fat: 3.3, isRegionalStaple: false),
        StarterFoodItem(id: "starter_milk_skim", nameEn: "Skim milk", nameAr: "حليب خالي الدسم", referenceGrams: 100, unitLabelEn: "ml", unitLabelAr: "مل", calories: 34, protein: 3.4, carbs: 5.0, fat: 0.1, isRegionalStaple: false),
        // Middle Eastern / Arabic everyday staples
        StarterFoodItem(id: "starter_hummus", nameEn: "Hummus (chickpea & tahini)", nameAr: "حمص بالطحينة", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 166, protein: 7.9, carbs: 14.3, fat: 9.6, isRegionalStaple: true),
        StarterFoodItem(id: "starter_falafel", nameEn: "Falafel / Taameya (baked)", nameAr: "طعمية / فلافل مشوية", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 230, protein: 11.2, carbs: 26.8, fat: 8.9, isRegionalStaple: true),
        StarterFoodItem(id: "starter_labneh", nameEn: "Labneh (strained yogurt)", nameAr: "لبنة", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 140, protein: 8.5, carbs: 5.2, fat: 9.5, isRegionalStaple: true),
        StarterFoodItem(id: "starter_halloumi", nameEn: "Halloumi cheese (grilled)", nameAr: "جبنة حلوم مشوية", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 321, protein: 21.4, carbs: 2.2, fat: 25.0, isRegionalStaple: true),
        StarterFoodItem(id: "starter_kofta_grilled", nameEn: "Grilled beef kofta", nameAr: "كفتة لحم مشوية", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 235, protein: 22.0, carbs: 2.5, fat: 15.0, isRegionalStaple: true),
        StarterFoodItem(id: "starter_shish_tawook", nameEn: "Shish tawook (grilled chicken)", nameAr: "شيش طاووق مشوي", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 168, protein: 26.5, carbs: 2.8, fat: 5.4, isRegionalStaple: true),
        StarterFoodItem(id: "starter_shawarma_chicken", nameEn: "Chicken shawarma (meat only)", nameAr: "شاورما دجاج بدون خبز", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 195, protein: 25.0, carbs: 3.2, fat: 9.0, isRegionalStaple: true),
        StarterFoodItem(id: "starter_lentil_soup", nameEn: "Red lentil soup", nameAr: "شوربة عدس", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 75, protein: 4.8, carbs: 12.2, fat: 0.9, isRegionalStaple: true),
        StarterFoodItem(id: "starter_molokhia", nameEn: "Molokhia stew", nameAr: "ملوخية مطهوة", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 48, protein: 3.1, carbs: 5.4, fat: 1.6, isRegionalStaple: true),
        StarterFoodItem(id: "starter_koshary", nameEn: "Koshary (rice, lentils & pasta)", nameAr: "كشري مصري", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 156, protein: 5.6, carbs: 29.8, fat: 1.6, isRegionalStaple: true),
        StarterFoodItem(id: "starter_baba_ganoush", nameEn: "Baba ganoush", nameAr: "بابا غنوج", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 128, protein: 3.2, carbs: 9.4, fat: 8.8, isRegionalStaple: true),
        StarterFoodItem(id: "starter_shakshuka", nameEn: "Shakshuka (eggs & tomato)", nameAr: "شكشوكة بالبيض والطماطم", referenceGrams: 100, unitLabelEn: "g", unitLabelAr: "جم", calories: 108, protein: 6.2, carbs: 5.8, fat: 6.8, isRegionalStaple: true),
    ]

    /// Pure deterministic local food-swap calculator matching protein or calories without prescribing a diet.
    public static func findSwaps(
        target: StarterFoodItem,
        targetAmount: Double,
        matchBy: FoodSwapMetric,
        limit: Int = 8
    ) -> [FoodSwapMatch] {
        guard targetAmount.isFinite, targetAmount > 0 else { return [] }
        let scale = targetAmount / target.referenceGrams
        let targetCal = target.calories * scale
        let targetPro = target.protein * scale
        let goal = matchBy == .protein ? targetPro : targetCal
        guard goal > 0 else { return [] }
        return all
            .filter { $0.id != target.id }
            .compactMap { candidate -> FoodSwapMatch? in
                let perRef = matchBy == .protein ? candidate.protein : candidate.calories
                guard perRef > 0 else { return nil }
                let qty = candidate.referenceGrams * (goal / perRef)
                guard qty.isFinite, qty > 0, qty <= 5000 else { return nil }
                let factor = qty / candidate.referenceGrams
                return FoodSwapMatch(
                    food: candidate,
                    matchedAmount: qty,
                    calories: candidate.calories * factor,
                    protein: candidate.protein * factor,
                    carbs: candidate.carbs * factor,
                    fat: candidate.fat * factor
                )
            }
            .sorted { abs($0.calories - targetCal) < abs($1.calories - targetCal) }
            .prefix(limit)
            .map { $0 }
    }
}
