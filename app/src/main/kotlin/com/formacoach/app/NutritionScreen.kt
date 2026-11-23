package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formacoach.coach.ExerciseSearch
import com.formacoach.coach.FamilyPotCalculator
import com.formacoach.coach.FoodSwapCalculator
import com.formacoach.coach.SwapMatchBy
import com.formacoach.model.*
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun EatContent(state: AppState, today: LocalDate, vm: FitnessViewModel, onMealLogged: (Meal) -> Unit = {}) {
    var day by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val date = LocalDate.ofEpochDay(day)
    var library by rememberSaveable { mutableStateOf(false) }
    var showOnlineSearch by rememberSaveable { mutableStateOf(false) }
    var showFoodSwap by rememberSaveable { mutableStateOf(false) }
    var showFamilyPot by rememberSaveable { mutableStateOf(false) }
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

    var showCopyDay by rememberSaveable { mutableStateOf(false) }
    if (showCopyDay) CopyMealDay(state, date, today, vm) { showCopyDay = false }
    PageHeading(R.string.eat_title)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateChoice(date, today, { day = it.toEpochDay() }, "food_date")
        OutlinedButton(
            onClick = { showOnlineSearch = true },
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.testTag("online_food_search"),
        ) {
            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.online_food_search_btn), fontWeight = FontWeight.SemiBold)
        }
        FilledTonalButton(
            onClick = { library = !library },
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.testTag("food_library"),
        ) {
            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (library) R.string.close_library else R.string.food_library), fontWeight = FontWeight.SemiBold)
        }
        // Less frequent actions live in one overflow menu so the toolbar stays scannable.
        var moreActions by remember { mutableStateOf(false) }
        Box {
            OutlinedIconButton(
                onClick = { moreActions = true },
                enabled = !state.busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(48.dp).testTag("food_more_actions"),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_food_actions))
            }
            DropdownMenu(expanded = moreActions, onDismissRequest = { moreActions = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_meal_day)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = { moreActions = false; showCopyDay = true },
                    modifier = Modifier.testTag("copy_meal_day"),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.family_pot_btn)) },
                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    onClick = { moreActions = false; showFamilyPot = true },
                    modifier = Modifier.testTag("family_pot_btn"),
                )
            }
        }
    }

    // Compact Macro Summary Dashboard (single glanceable card)
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.food_for_date, dateLabel(date)), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.eat_title, R.string.logged_only)
        }
        val calTarget = state.snapshot.profile?.caloriesTarget
        val proTarget = state.snapshot.profile?.proteinTarget
        var showRemaining by rememberSaveable { mutableStateOf(false) }
        val macroKcalTotal = (totalProtein * 4.0 + totalCarbs * 4.0 + totalFat * 9.0).coerceAtLeast(1.0)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = if (calTarget != null) {
                    Modifier.clip(RoundedCornerShape(16.dp)).clickable { showRemaining = !showRemaining }.testTag("calorie_ring_toggle")
                } else Modifier,
            ) {
                val delta = calTarget?.let { it - totalKcal }
                val ringValue = when {
                    calTarget != null && showRemaining && delta != null -> number(kotlin.math.abs(delta), 0)
                    else -> number(totalKcal, 0)
                }
                val kcalUnit = stringResource(R.string.kcal_unit)
                val ringSub = when {
                    calTarget != null && showRemaining && delta != null -> if (delta >= 0) stringResource(R.string.kcal_remaining, "") else stringResource(R.string.kcal_over, "")
                    calTarget != null -> "/ ${number(calTarget.toDouble(), 0)}"
                    else -> kcalUnit
                }.trim()
                CircularProgressRing(
                    progress = if (calTarget != null) (totalKcal / calTarget).toFloat() else if (totalKcal > 0) 1f else 0f,
                    valueText = ringValue,
                    labelText = ringSub,
                    size = 104.dp,
                    strokeWidth = 10.dp,
                    color = if (calTarget != null && totalKcal > calTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(if (calTarget != null) R.string.tap_to_toggle_remaining else R.string.calories_logged),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroProgressRow(
                    label = stringResource(R.string.protein),
                    valueText = if (proTarget != null) "${number(totalProtein, 0)} / ${number(proTarget, 0)}g" else "${number(totalProtein, 0)}g",
                    progress = if (proTarget != null && proTarget > 0) (totalProtein / proTarget).toFloat() else ((totalProtein * 4.0) / macroKcalTotal).toFloat(),
                    color = MaterialTheme.colorScheme.secondary,
                )
                MacroProgressRow(
                    label = stringResource(R.string.carbs),
                    valueText = "${number(totalCarbs, 0)}g",
                    progress = if (totalKcal > 0) ((totalCarbs * 4.0) / macroKcalTotal).toFloat() else 0f,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                MacroProgressRow(
                    label = stringResource(R.string.fat),
                    valueText = "${number(totalFat, 0)}g",
                    progress = if (totalKcal > 0) ((totalFat * 9.0) / macroKcalTotal).toFloat() else 0f,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
                    onClick = {
                        // One tap logs immediately, so confirm it and offer Undo instead of failing silently.
                        val repeated = recent.copy(id = java.util.UUID.randomUUID().toString(), date = date, slot = slot)
                        vm.addMeal(repeated) { onMealLogged(repeated) }
                    },
                    enabled = !state.busy,
                    modifier = Modifier.testTag("quick_repeat_${recent.name}"),
                    leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("${recent.name} · ${number(recent.calories, 0)}", maxLines = 1) },
                )
            }
        }
    }

    if (state.snapshot.foods.isNotEmpty()) {
        var showFoodPicker by rememberSaveable { mutableStateOf(false) }
        InfoCard {
            Text(stringResource(R.string.log_saved_food), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { showFoodPicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("choose_food"),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(food?.name ?: stringResource(R.string.choose_food), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
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
                // Quick portion chips
                val presets = if (item.unit == NutritionUnit.GRAMS) listOf("50", "100", "150", "200") else listOf("0.5", "1", "1.5", "2")
                val suffix = if (item.unit == NutritionUnit.GRAMS) "g" else "×"
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { quantity = preset },
                            label = { Text("$preset$suffix", fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }
                Text(stringResource(R.string.source_value, item.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                portion?.let { NutrientSummary(it.calories, it.protein, it.carbs, it.fat) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (vm.state.value.busy) return@Button; portion?.let { vm.addMeal(it) { quantity = "" } } },
                        enabled = portion != null && !state.busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("log_portion"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.save_meal), fontWeight = FontWeight.Bold)
                    }
                    if (state.snapshot.foods.size >= 2) {
                        OutlinedButton(
                            onClick = { showFoodSwap = true },
                            enabled = portion != null && !state.busy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 50.dp).testTag("find_food_swap"),
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.find_food_swap), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (showFoodPicker) {
            var foodQuery by remember { mutableStateOf("") }
            val filteredFoods = remember(state.snapshot.foods, foodQuery) {
                if (foodQuery.isBlank()) state.snapshot.foods
                else state.snapshot.foods.filter { ExerciseSearch.matches(foodQuery, it.name + " " + it.source) }
            }
            ModalBottomSheet(
                onDismissRequest = { showFoodPicker = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.choose_food), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Input(R.string.search_saved_foods, foodQuery, { foodQuery = it }, "food_picker_search", numeric = false)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (filteredFoods.isEmpty()) {
                            Text(stringResource(R.string.no_matching_foods), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            filteredFoods.forEach { item ->
                                val selected = item.id == foodId
                                Surface(
                                    onClick = { foodId = item.id; showFoodPicker = false },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                "${number(item.referenceAmount)} ${stringResource(unitLabel(item.unit))} · ${item.source}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        PillBadge(stringResource(R.string.kcal_value, number(item.nutrients.calories, 0)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    InfoCard { MealFields(null, date, today, state.busy, defaultSlot = slot, onSave = { meal, done -> vm.addMeal(meal, done) }) }

    Text(stringResource(R.string.food_for_date, dateLabel(date)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Input(R.string.search_meals, search, { search = it }, "meal_search", numeric = false)
    val meals = state.snapshot.meals.filter { it.date == date && (search.isBlank() || it.name.contains(search, true)) }
    var comboGroup by remember { mutableStateOf<Pair<MealSlot, List<Meal>>?>(null) }
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
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                        modifier = Modifier.weight(1f),
                    )
                    if (values.size in 2..30) {
                        TextButton(
                            onClick = { comboGroup = group to values },
                            enabled = !state.busy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("save_combo_${group.name}"),
                        ) {
                            Text(stringResource(R.string.save_as_combo), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
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
                        PillBadge(stringResource(R.string.kcal_value, number(meal.calories, 0)))
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
                    PillBadge(stringResource(R.string.kcal_value, number(meal.calories, 0)))
                }
                NutrientSummary(meal.calories, meal.protein, meal.carbs, meal.fat)
                OutlinedButton(
                    onClick = {
                        val repeated = meal.copy(id = java.util.UUID.randomUUID().toString(), date = date, slot = slot)
                        vm.addMeal(repeated) { onMealLogged(repeated) }
                    },
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
    comboGroup?.let { (slotGroup, items) ->
        val defaultName = stringResource(mealSlotLabel(slotGroup))
        val manualFallback = stringResource(R.string.manual_source)
        var comboName by remember(slotGroup, items) { mutableStateOf(defaultName) }
        val comboCandidate = remember(comboName, items) {
            runCatching {
                val ingredients = items.map { m ->
                    IngredientSnapshot(
                        name = m.name,
                        quantity = m.quantity ?: 1.0,
                        unit = m.unit ?: NutritionUnit.SERVING,
                        nutrients = Nutrients(m.calories, m.protein, m.carbs, m.fat),
                        source = m.source.ifBlank { manualFallback },
                    )
                }
                Food.recipe(
                    name = comboName.trim(),
                    servings = 1.0,
                    source = manualFallback,
                    ingredients = ingredients,
                )
            }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { comboGroup = null },
            title = { Text(stringResource(R.string.save_combo_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.save_combo_note, number(items.size.toDouble(), 0)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Input(R.string.save_combo_name, comboName, { comboName = it }, "combo_name_input", numeric = false)
                    NutrientSummary(
                        items.sumOf { it.calories },
                        items.sumOf { it.protein },
                        items.sumOf { it.carbs },
                        items.sumOf { it.fat },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        comboCandidate?.let { saved ->
                            vm.saveFood(saved) { comboGroup = null }
                        }
                    },
                    enabled = comboCandidate != null && !state.busy,
                    modifier = Modifier.testTag("confirm_save_combo"),
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { comboGroup = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showOnlineSearch) {
        OnlineFoodSearchSheet(
            busy = state.busy,
            onImport = { imported ->
                vm.saveFood(imported) {
                    foodId = imported.id
                    quantity = "100"
                    showOnlineSearch = false
                }
            },
            onDismiss = { showOnlineSearch = false },
        )
    }
    if (showFoodSwap && food != null) {
        val qtyVal = NumericInput.decimal(quantity) ?: food.referenceAmount
        FoodSwapSheet(
            target = food,
            targetQuantity = qtyVal,
            library = state.snapshot.foods,
            onSelectSwap = { swap ->
                foodId = swap.food.id
                quantity = formatInputNumber(swap.matchedQuantity, java.util.Locale.US, 1)
                showFoodSwap = false
            },
            onDismiss = { showFoodSwap = false },
        )
    }
    if (showFamilyPot) {
        FamilyPotSheet(
            date = date,
            slot = slot,
            recipes = state.snapshot.foods.filter { it.sourceKind == NutritionSource.RECIPE },
            busy = state.busy,
            onSave = { meal -> vm.addMeal(meal) { showFamilyPot = false } },
            onDismiss = { showFamilyPot = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyPotSheet(
    date: LocalDate,
    slot: MealSlot,
    recipes: List<Food>,
    busy: Boolean,
    onSave: (Meal) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("forma_ui_prefs", android.content.Context.MODE_PRIVATE) }
    val savedLeftoverName = remember { prefs.getString("last_pot_name", "").orEmpty() }
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = weight, 1 = shares, 2 = quick ballpark plate
    var dishName by rememberSaveable { mutableStateOf("") }
    var potKcal by rememberSaveable { mutableStateOf("") }
    var potProtein by rememberSaveable { mutableStateOf("") }
    var potCarbs by rememberSaveable { mutableStateOf("") }
    var potFat by rememberSaveable { mutableStateOf("") }
    var totalGrams by rememberSaveable { mutableStateOf("1200") }
    var myGrams by rememberSaveable { mutableStateOf("300") }
    var totalShares by rememberSaveable { mutableStateOf("4") }
    var myShares by rememberSaveable { mutableStateOf("1") }
    var plateSize by rememberSaveable { mutableIntStateOf(1) } // 0 = light, 1 = medium, 2 = full
    var addOilBuffer by rememberSaveable { mutableStateOf(false) }

    val potNutrients = runCatching {
        Nutrients(
            calories = requireNotNull(NumericInput.decimal(potKcal)),
            protein = NumericInput.decimal(potProtein) ?: 0.0,
            carbs = NumericInput.decimal(potCarbs) ?: 0.0,
            fat = NumericInput.decimal(potFat) ?: 0.0,
        )
    }.getOrNull()

    val share = if (mode != 2) {
        potNutrients?.let { n ->
            if (mode == 0) {
                val tg = NumericInput.decimal(totalGrams) ?: 0.0
                val mg = NumericInput.decimal(myGrams) ?: 0.0
                FamilyPotCalculator.byCookedWeight(n, tg, mg)
            } else {
                val ts = NumericInput.decimal(totalShares) ?: 0.0
                val ms = NumericInput.decimal(myShares) ?: 0.0
                FamilyPotCalculator.byFamilyShares(n, ts, ms)
            }
        }
    } else null

    val sourceLabel = stringResource(if (mode == 2) R.string.family_plate_source else R.string.family_pot_source)
    val defaultPlateName = stringResource(R.string.family_plate_default_name)
    val ballparkNutrients = remember(plateSize, addOilBuffer) {
        val base = when (plateSize) {
            0 -> Nutrients(450.0, 25.0, 50.0, 15.0)
            2 -> Nutrients(950.0, 48.0, 105.0, 34.0)
            else -> Nutrients(700.0, 35.0, 78.0, 24.0)
        }
        if (addOilBuffer) base + Nutrients(120.0, 0.0, 0.0, 13.5) else base
    }
    val mealCandidate = if (mode == 2) {
        runCatching {
            Meal(
                date = date,
                name = dishName.trim().ifBlank { defaultPlateName },
                calories = ballparkNutrients.calories,
                protein = ballparkNutrients.protein,
                carbs = ballparkNutrients.carbs,
                fat = ballparkNutrients.fat,
                quantity = 1.0,
                unit = NutritionUnit.SERVING,
                source = sourceLabel,
                slot = slot,
            )
        }.getOrNull()
    } else if (dishName.isNotBlank() && share != null) {
        runCatching {
            Meal(
                date = date,
                name = dishName.trim(),
                calories = share.portionNutrients.calories,
                protein = share.portionNutrients.protein,
                carbs = share.portionNutrients.carbs,
                fat = share.portionNutrients.fat,
                quantity = if (mode == 0) NumericInput.decimal(myGrams) else NumericInput.decimal(myShares),
                unit = if (mode == 0) NutritionUnit.GRAMS else NutritionUnit.SERVING,
                source = sourceLabel,
                slot = slot,
            )
        }.getOrNull()
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.family_pot_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.family_pot_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 1-Tap Leftovers + Saved Recipes
            if (savedLeftoverName.isNotBlank() || recipes.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (savedLeftoverName.isNotBlank()) {
                        AssistChip(
                            onClick = {
                                mode = prefs.getInt("last_pot_mode", 0).coerceIn(0, 1)
                                dishName = savedLeftoverName
                                potKcal = prefs.getString("last_pot_kcal", "").orEmpty()
                                potProtein = prefs.getString("last_pot_pro", "").orEmpty()
                                potCarbs = prefs.getString("last_pot_carbs", "").orEmpty()
                                potFat = prefs.getString("last_pot_fat", "").orEmpty()
                                totalGrams = prefs.getString("last_pot_tg", "1200").orEmpty()
                                totalShares = prefs.getString("last_pot_ts", "4").orEmpty()
                            },
                            leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = { Text(stringResource(R.string.family_pot_leftovers_chip, savedLeftoverName), maxLines = 1) },
                        )
                    }
                    recipes.take(6).forEach { r ->
                        AssistChip(
                            onClick = {
                                if (mode == 2) mode = 0
                                dishName = r.name
                                potKcal = r.nutrients.calories.toInt().toString()
                                potProtein = r.nutrients.protein.toInt().toString()
                                potCarbs = r.nutrients.carbs.toInt().toString()
                                potFat = r.nutrients.fat.toInt().toString()
                            },
                            label = { Text(r.name, maxLines = 1) },
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    label = { Text(stringResource(R.string.family_pot_by_weight), fontWeight = FontWeight.SemiBold) },
                )
                FilterChip(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    label = { Text(stringResource(R.string.family_pot_by_shares), fontWeight = FontWeight.SemiBold) },
                )
                FilterChip(
                    selected = mode == 2,
                    onClick = { mode = 2 },
                    label = { Text(stringResource(R.string.family_pot_ballpark), fontWeight = FontWeight.SemiBold) },
                )
            }

            Input(R.string.family_pot_name, dishName, { dishName = it }, "family_pot_name", numeric = false)

            if (mode == 2) {
                Text(stringResource(R.string.family_pot_ballpark_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.plate_light, R.string.plate_medium, R.string.plate_full).forEachIndexed { idx, labelRes ->
                        FilterChip(
                            selected = plateSize == idx,
                            onClick = { plateSize = idx },
                            label = { Text(stringResource(labelRes), fontWeight = FontWeight.SemiBold) },
                        )
                    }
                }
                FilterChip(
                    selected = addOilBuffer,
                    onClick = { addOilBuffer = !addOilBuffer },
                    label = { Text(stringResource(R.string.plate_oil_buffer)) },
                )
                Surface(
                    color = MaterialTheme.colorScheme.accentContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(sourceLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        NutrientSummary(ballparkNutrients.calories, ballparkNutrients.protein, ballparkNutrients.carbs, ballparkNutrients.fat)
                    }
                }
            } else {
                FormFieldPair(
                    first = { Input(R.string.family_pot_total_kcal, potKcal, { potKcal = it }, "family_pot_kcal") },
                    second = { Input(R.string.family_pot_total_protein, potProtein, { potProtein = it }, "family_pot_protein") },
                )
                FormFieldPair(
                    first = { Input(R.string.family_pot_total_carbs, potCarbs, { potCarbs = it }, "family_pot_carbs") },
                    second = { Input(R.string.family_pot_total_fat, potFat, { potFat = it }, "family_pot_fat") },
                )
                if (mode == 0) {
                    FormFieldPair(
                        first = { Input(R.string.family_pot_total_grams, totalGrams, { totalGrams = it }, "family_pot_total_g") },
                        second = { Input(R.string.family_pot_my_grams, myGrams, { myGrams = it }, "family_pot_my_g") },
                    )
                } else {
                    FormFieldPair(
                        first = { Input(R.string.family_pot_total_shares, totalShares, { totalShares = it }, "family_pot_total_s") },
                        second = { Input(R.string.family_pot_my_shares, myShares, { myShares = it }, "family_pot_my_s") },
                    )
                }

                share?.let { s ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.family_pot_share_summary, number(s.percent.toDouble(), 0)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            LinearProgressIndicator(
                                progress = { s.fraction.toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                                drawStopIndicator = {},
                            )
                            NutrientSummary(
                                s.portionNutrients.calories,
                                s.portionNutrients.protein,
                                s.portionNutrients.carbs,
                                s.portionNutrients.fat,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    mealCandidate?.let { saved ->
                        if (mode != 2) {
                            prefs.edit()
                                .putInt("last_pot_mode", mode)
                                .putString("last_pot_name", dishName.trim())
                                .putString("last_pot_kcal", potKcal)
                                .putString("last_pot_pro", potProtein)
                                .putString("last_pot_carbs", potCarbs)
                                .putString("last_pot_fat", potFat)
                                .putString("last_pot_tg", totalGrams)
                                .putString("last_pot_ts", totalShares)
                                .apply()
                        }
                        onSave(saved)
                    }
                },
                enabled = mealCandidate != null && !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_family_pot"),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.save_meal), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineFoodSearchSheet(
    busy: Boolean,
    onImport: (Food) -> Unit,
    onDismiss: () -> Unit,
) {
    val preferArabic = LocalConfiguration.current.locales[0].language == "ar"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var scanFailed by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineFoodResult>>(emptyList()) }

    fun runSearch(explicitQuery: String = query) {
        if (explicitQuery.isBlank() || searching) return
        searching = true
        failed = false
        scanFailed = false
        scope.launch {
            val res = OpenFoodFactsClient.search(explicitQuery, preferArabic)
            searching = false
            searched = true
            res.onSuccess { results = it }.onFailure { failed = true; results = emptyList() }
        }
    }

    fun scanBarcode() {
        scanFailed = false
        runCatching {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                )
                .enableAutoZoom()
                .build()
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue?.trim().orEmpty()
                    if (raw.isNotBlank()) {
                        query = raw
                        runSearch(raw)
                    }
                }
                .addOnFailureListener { scanFailed = true }
        }.onFailure { scanFailed = true }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.online_food_search), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = ::scanBarcode,
                    enabled = !searching,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("scan_barcode_btn"),
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.scan_barcode), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(stringResource(R.string.online_food_attribution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Input(R.string.online_food_hint, query, { query = it }, "online_food_query", numeric = false)
                }
                Button(
                    onClick = { runSearch() },
                    enabled = query.isNotBlank() && !searching,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.heightIn(min = 52.dp).testTag("run_online_food_search"),
                ) {
                    Text(stringResource(R.string.online_food_search_btn), fontWeight = FontWeight.Bold)
                }
            }
            if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (scanFailed) Text(stringResource(R.string.barcode_scan_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (failed) Text(stringResource(R.string.online_food_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (searched && !failed && !searching && results.isEmpty()) {
                Text(stringResource(R.string.online_food_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (item.brand.isNotBlank()) {
                                        Text(item.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                PillBadge(stringResource(R.string.kcal_per_100g, number(item.nutrientsPer100g.calories, 0)))
                            }
                            NutrientSummary(
                                item.nutrientsPer100g.calories,
                                item.nutrientsPer100g.protein,
                                item.nutrientsPer100g.carbs,
                                item.nutrientsPer100g.fat,
                            )
                            FilledTonalButton(
                                onClick = { onImport(item.toFood()) },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.import_to_library), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodSwapSheet(
    target: Food,
    targetQuantity: Double,
    library: List<Food>,
    onSelectSwap: (com.formacoach.coach.FoodSwapOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var matchBy by rememberSaveable { mutableStateOf(SwapMatchBy.PROTEIN) }
    val swaps = remember(target, targetQuantity, library, matchBy) {
        FoodSwapCalculator.findSwaps(target, targetQuantity, library, matchBy)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.food_swap_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = matchBy == SwapMatchBy.PROTEIN,
                    onClick = { matchBy = SwapMatchBy.PROTEIN },
                    label = { Text(stringResource(R.string.swap_match_protein), fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = matchBy == SwapMatchBy.CALORIES,
                    onClick = { matchBy = SwapMatchBy.CALORIES },
                    label = { Text(stringResource(R.string.swap_match_calories), fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (swaps.isEmpty()) {
                Text(stringResource(R.string.no_food_swaps), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    swaps.forEach { option ->
                        Surface(
                            onClick = { onSelectSwap(option) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(option.food.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    PillBadge("${number(option.matchedQuantity, 0)} ${stringResource(unitLabel(option.food.unit))}")
                                }
                                NutrientSummary(
                                    option.matchedNutrients.calories,
                                    option.matchedNutrients.protein,
                                    option.matchedNutrients.carbs,
                                    option.matchedNutrients.fat,
                                )
                            }
                        }
                    }
                }
            }
        }
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
    val nameTooLong = name.trim().length > 120
    var nameTouched by rememberSaveable(initial?.id) { mutableStateOf(initial != null) }
    val nameInvalid = nameTooLong || (nameTouched && name.isBlank())
    val calorieValue = NumericInput.decimal(calories)
    val proteinValue = NumericInput.decimal(protein)
    val carbsValue = NumericInput.decimal(carbs)
    val fatValue = NumericInput.decimal(fat)
    val caloriesInvalid = calories.isNotBlank() && (calorieValue == null || calorieValue !in 0.0..20000.0)
    val proteinInvalid = protein.isNotBlank() && (proteinValue == null || proteinValue !in 0.0..2000.0)
    val carbsInvalid = carbs.isNotBlank() && (carbsValue == null || carbsValue !in 0.0..2000.0)
    val fatInvalid = fat.isNotBlank() && (fatValue == null || fatValue !in 0.0..2000.0)
    val meal = runCatching {
        Meal(
            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
            date = if (initial != null) LocalDate.ofEpochDay(editedDay) else date,
            name = name.trim(),
            calories = requireNotNull(calorieValue),
            protein = requireNotNull(proteinValue),
            carbs = requireNotNull(carbsValue),
            fat = requireNotNull(fatValue),
            source = source.trim(),
            slot = if (initial == null) defaultSlot else editedSlot,
        )
    }.getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.manual_meal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (initial != null) DateChoice(LocalDate.ofEpochDay(editedDay), today, { editedDay = it.toEpochDay() }, "edit_meal_date")
        if (initial != null) MealSlotPicker(editedSlot) { editedSlot = it }
        Text(stringResource(R.string.portion_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Input(
            R.string.meal_name,
            name,
            { name = it; nameTouched = true },
            "meal_name",
            numeric = false,
            error = nameInvalid,
            supportingText = if (nameInvalid) stringResource(R.string.meal_name_error) else null,
        )
        FormFieldPair(
            first = {
                Input(
                    R.string.calories,
                    calories,
                    { calories = it },
                    "meal_calories",
                    error = caloriesInvalid,
                    supportingText = if (caloriesInvalid) stringResource(R.string.calories_error) else null,
                )
            },
            second = {
                Input(
                    R.string.protein,
                    protein,
                    { protein = it },
                    "meal_protein",
                    error = proteinInvalid,
                    supportingText = if (proteinInvalid) stringResource(R.string.macro_error) else null,
                )
            },
        )
        FormFieldPair(
            first = {
                Input(
                    R.string.carbs,
                    carbs,
                    { carbs = it },
                    "meal_carbs",
                    error = carbsInvalid,
                    supportingText = if (carbsInvalid) stringResource(R.string.macro_error) else null,
                )
            },
            second = {
                Input(
                    R.string.fat,
                    fat,
                    { fat = it },
                    "meal_fat",
                    error = fatInvalid,
                    supportingText = if (fatInvalid) stringResource(R.string.macro_error) else null,
                )
            },
        )
        if (proteinValue != null && carbsValue != null && fatValue != null) {
            val computed = kotlin.math.round(com.formacoach.coach.TrainingHistory.macroCalories(proteinValue, carbsValue, fatValue)).toInt()
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
                            nameTouched = false
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
private fun MacroProgressRow(label: String, valueText: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
            drawStopIndicator = {},
        )
    }
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
