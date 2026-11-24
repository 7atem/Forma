package com.formacoach.model

/**
 * Curated offline starter whole-food catalog grounded in public-domain USDA FoodData Central
 * reference values. Users explicitly import any item into their own editable library; logged
 * meals always retain immutable snapshots.
 */
data class StarterFoodEntry(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val referenceAmount: Double,
    val unit: NutritionUnit,
    val nutrients: Nutrients,
    val source: String = "USDA FoodData Central (CC0)",
) {
    fun toFood(arabic: Boolean): Food = Food(
        id = id,
        name = if (arabic) nameAr else nameEn,
        referenceAmount = referenceAmount,
        unit = unit,
        nutrients = nutrients,
        source = source,
        sourceKind = NutritionSource.LABEL,
    )
}

object StarterFoods {
    val all: List<StarterFoodEntry> = listOf(
        StarterFoodEntry("starter_chicken_breast", "Chicken breast (cooked)", "صدور دجاج مشوية", 100.0, NutritionUnit.GRAMS, Nutrients(165.0, 31.0, 0.0, 3.6)),
        StarterFoodEntry("starter_whole_egg", "Whole egg (large)", "بيضة كاملة كبيرة", 1.0, NutritionUnit.SERVING, Nutrients(72.0, 6.3, 0.4, 4.8)),
        StarterFoodEntry("starter_egg_whites", "Egg whites (liquid)", "بياض بيض", 100.0, NutritionUnit.GRAMS, Nutrients(52.0, 10.9, 0.7, 0.2)),
        StarterFoodEntry("starter_beef_lean", "Lean beef sirloin (cooked)", "لحم بقري صافي مطهو", 100.0, NutritionUnit.GRAMS, Nutrients(201.0, 30.0, 0.0, 8.0)),
        StarterFoodEntry("starter_salmon", "Atlantic salmon (cooked)", "سلمون مطهو", 100.0, NutritionUnit.GRAMS, Nutrients(206.0, 22.1, 0.0, 12.4)),
        StarterFoodEntry("starter_tuna_water", "Tuna canned in water (drained)", "تونة في الماء مصفاة", 100.0, NutritionUnit.GRAMS, Nutrients(116.0, 25.5, 0.0, 0.8)),
        StarterFoodEntry("starter_shrimp", "Shrimp (cooked)", "جمبري مطهو", 100.0, NutritionUnit.GRAMS, Nutrients(99.0, 24.0, 0.2, 0.3)),
        StarterFoodEntry("starter_greek_yogurt", "Greek yogurt (plain, low-fat)", "زبادي يوناني سادة قليل الدسم", 100.0, NutritionUnit.GRAMS, Nutrients(73.0, 10.0, 3.9, 1.9)),
        StarterFoodEntry("starter_cottage_cheese", "Cottage / Areesh cheese (low-fat)", "جبنة قريش قليلة الدسم", 100.0, NutritionUnit.GRAMS, Nutrients(82.0, 11.1, 3.4, 2.3)),
        StarterFoodEntry("starter_whey_scoop", "Whey protein (1 standard scoop)", "واي بروتين (سكوب قياسي)", 1.0, NutritionUnit.SERVING, Nutrients(120.0, 24.0, 3.0, 1.5), "Standard nutrition label reference"),
        StarterFoodEntry("starter_white_rice", "White rice (cooked)", "أرز أبيض مطهو", 100.0, NutritionUnit.GRAMS, Nutrients(130.0, 2.7, 28.2, 0.3)),
        StarterFoodEntry("starter_brown_rice", "Brown rice (cooked)", "أرز بني مطهو", 100.0, NutritionUnit.GRAMS, Nutrients(112.0, 2.3, 23.5, 0.8)),
        StarterFoodEntry("starter_oats", "Rolled oats (dry)", "شوفان كامل جاف", 100.0, NutritionUnit.GRAMS, Nutrients(389.0, 16.9, 66.3, 6.9)),
        StarterFoodEntry("starter_sweet_potato", "Sweet potato (baked)", "بطاطا حلوة مشوية", 100.0, NutritionUnit.GRAMS, Nutrients(90.0, 2.0, 20.7, 0.2)),
        StarterFoodEntry("starter_potato", "White potato (boiled)", "بطاطس مسلوقة", 100.0, NutritionUnit.GRAMS, Nutrients(87.0, 1.9, 20.1, 0.1)),
        StarterFoodEntry("starter_pasta", "Pasta (cooked)", "مكرونة مسلوقة", 100.0, NutritionUnit.GRAMS, Nutrients(131.0, 5.0, 25.0, 1.1)),
        StarterFoodEntry("starter_whole_wheat_bread", "Whole wheat / Baladi bread", "خبز بلدي / قمح كامل", 100.0, NutritionUnit.GRAMS, Nutrients(247.0, 13.0, 41.3, 3.4)),
        StarterFoodEntry("starter_fava_beans", "Fava beans / Foul (cooked)", "فول مدمس مطهو بدون زيت", 100.0, NutritionUnit.GRAMS, Nutrients(110.0, 7.6, 19.7, 0.4)),
        StarterFoodEntry("starter_lentils", "Lentils (cooked)", "عدس مطهو", 100.0, NutritionUnit.GRAMS, Nutrients(116.0, 9.0, 20.1, 0.4)),
        StarterFoodEntry("starter_chickpeas", "Chickpeas (cooked)", "حمص مسلوق", 100.0, NutritionUnit.GRAMS, Nutrients(164.0, 8.9, 27.4, 2.6)),
        StarterFoodEntry("starter_banana", "Banana (medium)", "موزة متوسطة", 100.0, NutritionUnit.GRAMS, Nutrients(89.0, 1.1, 22.8, 0.3)),
        StarterFoodEntry("starter_apple", "Apple (raw)", "تفاح طازج", 100.0, NutritionUnit.GRAMS, Nutrients(52.0, 0.3, 13.8, 0.2)),
        StarterFoodEntry("starter_dates", "Medjool dates", "تمر مجدول", 100.0, NutritionUnit.GRAMS, Nutrients(277.0, 1.8, 75.0, 0.2)),
        StarterFoodEntry("starter_olive_oil", "Extra virgin olive oil", "زيت زيتون بكر", 10.0, NutritionUnit.GRAMS, Nutrients(88.0, 0.0, 0.0, 10.0)),
        StarterFoodEntry("starter_peanut_butter", "Peanut butter (natural)", "زبدة فول سوداني طبيعية", 100.0, NutritionUnit.GRAMS, Nutrients(588.0, 25.1, 20.0, 50.4)),
        StarterFoodEntry("starter_almonds", "Almonds (raw)", "لوز نيء", 100.0, NutritionUnit.GRAMS, Nutrients(579.0, 21.2, 21.6, 49.9)),
        StarterFoodEntry("starter_tahini", "Sesame tahini", "طحينة سمسم", 100.0, NutritionUnit.GRAMS, Nutrients(595.0, 17.0, 21.2, 53.8)),
        StarterFoodEntry("starter_avocado", "Avocado", "أفوكادو", 100.0, NutritionUnit.GRAMS, Nutrients(160.0, 2.0, 8.5, 14.7)),
        StarterFoodEntry("starter_milk_whole", "Whole milk", "حليب كامل الدسم", 100.0, NutritionUnit.GRAMS, Nutrients(61.0, 3.2, 4.8, 3.3)),
        StarterFoodEntry("starter_milk_skim", "Skim milk", "حليب خالي الدسم", 100.0, NutritionUnit.GRAMS, Nutrients(34.0, 3.4, 5.0, 0.1)),
        // Middle Eastern / Arabic everyday staples (USDA FDC CC0 & FAO Near East reference values)
        StarterFoodEntry("starter_hummus", "Hummus (chickpea & tahini)", "حمص بالطحينة", 100.0, NutritionUnit.GRAMS, Nutrients(166.0, 7.9, 14.3, 9.6)),
        StarterFoodEntry("starter_falafel", "Falafel / Taameya (baked)", "طعمية / فلافل مشوية", 100.0, NutritionUnit.GRAMS, Nutrients(230.0, 11.2, 26.8, 8.9), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_labneh", "Labneh (strained yogurt)", "لبنة", 100.0, NutritionUnit.GRAMS, Nutrients(140.0, 8.5, 5.2, 9.5), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_halloumi", "Halloumi cheese (grilled)", "جبنة حلوم مشوية", 100.0, NutritionUnit.GRAMS, Nutrients(321.0, 21.4, 2.2, 25.0)),
        StarterFoodEntry("starter_kofta_grilled", "Grilled beef kofta", "كفتة لحم مشوية", 100.0, NutritionUnit.GRAMS, Nutrients(235.0, 22.0, 2.5, 15.0), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_shish_tawook", "Shish tawook (grilled chicken)", "شيش طاووق مشوي", 100.0, NutritionUnit.GRAMS, Nutrients(168.0, 26.5, 2.8, 5.4), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_shawarma_chicken", "Chicken shawarma (meat only)", "شاورما دجاج بدون خبز", 100.0, NutritionUnit.GRAMS, Nutrients(195.0, 25.0, 3.2, 9.0), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_lentil_soup", "Red lentil soup", "شوربة عدس", 100.0, NutritionUnit.GRAMS, Nutrients(75.0, 4.8, 12.2, 0.9), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_molokhia", "Molokhia stew", "ملوخية مطهوة", 100.0, NutritionUnit.GRAMS, Nutrients(48.0, 3.1, 5.4, 1.6), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_koshary", "Koshary (rice, lentils & pasta)", "كشري مصري", 100.0, NutritionUnit.GRAMS, Nutrients(156.0, 5.6, 29.8, 1.6), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_baba_ganoush", "Baba ganoush", "بابا غنوج", 100.0, NutritionUnit.GRAMS, Nutrients(128.0, 3.2, 9.4, 8.8), "FAO Near East / USDA reference"),
        StarterFoodEntry("starter_shakshuka", "Shakshuka (eggs & tomato)", "شكشوكة بالبيض والطماطم", 100.0, NutritionUnit.GRAMS, Nutrients(108.0, 6.2, 5.8, 6.8), "FAO Near East / USDA reference"),
    )
}
