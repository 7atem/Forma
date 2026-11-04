package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.model.*
import java.time.LocalDate

@Composable
internal fun EatContent(state:AppState,today:LocalDate,vm:FitnessViewModel) {
    var day by rememberSaveable {mutableLongStateOf(today.toEpochDay())}
    val date=LocalDate.ofEpochDay(day)
    var library by rememberSaveable {mutableStateOf(false)}
    var search by rememberSaveable {mutableStateOf("")}
    var editing by remember {mutableStateOf<Meal?>(null)}
    var foodId by rememberSaveable {mutableStateOf<String?>(null)}
    var quantity by rememberSaveable {mutableStateOf("")}
    val food=state.snapshot.foods.firstOrNull {it.id==foodId}
    val portion=runCatching {food?.portion(requireNotNull(NumericInput.decimal(quantity)),date)}.getOrNull()
    PageHeading(R.string.eat_title)
    DateChoice(date,today,{day=it.toEpochDay()},"food_date")
    val logged=state.snapshot.meals.filter {it.date==date}
    CopyMealDay(state,date,today,vm)
    InfoCard {
        Text(stringResource(R.string.food_for_date,dateLabel(date)),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            MetricTile(number(logged.sumOf {it.calories},0),stringResource(R.string.calories_logged),Modifier.weight(1f))
            MetricTile(number(logged.sumOf {it.protein}),stringResource(R.string.protein_logged),Modifier.weight(1f))
        }
        Text(stringResource(R.string.logged_only),style=MaterialTheme.typography.bodySmall)
    }
    OutlinedButton(onClick={library=!library},modifier=Modifier.fillMaxWidth().testTag("food_library")) {Text(stringResource(if(library) R.string.close_library else R.string.food_library))}
    if(library) FoodLibrary(state,vm)
    if(state.snapshot.foods.isNotEmpty()) InfoCard {
        Text(stringResource(R.string.log_saved_food),style=MaterialTheme.typography.titleLarge)
        var expanded by remember {mutableStateOf(false)}
        Box {
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("choose_food")) {Text(food?.name?:stringResource(R.string.choose_food))}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {state.snapshot.foods.forEach {item ->
                DropdownMenuItem(text={Text(item.name)},onClick={foodId=item.id;expanded=false})
            }}
        }
        food?.let {item ->
            Text(stringResource(R.string.reference_portion,number(item.referenceAmount),stringResource(unitLabel(item.unit))))
            Input(if(item.unit==NutritionUnit.GRAMS) R.string.quantity_grams else R.string.quantity_servings,quantity,{quantity=it},"portion_quantity")
            Text(stringResource(R.string.source_value,item.source),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.user_nutrition_note),style=MaterialTheme.typography.bodySmall)
            portion?.let {NutrientSummary(it.calories,it.protein,it.carbs,it.fat)}
            Button(onClick={portion?.let {vm.addMeal(it){quantity=""}}},enabled=portion!=null&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("log_portion")) {Text(stringResource(R.string.save_meal))}
        }
    }
    InfoCard {MealFields(null,date,state.busy,onSave={meal,done ->vm.addMeal(meal,done)})}
    Text(stringResource(R.string.food_for_date,dateLabel(date)),style=MaterialTheme.typography.titleLarge)
    Input(R.string.search_meals,search,{search=it},"meal_search",numeric=false)
    val meals=state.snapshot.meals.filter {it.date==date && (search.isBlank()||it.name.contains(search,true))}
    if(meals.isEmpty()) Text(stringResource(R.string.no_meals))
    meals.forEach {meal -> key(meal.id) {InfoCard {
        Text(meal.name,style=MaterialTheme.typography.titleMedium)
        NutrientSummary(meal.calories,meal.protein,meal.carbs,meal.fat)
        meal.quantity?.let {Text(stringResource(R.string.portion_record,number(it),stringResource(unitLabel(checkNotNull(meal.unit)))))}
        Text(if(meal.source.isBlank()) stringResource(R.string.manual_source) else stringResource(R.string.source_value,meal.source),style=MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick={editing=meal},enabled=!state.busy) {Text(stringResource(R.string.edit))}
            ConfirmButton(R.string.remove,stringResource(R.string.delete_meal_note),!state.busy) {vm.removeMeal(meal.id)}
        }
    }}}
    Text(stringResource(R.string.reuse_meal),style=MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.reuse_selected_note),style=MaterialTheme.typography.bodySmall)
    state.snapshot.meals.distinctBy {listOf(it.name,it.calories,it.protein,it.carbs,it.fat,it.source)}.take(5).forEach {meal -> InfoCard {
        Text(meal.name);NutrientSummary(meal.calories,meal.protein,meal.carbs,meal.fat)
        OutlinedButton(onClick={vm.addMeal(meal.copy(id=java.util.UUID.randomUUID().toString(),date=date)){}},enabled=!state.busy) {Text(stringResource(R.string.repeat_for_date,dateLabel(date)))}
    }}
    editing?.let {meal -> AlertDialog(onDismissRequest={editing=null},title={Text(stringResource(R.string.edit_meal))},
        text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState())) {
            MealFields(meal,date,state.busy,onSave={value,done ->vm.addMeal(value){done();editing=null}})
        }},confirmButton={},dismissButton={TextButton(onClick={editing=null}) {Text(stringResource(R.string.cancel))}})}
}

@Composable
private fun MealFields(initial:Meal?,date:LocalDate,busy:Boolean,onSave:(Meal,()->Unit)->Unit) {
    var name by rememberSaveable(initial?.id) {mutableStateOf(initial?.name.orEmpty())}
    var calories by rememberSaveable(initial?.id) {mutableStateOf(initial?.calories?.toString().orEmpty())}
    var protein by rememberSaveable(initial?.id) {mutableStateOf(initial?.protein?.toString().orEmpty())}
    var carbs by rememberSaveable(initial?.id) {mutableStateOf(initial?.carbs?.toString().orEmpty())}
    var fat by rememberSaveable(initial?.id) {mutableStateOf(initial?.fat?.toString().orEmpty())}
    var source by rememberSaveable(initial?.id) {mutableStateOf(initial?.source.orEmpty())}
    var editedDay by rememberSaveable(initial?.id) {mutableLongStateOf((initial?.date?:date).toEpochDay())}
    val meal=runCatching {Meal(id=initial?.id?:java.util.UUID.randomUUID().toString(),date=if(initial!=null) LocalDate.ofEpochDay(editedDay) else date,name=name.trim(),calories=requireNotNull(NumericInput.decimal(calories)),protein=requireNotNull(NumericInput.decimal(protein)),carbs=requireNotNull(NumericInput.decimal(carbs)),fat=requireNotNull(NumericInput.decimal(fat)),source=source.trim())}.getOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.manual_meal),style=MaterialTheme.typography.titleMedium)
        if(initial!=null) DateChoice(LocalDate.ofEpochDay(editedDay),LocalDate.now(),{editedDay=it.toEpochDay()},"edit_meal_date")
        Text(stringResource(R.string.portion_note))
        Input(R.string.meal_name,name,{name=it},"meal_name",numeric=false)
        FormFieldPair(first={Input(R.string.calories,calories,{calories=it},"meal_calories")},second={Input(R.string.protein,protein,{protein=it},"meal_protein")})
        FormFieldPair(first={Input(R.string.carbs,carbs,{carbs=it},"meal_carbs")},second={Input(R.string.fat,fat,{fat=it},"meal_fat")})
        Input(R.string.source_optional,source,{source=it},"meal_source",numeric=false)
        if(initial!=null) Text(stringResource(R.string.manual_correction_note),style=MaterialTheme.typography.bodySmall)
        Button(onClick={meal?.let {onSave(it){if(initial==null){name="";calories="";protein="";carbs="";fat="";source=""}}}},enabled=meal!=null&&!busy,modifier=Modifier.fillMaxWidth().testTag("save_meal")) {Text(stringResource(R.string.save_meal))}
    }
}

@Composable
private fun FoodLibrary(state:AppState,vm:FitnessViewModel) {
    var editor by remember {mutableStateOf<Food?>(null)}
    var create by rememberSaveable {mutableStateOf(false)}
    var recipe by rememberSaveable {mutableStateOf(false)}
    var search by rememberSaveable {mutableStateOf("")}
    InfoCard {
        Text(stringResource(R.string.food_library),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.library_note))
        Button(onClick={create=true},modifier=Modifier.testTag("new_food")) {Text(stringResource(R.string.new_food))}
        OutlinedButton(onClick={recipe=true},enabled=state.snapshot.foods.isNotEmpty()) {Text(stringResource(R.string.new_recipe))}
        Input(R.string.search_meals,search,{search=it},"food_search",numeric=false)
        state.snapshot.foods.filter {it.name.contains(search,true)}.forEach {food ->
            Text(food.name,style=MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.reference_portion,number(food.referenceAmount),stringResource(unitLabel(food.unit))))
            NutrientSummary(food.nutrients.calories,food.nutrients.protein,food.nutrients.carbs,food.nutrients.fat)
            Text(stringResource(R.string.source_value,food.source),style=MaterialTheme.typography.bodySmall)
            if(food.ingredients.isNotEmpty()) food.ingredients.forEach {ingredient ->
                Text(stringResource(R.string.ingredient_record,ingredient.name,number(ingredient.quantity),stringResource(unitLabel(ingredient.unit))),style=MaterialTheme.typography.bodySmall)
            }
            Row {
                if(food.sourceKind!=NutritionSource.RECIPE) TextButton(onClick={editor=food}) {Text(stringResource(R.string.edit))}
                ConfirmButton(R.string.remove,stringResource(R.string.delete_food_note),!state.busy) {vm.deleteFood(food.id)}
            }
            HorizontalDivider()
        }
    }
    if(create||editor!=null) FoodDialog(editor,state.busy,{create=false;editor=null}) {vm.saveFood(it){create=false;editor=null}}
    if(recipe) RecipeDialog(state.snapshot.foods,state.busy,{recipe=false}) {vm.saveFood(it){recipe=false}}
}

@Composable
private fun FoodDialog(initial:Food?,busy:Boolean,onDismiss:()->Unit,onSave:(Food)->Unit) {
    var name by remember {mutableStateOf(initial?.name.orEmpty())}
    var amount by remember {mutableStateOf((initial?.referenceAmount?:100.0).toString())}
    var unit by remember {mutableStateOf(initial?.unit?:NutritionUnit.GRAMS)}
    var sourceKind by remember {mutableStateOf(initial?.sourceKind?:NutritionSource.LABEL)}
    var source by remember {mutableStateOf(initial?.source.orEmpty())}
    var kcal by remember {mutableStateOf(initial?.nutrients?.calories?.toString().orEmpty())}
    var protein by remember {mutableStateOf(initial?.nutrients?.protein?.toString().orEmpty())}
    var carbs by remember {mutableStateOf(initial?.nutrients?.carbs?.toString().orEmpty())}
    var fat by remember {mutableStateOf(initial?.nutrients?.fat?.toString().orEmpty())}
    val candidate=runCatching {Food(id=initial?.id?:java.util.UUID.randomUUID().toString(),name=name.trim(),referenceAmount=requireNotNull(NumericInput.decimal(amount)),unit=unit,
        nutrients=Nutrients(requireNotNull(NumericInput.decimal(kcal)),requireNotNull(NumericInput.decimal(protein)),requireNotNull(NumericInput.decimal(carbs)),requireNotNull(NumericInput.decimal(fat))),source=source.trim(),sourceKind=sourceKind)}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.new_food))},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Input(R.string.meal_name,name,{name=it},"food_name",numeric=false)
        NutritionUnit.entries.forEach {value -> FilterChip(selected=unit==value,onClick={unit=value},label={Text(stringResource(unitLabel(value)))})}
        Input(R.string.reference_amount,amount,{amount=it},"food_reference")
        Input(R.string.calories,kcal,{kcal=it},"food_calories");Input(R.string.protein,protein,{protein=it},"food_protein")
        Input(R.string.carbs,carbs,{carbs=it},"food_carbs");Input(R.string.fat,fat,{fat=it},"food_fat")
        Input(R.string.source_required,source,{source=it},"food_source",numeric=false)
        listOf(NutritionSource.LABEL,NutritionSource.MANUAL).forEach {value -> FilterChip(selected=sourceKind==value,onClick={sourceKind=value},label={Text(stringResource(if(value==NutritionSource.LABEL) R.string.label_source else R.string.manual_source))})}
        Text(stringResource(R.string.user_nutrition_note),style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={candidate?.let(onSave)},enabled=candidate!=null&&!busy,modifier=Modifier.testTag("save_food")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.cancel))}})
}

@Composable
private fun RecipeDialog(foods:List<Food>,busy:Boolean,onDismiss:()->Unit,onSave:(Food)->Unit) {
    var name by remember {mutableStateOf("")};var servings by remember {mutableStateOf("")};var source by remember {mutableStateOf("")}
    var selected by remember {mutableStateOf(foods.first().id)};var quantity by remember {mutableStateOf("")}
    var ingredients by remember {mutableStateOf<List<IngredientSnapshot>>(emptyList())}
    var expanded by remember {mutableStateOf(false)}
    val food=foods.firstOrNull {it.id==selected}?:foods.first()
    val ingredient=runCatching {food.ingredient(requireNotNull(NumericInput.decimal(quantity)))}.getOrNull()
    val recipe=runCatching {Food.recipe(name.trim(),requireNotNull(NumericInput.decimal(servings)),source.trim(),ingredients)}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.new_recipe))},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Input(R.string.meal_name,name,{name=it},"recipe_name",numeric=false)
        Input(R.string.recipe_yield,servings,{servings=it},"recipe_yield")
        Input(R.string.source_required,source,{source=it},"recipe_source",numeric=false)
        Box {
            OutlinedButton(onClick={expanded=true}) {Text(food.name)}
            DropdownMenu(expanded,onDismissRequest={expanded=false}) {foods.forEach {item -> DropdownMenuItem(text={Text(item.name)},onClick={selected=item.id;expanded=false})}}
        }
        Input(if(food.unit==NutritionUnit.GRAMS) R.string.quantity_grams else R.string.quantity_servings,quantity,{quantity=it},"ingredient_quantity")
        OutlinedButton(onClick={ingredient?.let {ingredients=ingredients+it;quantity=""}},enabled=ingredient!=null&&ingredients.size<30) {Text(stringResource(R.string.add_ingredient))}
        ingredients.forEachIndexed {index,item ->
            Text(stringResource(R.string.ingredient_record,item.name,number(item.quantity),stringResource(unitLabel(item.unit))))
            TextButton(onClick={ingredients=ingredients.filterIndexed {i,_ ->i!=index}}) {Text(stringResource(R.string.remove))}
        }
        Text(stringResource(R.string.recipe_note),style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={recipe?.let(onSave)},enabled=recipe!=null&&!busy,modifier=Modifier.testTag("save_recipe")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.cancel))}})
}

@Composable
internal fun NutrientSummary(kcal:Double,protein:Double,carbs:Double,fat:Double) {
    Text(stringResource(R.string.meal_summary,number(kcal,0),number(protein),number(carbs),number(fat)))
}
internal fun unitLabel(unit:NutritionUnit)=if(unit==NutritionUnit.GRAMS) R.string.grams else R.string.servings
