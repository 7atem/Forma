package com.formacoach.model

import kotlin.math.round

enum class WeightUnit { KG, LB }

/** Display/input conversion helper. Canonical persistence and transfers remain in KG. */
object WeightUnits {
    const val LB_PER_KG = 2.20462262185

    fun fromKg(kg: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.KG) kg else round(kg * LB_PER_KG * 10.0) / 10.0

    fun toKg(displayValue: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.KG) displayValue else round((displayValue / LB_PER_KG) * 1000.0) / 1000.0

    fun step(unit: WeightUnit): Double = if (unit == WeightUnit.KG) 2.5 else 5.0

    fun quickBumps(unit: WeightUnit): List<Double> =
        if (unit == WeightUnit.KG) listOf(2.5, 5.0, 10.0) else listOf(5.0, 10.0, 25.0)
}
