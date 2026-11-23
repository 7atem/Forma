package com.formacoach.coach

import com.formacoach.model.Food
import com.formacoach.model.Nutrients
import kotlin.math.abs

enum class SwapMatchBy { PROTEIN, CALORIES }

data class FoodSwapOption(
    val food: Food,
    val matchedQuantity: Double,
    val matchedNutrients: Nutrients,
)

/**
 * Pure deterministic local food-swap calculator over the user's own food library.
 * Computes equivalent portions matching protein or calories without prescribing a diet.
 */
object FoodSwapCalculator {
    fun findSwaps(
        target: Food,
        targetQuantity: Double,
        library: List<Food>,
        matchBy: SwapMatchBy,
        limit: Int = 8,
    ): List<FoodSwapOption> {
        if (!targetQuantity.isFinite() || targetQuantity <= 0.0) return emptyList()
        val targetNutrients = target.nutrients * (targetQuantity / target.referenceAmount)
        val goalMetric = when (matchBy) {
            SwapMatchBy.PROTEIN -> targetNutrients.protein
            SwapMatchBy.CALORIES -> targetNutrients.calories
        }
        if (goalMetric <= 0.0) return emptyList()

        return library
            .filter { it.id != target.id }
            .mapNotNull { candidate ->
                val perRef = when (matchBy) {
                    SwapMatchBy.PROTEIN -> candidate.nutrients.protein
                    SwapMatchBy.CALORIES -> candidate.nutrients.calories
                }
                if (perRef <= 0.0) return@mapNotNull null
                val rawQty = candidate.referenceAmount * (goalMetric / perRef)
                if (!rawQty.isFinite() || rawQty <= 0.0 || rawQty > 5000.0) return@mapNotNull null
                val matchedNutrients = runCatching { candidate.nutrients * (rawQty / candidate.referenceAmount) }.getOrNull()
                    ?: return@mapNotNull null
                FoodSwapOption(candidate, rawQty, matchedNutrients)
            }
            .sortedBy { option ->
                // Rank closest overall calorie match first (so high-protein swaps stay calorie-sensible).
                abs(option.matchedNutrients.calories - targetNutrients.calories)
            }
            .take(limit)
    }
}
