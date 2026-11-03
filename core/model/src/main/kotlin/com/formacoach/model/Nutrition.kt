@file:kotlinx.serialization.UseSerializers(com.formacoach.model.LocalDateSerializer::class)

package com.formacoach.model

import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

enum class NutritionUnit { GRAMS, SERVING }
enum class NutritionSource { LABEL, MANUAL, RECIPE }

@Serializable
data class Nutrients(val calories: Double, val protein: Double, val carbs: Double, val fat: Double) {
    init {
        require(calories.isFinite() && calories in 0.0..20000.0)
        require(listOf(protein, carbs, fat).all {it.isFinite() && it in 0.0..2000.0})
    }
    operator fun times(factor: Double): Nutrients {
        require(factor.isFinite() && factor > 0)
        return Nutrients(calories*factor, protein*factor, carbs*factor, fat*factor)
    }
    operator fun plus(other: Nutrients) = Nutrients(calories+other.calories, protein+other.protein, carbs+other.carbs, fat+other.fat)
}

/** Immutable flat recipe ingredients: editing a food never silently changes a saved recipe. */
@Serializable
data class IngredientSnapshot(
    val name: String,
    val quantity: Double,
    val unit: NutritionUnit,
    val nutrients: Nutrients,
    val source: String,
) {
    init {
        require(name.isNotBlank() && name.length <= 120)
        require(quantity.isFinite() && quantity > 0 && quantity <= 100000)
        require(source.isNotBlank() && source.length <= 4000)
    }
}

@Serializable
data class Food(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val referenceAmount: Double,
    val unit: NutritionUnit,
    val nutrients: Nutrients,
    val source: String,
    val sourceKind: NutritionSource = NutritionSource.MANUAL,
    val ingredients: List<IngredientSnapshot> = emptyList(),
) {
    init {
        require(id.isNotBlank() && id.length <= 80)
        require(name.isNotBlank() && name.length <= 120)
        require(referenceAmount.isFinite() && referenceAmount > 0 && referenceAmount <= 100000)
        require(source.isNotBlank() && source.length <= 4000)
        require(ingredients.size <= 30)
        require((sourceKind == NutritionSource.RECIPE) == ingredients.isNotEmpty())
        if (sourceKind == NutritionSource.RECIPE) {
            require(unit == NutritionUnit.SERVING)
            val total=ingredients.fold(Nutrients(0.0,0.0,0.0,0.0)) {a,i -> a+i.nutrients}
            require(total == nutrients) { "Recipe nutrition must match its ingredient snapshots" }
        }
    }

    fun portion(quantity: Double, date: LocalDate): Meal {
        require(quantity.isFinite() && quantity > 0 && quantity <= 100000)
        val values=nutrients * (quantity/referenceAmount)
        return Meal(date=date,name=name,calories=values.calories,protein=values.protein,carbs=values.carbs,fat=values.fat,
            foodId=id,quantity=quantity,unit=unit,source=source)
    }

    fun ingredient(quantity: Double): IngredientSnapshot {
        val values=portion(quantity,LocalDate.of(2000,1,1))
        return IngredientSnapshot(name,quantity,unit,Nutrients(values.calories,values.protein,values.carbs,values.fat),source)
    }

    companion object {
        fun recipe(name: String, servings: Double, source: String, ingredients: List<IngredientSnapshot>): Food {
            require(ingredients.isNotEmpty())
            val total=ingredients.fold(Nutrients(0.0,0.0,0.0,0.0)) {a,i -> a+i.nutrients}
            return Food(name=name, referenceAmount=servings, unit=NutritionUnit.SERVING, nutrients=total,
                source=source,sourceKind=NutritionSource.RECIPE,ingredients=ingredients)
        }
    }
}
