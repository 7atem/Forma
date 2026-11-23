package com.formacoach.coach

import com.formacoach.model.Nutrients

data class FamilyPotShare(
    val fraction: Double,
    val percent: Int,
    val portionNutrients: Nutrients,
)

/**
 * Deterministic "Shared Family Pot" math:
 * Computes a single family member's exact share either by bowl weight (grams / total cooked grams)
 * or by equal family portions (my shares / total family shares).
 */
object FamilyPotCalculator {
    fun byCookedWeight(
        totalPotNutrients: Nutrients,
        totalCookedGrams: Double,
        myBowlGrams: Double,
    ): FamilyPotShare? {
        if (!totalCookedGrams.isFinite() || !myBowlGrams.isFinite()) return null
        if (totalCookedGrams <= 0.0 || myBowlGrams <= 0.0 || myBowlGrams > totalCookedGrams) return null
        val fraction = myBowlGrams / totalCookedGrams
        val nutrients = runCatching { totalPotNutrients * fraction }.getOrNull() ?: return null
        return FamilyPotShare(
            fraction = fraction,
            percent = (fraction * 100.0).toInt().coerceIn(1, 100),
            portionNutrients = nutrients,
        )
    }

    fun byFamilyShares(
        totalPotNutrients: Nutrients,
        totalShares: Double,
        myShares: Double,
    ): FamilyPotShare? {
        if (!totalShares.isFinite() || !myShares.isFinite()) return null
        if (totalShares <= 0.0 || myShares <= 0.0 || myShares > totalShares) return null
        val fraction = myShares / totalShares
        val nutrients = runCatching { totalPotNutrients * fraction }.getOrNull() ?: return null
        return FamilyPotShare(
            fraction = fraction,
            percent = (fraction * 100.0).toInt().coerceIn(1, 100),
            portionNutrients = nutrients,
        )
    }
}
