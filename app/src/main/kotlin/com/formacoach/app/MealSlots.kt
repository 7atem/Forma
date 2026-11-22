package com.formacoach.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.MealSlot

internal fun mealSlotLabel(value: MealSlot): Int = when (value) {
    MealSlot.UNSPECIFIED -> R.string.meal_slot_unspecified
    MealSlot.BREAKFAST -> R.string.meal_slot_breakfast
    MealSlot.LUNCH -> R.string.meal_slot_lunch
    MealSlot.DINNER -> R.string.meal_slot_dinner
    MealSlot.SNACK -> R.string.meal_slot_snack
}

internal fun mealSlotIcon(value: MealSlot): ImageVector = when (value) {
    MealSlot.UNSPECIFIED -> Icons.Default.Restaurant
    MealSlot.BREAKFAST -> Icons.Default.WbTwilight
    MealSlot.LUNCH -> Icons.Default.WbSunny
    MealSlot.DINNER -> Icons.Default.NightsStay
    MealSlot.SNACK -> Icons.Default.LocalCafe
}

@Composable
internal fun MealSlotPicker(selected: MealSlot, onChange: (MealSlot) -> Unit) {
    Text(
        stringResource(R.string.log_meal_under),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf(MealSlot.UNSPECIFIED) + MealSlot.entries.filter { it != MealSlot.UNSPECIFIED }).forEach { slot ->
            FilterChip(
                selected = selected == slot,
                onClick = { onChange(slot) },
                leadingIcon = { Icon(mealSlotIcon(slot), contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(stringResource(mealSlotLabel(slot)), fontWeight = if (selected == slot) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("meal_slot_${slot.name}"),
            )
        }
    }
}
