package com.formacoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formacoach.coach.NextAction
import com.formacoach.model.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FormaApp() }
    }
}

private val FormaColors = darkColorScheme(
    primary=Color(0xFFC5EE89), onPrimary=Color(0xFF18311B),
    secondary=Color(0xFFFFBD8A), background=Color(0xFF10231D),
    surface=Color(0xFF19332A), surfaceVariant=Color(0xFF254237),
    onBackground=Color(0xFFF2F6EC), onSurface=Color(0xFFF2F6EC),
    onSurfaceVariant=Color(0xFFBED0C3), outline=Color(0xFF6E8C7A),
)

@Composable
fun FormaApp(vm: FitnessViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    val pageState=rememberSaveableStateHolder()
    // Re-evaluate calendar totals if the app remains open over local midnight.
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) { today=LocalDate.now(); kotlinx.coroutines.delay(30_000) }
    }
    MaterialTheme(colorScheme=FormaColors, shapes=Shapes(medium=RoundedCornerShape(20.dp), large=RoundedCornerShape(28.dp))) {
        Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            } else if (state.loadFailed) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment=Alignment.Center) {
                    Text(stringResource(R.string.load_failed))
                }
            } else if (state.snapshot.profile == null || editingProfile) {
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding(), contentAlignment=Alignment.TopCenter) {
                    ProfileForm(state.snapshot.profile, state.busy, state.error,
                        onSave={ vm.saveProfile(it) { editingProfile=false } },
                        onCancel=if (state.snapshot.profile == null) null else ({ editingProfile=false }))
                }
            } else {
                val titles=listOf(R.string.today, R.string.train, R.string.eat, R.string.progress)
                Scaffold(
                    containerColor=MaterialTheme.colorScheme.background,
                    bottomBar={
                        NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
                            titles.forEachIndexed { index, title ->
                                NavigationBarItem(selected=tab==index, onClick={tab=index},
                                    icon={Text(if(tab==index) "●" else "○")},
                                    label={Text(stringResource(title))}, modifier=Modifier.testTag("tab_$index"))
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment=Alignment.TopCenter) {
                        pageState.SaveableStateProvider(tab) {
                        Column(Modifier.widthIn(max=760.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.app_name), style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
                                    Text(dateLabel(today), style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick={editingProfile=true}) { Text(stringResource(R.string.profile)) }
                            }
                            if(state.error) {
                                InfoCard { Text(stringResource(R.string.save_failed)); TextButton(onClick=vm::clearError) { Text(stringResource(R.string.dismiss)) } }
                            }
                            when(tab) {
                                0 -> TodayContent(state.snapshot, today, vm, onTrain={tab=1}, onEat={tab=2})
                                1 -> TrainContent(state, vm)
                                2 -> EatContent(state, today, vm)
                                3 -> ProgressContent(state, today, vm)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileForm(profile: Profile?, busy: Boolean, error: Boolean, onSave:(Profile)->Unit, onCancel:(()->Unit)?) {
    var name by rememberSaveable { mutableStateOf(profile?.name.orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(profile?.goal ?: Goal.FAT_LOSS) }
    var sessions by rememberSaveable { mutableIntStateOf(profile?.sessionsPerWeek ?: 3) }
    var calories by rememberSaveable { mutableStateOf(profile?.caloriesTarget?.toString().orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(profile?.proteinTarget?.toString().orEmpty()) }
    val candidate=runCatching {
        Profile(name.trim(), goal, sessions,
            if(calories.isBlank()) null else requireNotNull(NumericInput.integer(calories)),
            if(protein.isBlank()) null else requireNotNull(NumericInput.decimal(protein)))
    }.getOrNull()
    Column(Modifier.widthIn(max=600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.welcome), style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold)
        Text(stringResource(R.string.welcome_body), color=MaterialTheme.colorScheme.onSurfaceVariant)
        Input(R.string.name, name, {name=it}, "profile_name", numeric=false)
        Text(stringResource(R.string.your_goal), style=MaterialTheme.typography.titleMedium)
        Goal.entries.forEach { value ->
            FilterChip(selected=goal==value, onClick={goal=value}, label={Text(stringResource(goalLabel(value)))})
        }
        Text(stringResource(R.string.sessions_per_week))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            (2..4).forEach { count -> FilterChip(selected=sessions==count, onClick={sessions=count}, label={Text(number(count.toDouble(),0))}) }
        }
        InfoCard {
            Text(stringResource(R.string.manual_targets), style=MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.manual_targets_body))
            Input(R.string.calorie_target,calories,{calories=it},"profile_calories")
            Input(R.string.protein_target,protein,{protein=it},"profile_protein")
        }
        if(error) Text(stringResource(R.string.save_failed),color=MaterialTheme.colorScheme.error)
        Button(onClick={candidate?.let(onSave)}, enabled=candidate!=null&&!busy, modifier=Modifier.fillMaxWidth().testTag("save_profile")) {
            Text(stringResource(if(profile==null) R.string.start_journey else R.string.save))
        }
        onCancel?.let { TextButton(onClick=it) { Text(stringResource(R.string.cancel)) } }
        Text(stringResource(R.string.local_privacy),style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TodayContent(snapshot: FitnessSnapshot, today: LocalDate, vm: FitnessViewModel, onTrain:()->Unit, onEat:()->Unit) {
    val profile=checkNotNull(snapshot.profile)
    val meals=snapshot.meals.filter { it.date==today }
    val review=vm.reviewEngine.review(snapshot)
    Text(stringResource(R.string.greeting,profile.name),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    Text(stringResource(R.string.today_subtitle),color=MaterialTheme.colorScheme.onSurfaceVariant)
    InfoCard {
        Text(stringResource(R.string.your_week),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.workout_count,number(review.completedSessions.toDouble(),0),number(profile.sessionsPerWeek.toDouble(),0)),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.completed_sessions))
        Button(onClick=onTrain,modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_training)) }
    }
    InfoCard {
        Text(stringResource(R.string.food_today),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.kcal_value,number(meals.sumOf {it.calories},0)),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.protein_value,number(meals.sumOf {it.protein})))
        profile.caloriesTarget?.let { Text(stringResource(R.string.user_calorie_target,number(it.toDouble(),0))) }
        profile.proteinTarget?.let { Text(stringResource(R.string.user_protein_target,number(it))) }
        Text(stringResource(R.string.logged_only),style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick=onEat,modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_meal)) }
    }
    InfoCard {
        Text(stringResource(R.string.next_step),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(actionLabel(review.nextAction)))
    }
    Text(stringResource(R.string.alpha_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TrainContent(state: AppState, vm: FitnessViewModel) {
    val active=state.snapshot.sessions.firstOrNull {it.completedAt==null}
    Text(stringResource(R.string.training_title),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    Text(stringResource(R.string.template_note),color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(active==null) {
        StarterTemplates.all.forEach { template ->
            InfoCard {
                Text(stringResource(if(template.id=="A") R.string.template_a else R.string.template_b),style=MaterialTheme.typography.titleLarge)
                template.exercises.forEach { Text(stringResource(exerciseLabel(it))) }
                Button(onClick={vm.start(template.id)},enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("start_${template.id}")) { Text(stringResource(R.string.start_session)) }
            }
        }
    } else {
        SessionForm(active,state.snapshot.sets.filter {it.sessionId==active.id},state.busy,vm)
    }
    val completed=state.snapshot.sessions.filter {it.completedAt!=null}.take(10)
    Text(stringResource(R.string.recent_sessions),style=MaterialTheme.typography.titleLarge)
    if(completed.isEmpty()) Text(stringResource(R.string.no_sessions))
    completed.forEach { session ->
        InfoCard {
            Text(dateLabel(session.date))
            Text(stringResource(R.string.session_summary,session.templateId,number(state.snapshot.sets.count {it.sessionId==session.id}.toDouble(),0)))
            state.snapshot.sets.filter {it.sessionId==session.id}.forEach { set ->
                Text(stringResource(R.string.set_summary,stringResource(exerciseLabel(set.exercise)),number(set.reps.toDouble(),0),number(set.weightKg)))
            }
        }
    }
}

@Composable
private fun SessionForm(session: Session, sets: List<TrainingSet>, busy: Boolean, vm: FitnessViewModel) {
    var exercise by rememberSaveable(session.id) { mutableStateOf(StarterTemplates.all.first {it.id==session.templateId}.exercises.first()) }
    var reps by rememberSaveable(session.id) { mutableStateOf("") }
    var weight by rememberSaveable(session.id) { mutableStateOf("") }
    val candidate=runCatching { TrainingSet(sessionId=session.id, exercise=exercise, reps=requireNotNull(NumericInput.integer(reps)), weightKg=requireNotNull(NumericInput.decimal(weight))) }.getOrNull()
    InfoCard {
        Text(stringResource(R.string.active_session,session.templateId),style=MaterialTheme.typography.titleLarge)
        Text(dateLabel(session.date))
        Exercise.entries.forEach { value ->
            FilterChip(selected=exercise==value,onClick={exercise=value},label={Text(stringResource(exerciseLabel(value)))})
        }
        Input(R.string.reps,reps,{reps=it},"set_reps")
        Input(R.string.weight_kg,weight,{weight=it},"set_weight")
        Text(stringResource(R.string.zero_external_load),style=MaterialTheme.typography.bodySmall)
        Button(onClick={candidate?.let {vm.addSet(session.id,it.exercise,it.reps,it.weightKg) {reps="";weight=""}}},enabled=candidate!=null&&!busy,modifier=Modifier.fillMaxWidth().testTag("add_set")) { Text(stringResource(R.string.add_set)) }
        sets.forEach { set ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(R.string.set_summary,stringResource(exerciseLabel(set.exercise)),number(set.reps.toDouble(),0),number(set.weightKg)),Modifier.weight(1f))
                TextButton(onClick={vm.removeSet(set.id)},enabled=!busy) {Text(stringResource(R.string.remove))}
            }
        }
        OutlinedButton(onClick={vm.finish(session.id)},enabled=sets.isNotEmpty()&&!busy,modifier=Modifier.fillMaxWidth().testTag("finish_session")) { Text(stringResource(R.string.finish_session)) }
    }
}

@Composable
private fun EatContent(state: AppState, today: LocalDate, vm: FitnessViewModel) {
    var name by rememberSaveable {mutableStateOf("")}
    var calories by rememberSaveable {mutableStateOf("")}
    var protein by rememberSaveable {mutableStateOf("")}
    var carbs by rememberSaveable {mutableStateOf("")}
    var fat by rememberSaveable {mutableStateOf("")}
    val meal=runCatching {Meal(date=today,name=name.trim(),calories=requireNotNull(NumericInput.decimal(calories)),protein=requireNotNull(NumericInput.decimal(protein)),carbs=requireNotNull(NumericInput.decimal(carbs)),fat=requireNotNull(NumericInput.decimal(fat)))}.getOrNull()
    Text(stringResource(R.string.eat_title),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    InfoCard {
        Text(stringResource(R.string.portion_note))
        Input(R.string.meal_name,name,{name=it},"meal_name",numeric=false)
        Input(R.string.calories,calories,{calories=it},"meal_calories")
        Input(R.string.protein,protein,{protein=it},"meal_protein")
        Input(R.string.carbs,carbs,{carbs=it},"meal_carbs")
        Input(R.string.fat,fat,{fat=it},"meal_fat")
        Button(onClick={meal?.let {vm.addMeal(it){name="";calories="";protein="";carbs="";fat=""}}},enabled=meal!=null&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("save_meal")) {Text(stringResource(R.string.save_meal))}
    }
    val todayMeals=state.snapshot.meals.filter {it.date==today}
    Text(stringResource(R.string.food_today),style=MaterialTheme.typography.titleLarge)
    if(todayMeals.isEmpty()) Text(stringResource(R.string.no_meals))
    todayMeals.forEach { mealRow ->
        InfoCard {
            Text(mealRow.name,style=MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.meal_summary,number(mealRow.calories,0),number(mealRow.protein),number(mealRow.carbs),number(mealRow.fat)))
            TextButton(onClick={vm.removeMeal(mealRow.id)},enabled=!state.busy) {Text(stringResource(R.string.remove))}
        }
    }
    Text(stringResource(R.string.reuse_meal),style=MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.reuse_note),style=MaterialTheme.typography.bodySmall)
    state.snapshot.meals.distinctBy {listOf(it.name,it.calories,it.protein,it.carbs,it.fat)}.take(5).forEach { mealRow ->
        InfoCard {
            Text(mealRow.name)
            Text(stringResource(R.string.meal_summary,number(mealRow.calories,0),number(mealRow.protein),number(mealRow.carbs),number(mealRow.fat)))
            OutlinedButton(onClick={vm.repeatMeal(mealRow)},enabled=!state.busy) {Text(stringResource(R.string.log_again))}
        }
    }
}

@Composable
private fun ProgressContent(state: AppState, today: LocalDate, vm: FitnessViewModel) {
    var weight by rememberSaveable {mutableStateOf("")}
    var waist by rememberSaveable {mutableStateOf("")}
    val measure=runCatching { Measurement(today,
        if(weight.isBlank()) null else requireNotNull(NumericInput.decimal(weight)),
        if(waist.isBlank()) null else requireNotNull(NumericInput.decimal(waist))) }.getOrNull()
    val review=vm.reviewEngine.review(state.snapshot)
    Text(stringResource(R.string.progress_title),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    InfoCard {
        Text(stringResource(R.string.measure_today),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.measure_note),style=MaterialTheme.typography.bodySmall)
        Input(R.string.body_weight,weight,{weight=it},"body_weight")
        Input(R.string.waist,waist,{waist=it},"waist")
        Button(onClick={measure?.let {vm.measure(it){weight="";waist=""}}},enabled=measure!=null&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("save_measurement")) {Text(stringResource(R.string.save_measurement))}
    }
    InfoCard {
        Text(stringResource(R.string.weekly_review),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.date_range,dateLabel(review.start),dateLabel(review.end)))
        Text(stringResource(R.string.review_sessions,number(review.completedSessions.toDouble(),0)))
        Text(stringResource(R.string.review_meal_days,number(review.mealDays.toDouble(),0)))
        review.meanLoggedCalories?.let {Text(stringResource(R.string.review_calories,number(it,0)))}
        Text(stringResource(R.string.logged_only),style=MaterialTheme.typography.bodySmall)
        review.meanWeight?.let {Text(stringResource(R.string.review_weight,number(it),number(review.weightObservations.toDouble(),0)))}
        review.meanWaist?.let {Text(stringResource(R.string.review_waist,number(it)))}
        val weightDelta=review.weightDelta
        if(weightDelta!=null) {
            Text(stringResource(R.string.review_delta,number(weightDelta)))
            Text(stringResource(R.string.comparison_range,dateLabel(review.start.minusDays(7)),dateLabel(review.start.minusDays(1)),number(review.previousWeightObservations.toDouble(),0)),style=MaterialTheme.typography.bodySmall)
        } else Text(stringResource(R.string.not_enough_weight),style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(stringResource(actionLabel(review.nextAction)))
        Text(stringResource(R.string.no_auto_change),style=MaterialTheme.typography.bodySmall)
    }
    Text(stringResource(R.string.measurement_history),style=MaterialTheme.typography.titleLarge)
    if(state.snapshot.measurements.isEmpty()) Text(stringResource(R.string.no_measurements))
    state.snapshot.measurements.take(14).forEach {value ->
        InfoCard {
            Text(dateLabel(value.date))
            value.weightKg?.let {Text(stringResource(R.string.body_weight_value,number(it)))}
            value.waistCm?.let {Text(stringResource(R.string.waist_value,number(it)))}
            TextButton(onClick={vm.removeMeasurement(value.date)},enabled=!state.busy) {Text(stringResource(R.string.remove))}
        }
    }
}

@Composable
private fun InfoCard(content:@Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}

@Composable
private fun Input(label: Int, value: String, onChange:(String)->Unit, tag: String, numeric: Boolean=true) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(stringResource(label))},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric) KeyboardType.Decimal else KeyboardType.Text,imeAction=ImeAction.Done),
        modifier=Modifier.fillMaxWidth().testTag(tag))
}

@Composable
private fun number(value: Double, digits: Int=1): String {
    val locale=LocalConfiguration.current.locales[0]
    return NumberFormat.getNumberInstance(locale).apply {maximumFractionDigits=digits}.format(value)
}

@Composable
private fun dateLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0]))

private fun goalLabel(goal: Goal): Int = when(goal) {
    Goal.FAT_LOSS -> R.string.goal_fat_loss
    Goal.BUILD_STRENGTH -> R.string.goal_strength
    Goal.BUILD_HABIT -> R.string.goal_habit
}
private fun exerciseLabel(exercise: Exercise): Int = when(exercise) {
    Exercise.SQUAT -> R.string.exercise_squat
    Exercise.LEG_PRESS -> R.string.exercise_leg_press
    Exercise.CHEST_PRESS -> R.string.exercise_chest_press
    Exercise.ROW -> R.string.exercise_row
    Exercise.LAT_PULLDOWN -> R.string.exercise_lat_pulldown
    Exercise.HIP_HINGE -> R.string.exercise_hip_hinge
    Exercise.SHOULDER_PRESS -> R.string.exercise_shoulder_press
    Exercise.SPLIT_SQUAT -> R.string.exercise_split_squat
}
private fun actionLabel(action: NextAction): Int = when(action) {
    NextAction.LOG_FIRST_SESSION -> R.string.action_first_session
    NextAction.KEEP_TRAINING_LOG -> R.string.action_keep_training
    NextAction.ADD_MEAL_CONTEXT -> R.string.action_meals
    NextAction.ADD_MEASUREMENTS -> R.string.action_measurements
    NextAction.REVIEW_WITH_CONTEXT -> R.string.action_review
}
