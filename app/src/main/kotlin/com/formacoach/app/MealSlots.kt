package com.formacoach.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.model.MealSlot

internal fun mealSlotLabel(value:MealSlot):Int=when(value) {
    MealSlot.UNSPECIFIED->R.string.meal_slot_unspecified
    MealSlot.BREAKFAST->R.string.meal_slot_breakfast
    MealSlot.LUNCH->R.string.meal_slot_lunch
    MealSlot.DINNER->R.string.meal_slot_dinner
    MealSlot.SNACK->R.string.meal_slot_snack
}

@Composable
internal fun MealSlotPicker(selected:MealSlot,onChange:(MealSlot)->Unit) {
    Text(stringResource(R.string.log_meal_under),style=MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        (listOf(MealSlot.UNSPECIFIED)+MealSlot.entries.filter {it!=MealSlot.UNSPECIFIED}).forEach {slot ->
            FilterChip(selected==slot,onClick={onChange(slot)},label={Text(stringResource(mealSlotLabel(slot)))},modifier=Modifier.testTag("meal_slot_${slot.name}"))
        }
    }
}
