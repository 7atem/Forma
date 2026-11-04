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
internal fun CopyMealDay(state:AppState,target:LocalDate,today:LocalDate,vm:FitnessViewModel) {
    var show by rememberSaveable {mutableStateOf(false)}
    var sourceDay by rememberSaveable(target) {mutableLongStateOf(maxOf(target.minusDays(1),LocalDate.of(1970,1,1)).toEpochDay())}
    var preview by remember {mutableStateOf<List<Meal>?>(null)}
    val source=LocalDate.ofEpochDay(sourceDay)
    val meals=state.snapshot.meals.filter {it.date==source}
    OutlinedButton(onClick={show=true},enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("copy_meal_day")) {Text(stringResource(R.string.copy_meal_day))}
    if(show) AlertDialog(onDismissRequest={show=false;preview=null},title={Text(stringResource(R.string.copy_meal_day))},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.copy_day_note,dateLabel(target)))
        DateChoice(source,today,{sourceDay=it.toEpochDay();preview=null},"copy_food_source")
        val values=preview?:meals
        Text(stringResource(R.string.copy_meal_count,number(values.size.toDouble(),0)))
        values.take(200).forEach {Text(it.name);NutrientSummary(it.calories,it.protein,it.carbs,it.fat)}
    }},confirmButton={TextButton(onClick={
        if(preview==null) preview=meals.toList()
        else vm.copyMealDay(checkNotNull(preview),target){show=false;preview=null}
    },enabled=!state.busy&&source!=target&&meals.size in 1..200,modifier=Modifier.testTag("confirm_copy_day")) {Text(stringResource(if(preview==null) R.string.review_copy_day else R.string.confirm))}},dismissButton={TextButton(onClick={show=false;preview=null}) {Text(stringResource(R.string.cancel))}})
}
