package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.*
import java.time.LocalDate

@Composable
internal fun EatContent(state: AppState, today: LocalDate, vm: FitnessViewModel) {
    var day by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val date = LocalDate.ofEpochDay(day)
    var library by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Meal?>(null) }
    var foodId by rememberSaveable { mutableStateOf<String?>(null) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var slot by rememberSaveable { mutableStateOf(MealSlot.UNSPECIFIED) }
    val food = state.snapshot.foods.firstOrNull { it.id == foodId }
    val portion = runCatching { food?.portion(requireNotNull(NumericInput.decimal(quantity)), date)?.copy(slot = slot) }.getOrNull()
    val logged = state.snapshot.meals.filter { it.date == date }
    val totalKcal = logged.sumOf { it.calories }
    val totalProtein = logged.sumOf { it.protein }
    val totalCarbs = logged.sumOf { it.carbs }
    val totalFat = logged.sumOf { it.fat }

    PageHeading(R.string.eat_title)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateChoice(date, today, { day = it.toEpochDay() }, "food_date")
        Spacer(Modifier.weight(1f))
        FilledTonalButton(
            onClick = { library = !library },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("food_library"),
        ) {
            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (library) R.string.close_library else R.string.food_library), fontWeight = FontWeight.SemiBold)
        }
    }
    CopyMealDay(state, date, today, vm)

    // Visual Macro Summary Dashboard
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.food_for_date, dateLabel(date)), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.eat_title, R.string.logged_only)
        }
        val calTarget = state.snapshot.profile?.caloriesTarget
        val proTarget = state.snapshot.profile?.proteinTarget
        if (calTarget != null || proTarget != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                calTarget?.let { target ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressRing(
                            progress = (totalKcal / target).toFloat(),
                            valueText = number(totalKcal, 0),
                            labelText = "/ ${number(target.toDouble(), 0)}",
                            size = 96.dp,
                            strokeWidth = 9.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(stringResource(R.string.calories_logged), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } ?: MetricTile(number(totalKcal, 0), stringResource(R.string.calories_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.primary)
                proTarget?.let { target ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressRing(
                            progress = (totalProtein / target).toFloat(),
                            valueText = number(totalProtein, 0),
                            labelText = "/ ${number(target, 0)}g",
                            size = 96.dp,
                            strokeWidth = 9.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(stringResource(R.string.protein_logged), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } ?: MetricTile(number(totalProtein), stringResource(R.string.protein_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(number(totalKcal, 0), stringResource(R.string.calories_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.primary)
                MetricTile(number(totalProtein), stringResource(R.string.protein_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
            }
        }
        if (totalKcal > 0) {
            MacroDonutRing(protein = totalProtein, carbs = totalCarbs, fat = totalFat)
            MacroBreakdownBar(protein = totalProtein, carbs = totalCarbs, fat = totalFat)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillBadge(
                    "${stringResource(R.string.protein)} ${number(totalProtein, 0)}g",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                )
                PillBadge(
                    "${stringResource(R.string.carbs)} ${number(totalCarbs, 0)}g",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.tertiary,
                )
                PillBadge(
                    "${stringResource(R.string.fat)} ${number(totalFat, 0)}g",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
        calTarget?.let { target ->
            Text(stringResource(R.string.logged_calorie_target, number(totalKcal, 0), number(target.toDouble(), 0)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { (totalKcal / target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                drawStopIndicator = {},
            )
        }
        proTarget?.let { target ->
            Text(stringResource(R.string.logged_protein_target, number(totalProtein), number(target)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { (totalProtein / target).toFloat().coerceIn(0f, 1f) },
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                drawStopIndicator = {},
            )
        }
    }

    if (library) FoodLibrary(state, vm)

    if (state.snapshot.foods.isEmpty()) {
        val arabic = LocalConfiguration.current.locales[0].language == "ar"
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.starter_foods_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.starter_foods_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(
                    onClick = { vm.saveFoods(StarterFoods.all.map { it.toFood(arabic) }) },
                    enabled = !state.busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("load_starter_foods"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.load_all_starters), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    MealSlotPicker(slot) { slot = it }

    val quickRepeats = state.snapshot.meals.distinctBy { listOf(it.name, it.calories, it.protein, it.carbs, it.fat, it.source) }.take(6)
    if (quickRepeats.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickRepeats.forEach { recent ->
                AssistChip(
                    onClick = { vm.addMeal(recent.copy(id = java.util.UUID.randomUUID().toString(), date = date, slot = slot)) {} },
                    enabled = !state.busy,
                    leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("${recent.name} · ${number(recent.calories, 0)}", maxLines = 1) },
                )
            }
        }
    }

    if (state.snapshot.foods.isNotEmpty()) {
        InfoCard {
            Text(stringResource(R.string.log_saved_food), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("choose_food"),
                ) {
                    Text(food?.name ?: stringResource(R.string.choose_food), Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.snapshot.foods.forEach { item ->
                        DropdownMenuItem(text = { Text(item.name) }, onClick = { foodId = item.id; expanded = false })
                    }
                }
            }
            food?.let { item ->
                Text(
                    stringResource(R.string.reference_portion, number(item.referenceAmount), stringResource(unitLabel(item.unit))),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Input(
                    if (item.unit == NutritionUnit.GRAMS) R.string.quantity_grams else R.string.quantity_servings,
                    quantity,
                    { quantity = it },
                    "portion_quantity",
                )
                Text(stringResource(R.string.source_value, item.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                portion?.let { NutrientSummary(it.calories, it.protein, it.carbs, it.fat) }
                Button(
                    onClick = { portion?.let { vm.addMeal(it) { quantity = "" } } },
                    enabled = portion != null && !state.busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("log_portion"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.save_meal), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    InfoCard { MealFields(null, date, today, state.busy, defaultSlot = slot, onSave = { meal, done -> vm.addMeal(meal, done) }) }

    Text(stringResource(R.string.food_for_date, dateLabel(date)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Input(R.string.search_meals, search, { search = it }, "meal_search", numeric = false)
    val meals = state.snapshot.meals.filter { it.date == date && (search.isBlank() || it.name.contains(search, true)) }
    if (meals.isEmpty()) Text(stringResource(R.string.no_meals), color = MaterialTheme.colorScheme.onSurfaceVariant)
    MealSlot.entries.forEach { group ->
        val values = meals.filter { it.slot == group }
        if (values.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("meal_group_${group.name}"),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(mealSlotIcon(group), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(
                            R.string.meal_group_heading,
                            stringResource(mealSlotLabel(group)),
                            number(values.sumOf { it.calories }, 0),
                            number(values.sumOf { it.protein }),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        values.forEach { meal ->
            key(meal.id) {
                InfoCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(meal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            meal.quantity?.let {
                                Text(
                                    stringResource(R.string.portion_record, number(it), stringResource(unitLabel(checkNotNull(meal.unit)))),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        PillBadge("${number(meal.calories, 0)} kcal")
                    }
                    NutrientSummary(meal.calories, meal.protein, meal.carbs, meal.fat)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (meal.source.isBlank()) stringResource(R.string.manual_source) else stringResource(R.string.source_value, meal.source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { editing = meal }, enabled = !state.busy) { Text(stringResource(R.string.edit)) }
                        ConfirmButton(R.string.remove, stringResource(R.string.delete_meal_note), !state.busy) { vm.removeMeal(meal.id) }
                    }
                }
            }
        }
    }

    val recentMeals = state.snapshot.meals.distinctBy { listOf(it.name, it.calories, it.protein, it.carbs, it.fat, it.source) }.take(5)
    if (recentMeals.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.reuse_meal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.reuse_selected_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        recentMeals.forEach { meal ->
            InfoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    PillBadge("${number(meal.calories, 0)} kcal")
                }
                NutrientSummary(meal.calories, meal.protein, meal.carbs, meal.fat)
                OutlinedButton(
                    onClick = { vm.addMeal(meal.copy(id = java.util.UUID.randomUUID().toString(), date = date, slot = slot)) {} },
                    enabled = !state.busy,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.repeat_for_date, dateLabel(date)))
                }
            }
        }
    }

    editing?.let { meal ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit_meal), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                    MealFields(meal, date, today, state.busy, onSave = { value, done -> vm.addMeal(value) { done(); editing = null } })
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MealFields(initial: Meal?, date: LocalDate, today: LocalDate, busy: Boolean, defaultSlot: MealSlot = MealSlot.UNSPECIFIED, onSave: (Meal, () -> Unit) -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var calories by rememberSaveable(initial?.id) { mutableStateOf(initial?.calories?.toString().orEmpty()) }
    var protein by rememberSaveable(initial?.id) { mutableStateOf(initial?.protein?.toString().orEmpty()) }
    var carbs by rememberSaveable(initial?.id) { mutableStateOf(initial?.carbs?.toString().orEmpty()) }
    var fat by rememberSaveable(initial?.id) { mutableStateOf(initial?.fat?.toString().orEmpty()) }
    var source by rememberSaveable(initial?.id) { mutableStateOf(initial?.source.orEmpty()) }
    var editedDay by rememberSaveable(initial?.id) { mutableLongStateOf((initial?.date ?: date).toEpochDay()) }
    var editedSlot by rememberSaveable(initial?.id) { mutableStateOf(initial?.slot ?: defaultSlot) }
    val meal = runCatching {
        Meal(
            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
            date = if (initial != null) LocalDate.ofEpochDay(editedDay) else date,
            name = name.trim(),
            calories = requireNotNull(NumericInput.decimal(calories)),
            protein = requireNotNull(NumericInput.decimal(protein)),
            carbs = requireNotNull(NumericInput.decimal(carbs)),
            fat = requireNotNull(NumericInput.decimal(fat)),
            source = source.trim(),
            slot = if (initial == null) defaultSlot else editedSlot,
        )
    }.getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.manual_meal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (initial != null) DateChoice(LocalDate.ofEpochDay(editedDay), today, { editedDay = it.toEpochDay() }, "edit_meal_date")
        if (initial != null) MealSlotPicker(editedSlot) { editedSlot = it }
        Text(stringResource(R.string.portion_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Input(R.string.meal_name, name, { name = it }, "meal_name", numeric = false)
        FormFieldPair(
            first = { Input(R.string.calories, calories, { calories = it }, "meal_calories") },
            second = { Input(R.string.protein, protein, { protein = it }, "meal_protein") },
        )
        FormFieldPair(
            first = { Input(R.string.carbs, carbs, { carbs = it }, "meal_carbs") },
            second = { Input(R.string.fat, fat, { fat = it }, "meal_fat") },
        )
        val pVal = NumericInput.decimal(protein)
        val cVal = NumericInput.decimal(carbs)
        val fVal = NumericInput.decimal(fat)
        if (pVal != null && cVal != null && fVal != null) {
            val computed = kotlin.math.round(com.formacoach.coach.TrainingHistory.macroCalories(pVal, cVal, fVal)).toInt()
            OutlinedButton(
                onClick = { calories = computed.toString() },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("${stringResource(R.string.calc_from_macros)} ($computed kcal)")
            }
        }
        Input(R.string.source_optional, source, { source = it }, "meal_source", numeric = false)
        if (initial != null) Text(stringResource(R.string.manual_correction_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                meal?.let {
                    onSave(it) {
                        if (initial == null) {
                            name = ""
                            calories = ""
                            protein = ""
                            carbs = ""
                            fat = ""
                            source = ""
                        }
                    }
                }
            },
            enabled = meal != null && !busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_meal"),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.save_meal), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FoodLibrary(state: AppState, vm: FitnessViewModel) {
    var editor by remember { mutableStateOf<Food?>(null) }
    var create by rememberSaveable { mutableStateOf(false) }
    var recipe by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    InfoCard {
        Text(stringResource(R.string.food_library), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.library_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { create = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.testTag("new_food")) {
                Text(stringResource(R.string.new_food))
            }
            OutlinedButton(onClick = { recipe = true }, enabled = state.snapshot.foods.isNotEmpty(), shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.new_recipe))
            }
        }
        val arabic = LocalConfiguration.current.locales[0].language == "ar"
        val existingIds = state.snapshot.foods.map { it.id }.toSet()
        val missingStarters = StarterFoods.all.filter { it.id !in existingIds }
        if (missingStarters.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.starter_foods_title), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { vm.saveFoods(missingStarters.map { it.toFood(arabic) }) },
                        enabled = !state.busy,
                    ) { Text(stringResource(R.string.load_all_starters)) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    missingStarters.take(12).forEach { entry ->
                        val item = entry.toFood(arabic)
                        AssistChip(
                            onClick = { vm.saveFood(item) {} },
                            enabled = !state.busy,
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(item.name) },
                        )
                    }
                }
            }
        }
        Input(R.string.search_meals, search, { search = it }, "food_search", numeric = false)
        state.snapshot.foods.filter { it.name.contains(search, true) }.forEach { food ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(food.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.reference_portion, number(food.referenceAmount), stringResource(unitLabel(food.unit))),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    NutrientSummary(food.nutrients.calories, food.nutrients.protein, food.nutrients.carbs, food.nutrients.fat)
                    Text(stringResource(R.string.source_value, food.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (food.ingredients.isNotEmpty()) {
                        food.ingredients.forEach { ingredient ->
                            Text(
                                stringResource(R.string.ingredient_record, ingredient.name, number(ingredient.quantity), stringResource(unitLabel(ingredient.unit))),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row {
                        if (food.sourceKind != NutritionSource.RECIPE) TextButton(onClick = { editor = food }) { Text(stringResource(R.string.edit)) }
                        ConfirmButton(R.string.remove, stringResource(R.string.delete_food_note), !state.busy) { vm.deleteFood(food.id) }
                    }
                }
            }
        }
    }
    if (create || editor != null) {
        FoodDialog(editor, state.busy, { create = false; editor = null }) {
            vm.saveFood(it) { create = false; editor = null }
        }
    }
    if (recipe) {
        RecipeDialog(state.snapshot.foods, state.busy, { recipe = false }) {
            vm.saveFood(it) { recipe = false }
        }
    }
}

@Composable
private fun FoodDialog(initial: Food?, busy: Boolean, onDismiss: () -> Unit, onSave: (Food) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember { mutableStateOf((initial?.referenceAmount ?: 100.0).toString()) }
    var unit by remember { mutableStateOf(initial?.unit ?: NutritionUnit.GRAMS) }
    var sourceKind by remember { mutableStateOf(initial?.sourceKind ?: NutritionSource.LABEL) }
    var source by remember { mutableStateOf(initial?.source.orEmpty()) }
    var kcal by remember { mutableStateOf(initial?.nutrients?.calories?.toString().orEmpty()) }
    var protein by remember { mutableStateOf(initial?.nutrients?.protein?.toString().orEmpty()) }
    var carbs by remember { mutableStateOf(initial?.nutrients?.carbs?.toString().orEmpty()) }
    var fat by remember { mutableStateOf(initial?.nutrients?.fat?.toString().orEmpty()) }
    val candidate = runCatching {
        Food(
            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            referenceAmount = requireNotNull(NumericInput.decimal(amount)),
            unit = unit,
            nutrients = Nutrients(
                requireNotNull(NumericInput.decimal(kcal)),
                requireNotNull(NumericInput.decimal(protein)),
                requireNotNull(NumericInput.decimal(carbs)),
                requireNotNull(NumericInput.decimal(fat)),
            ),
            source = source.trim(),
            sourceKind = sourceKind,
        )
    }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_food), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Input(R.string.meal_name, name, { name = it }, "food_name", numeric = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionUnit.entries.forEach { value ->
                        FilterChip(selected = unit == value, onClick = { unit = value }, label = { Text(stringResource(unitLabel(value))) })
                    }
                }
                Input(R.string.reference_amount, amount, { amount = it }, "food_reference")
                Input(R.string.calories, kcal, { kcal = it }, "food_calories")
                Input(R.string.protein, protein, { protein = it }, "food_protein")
                Input(R.string.carbs, carbs, { carbs = it }, "food_carbs")
                Input(R.string.fat, fat, { fat = it }, "food_fat")
                Input(R.string.source_required, source, { source = it }, "food_source", numeric = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(NutritionSource.LABEL, NutritionSource.MANUAL).forEach { value ->
                        FilterChip(
                            selected = sourceKind == value,
                            onClick = { sourceKind = value },
                            label = { Text(stringResource(if (value == NutritionSource.LABEL) R.string.label_source else R.string.manual_source)) },
                        )
                    }
                }
                Text(stringResource(R.string.user_nutrition_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { candidate?.let(onSave) }, enabled = candidate != null && !busy, modifier = Modifier.testTag("save_food")) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RecipeDialog(foods: List<Food>, busy: Boolean, onDismiss: () -> Unit, onSave: (Food) -> Unit) {
    var name by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(foods.first().id) }
    var quantity by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<IngredientSnapshot>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val food = foods.firstOrNull { it.id == selected } ?: foods.first()
    val ingredient = runCatching { food.ingredient(requireNotNull(NumericInput.decimal(quantity))) }.getOrNull()
    val recipe = runCatching { Food.recipe(name.trim(), requireNotNull(NumericInput.decimal(servings)), source.trim(), ingredients) }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_recipe), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Input(R.string.meal_name, name, { name = it }, "recipe_name", numeric = false)
                Input(R.string.recipe_yield, servings, { servings = it }, "recipe_yield")
                Input(R.string.source_required, source, { source = it }, "recipe_source", numeric = false)
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(food.name) }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        foods.forEach { item ->
                            DropdownMenuItem(text = { Text(item.name) }, onClick = { selected = item.id; expanded = false })
                        }
                    }
                }
                Input(
                    if (food.unit == NutritionUnit.GRAMS) R.string.quantity_grams else R.string.quantity_servings,
                    quantity,
                    { quantity = it },
                    "ingredient_quantity",
                )
                OutlinedButton(
                    onClick = { ingredient?.let { ingredients = ingredients + it; quantity = "" } },
                    enabled = ingredient != null && ingredients.size < 30,
                ) { Text(stringResource(R.string.add_ingredient)) }
                ingredients.forEachIndexed { index, item ->
                    Text(stringResource(R.string.ingredient_record, item.name, number(item.quantity), stringResource(unitLabel(item.unit))))
                    TextButton(onClick = { ingredients = ingredients.filterIndexed { i, _ -> i != index } }) { Text(stringResource(R.string.remove)) }
                }
                Text(stringResource(R.string.recipe_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { recipe?.let(onSave) }, enabled = recipe != null && !busy, modifier = Modifier.testTag("save_recipe")) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun NutrientSummary(kcal: Double, protein: Double, carbs: Double, fat: Double) {
    Text(
        stringResource(R.string.meal_summary, number(kcal, 0), number(protein), number(carbs), number(fat)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun unitLabel(unit: NutritionUnit) = if (unit == NutritionUnit.GRAMS) R.string.grams else R.string.servings
