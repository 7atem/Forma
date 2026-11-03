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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
    secondary=Color(0xFFFFBD8A), secondaryContainer=Color(0xFF36583B), onSecondaryContainer=Color(0xFFE5F7DE), background=Color(0xFF10231D),
    surface=Color(0xFF19332A), surfaceVariant=Color(0xFF254237),
    onBackground=Color(0xFFF2F6EC), onSurface=Color(0xFFF2F6EC),
    onSurfaceVariant=Color(0xFFBED0C3), outline=Color(0xFF6E8C7A),
)

@Composable
fun FormaApp(vm: FitnessViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable {mutableStateOf(false)}
    var generation by rememberSaveable {mutableIntStateOf(0)}
    val pageState=key(generation) {rememberSaveableStateHolder()}
    // Re-evaluate calendar totals if the app remains open over local midnight.
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) { today=LocalDate.now(); kotlinx.coroutines.delay(30_000) }
    }
    LaunchedEffect(today) {vm.onDayChanged()}
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
                        onCancel=if (state.snapshot.profile == null) null else ({ editingProfile=false }),
                        onBackup={showBackup=true})
                }
            } else {
                val titles=listOf(R.string.today, R.string.train, R.string.eat, R.string.progress, R.string.coach)
                val icons=listOf(Icons.Default.Home,Icons.Default.FitnessCenter,Icons.Default.Restaurant,Icons.AutoMirrored.Filled.ShowChart,Icons.Default.Forum)
                Scaffold(
                    containerColor=MaterialTheme.colorScheme.background,
                    bottomBar={
                        NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
                            titles.forEachIndexed { index, title ->
                                NavigationBarItem(selected=tab==index, onClick={tab=index},
                                    icon={Icon(icons[index],contentDescription=null)},
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
                                IconButton(onClick={editingProfile=true}) {Icon(Icons.Default.AccountCircle,contentDescription=stringResource(R.string.profile))}
                                IconButton(onClick={showBackup=true},modifier=Modifier.testTag("backup_settings")) {Icon(Icons.Default.SaveAlt,contentDescription=stringResource(R.string.backup))}
                            }
                            if(state.error) {
                                InfoCard { Text(stringResource(R.string.save_failed)); TextButton(onClick=vm::clearError) { Text(stringResource(R.string.dismiss)) } }
                            }
                            when(tab) {
                                0 -> TodayContent(state.snapshot, today, vm, onTrain={tab=1}, onEat={tab=2})
                                1 -> TrainContent(state, today, vm)
                                2 -> EatContent(state, today, vm)
                                3 -> ProgressContent(state, today, vm)
                                4 -> CoachContent(state,today,vm)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        }
                    }
                }
            }
            if(showBackup) BackupDialog(vm,state,onDismiss={showBackup=false},onRestored={showBackup=false;editingProfile=false;tab=0;generation++})
        }
    }
}

@Composable
private fun ProfileForm(profile: Profile?, busy: Boolean, error: Boolean, onSave:(Profile)->Unit, onCancel:(()->Unit)?, onBackup:()->Unit) {
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
            (2..4).forEach { count -> FilterChip(selected=sessions==count, onClick={sessions=count}, label={Text(number(count.toDouble(),0))},modifier=Modifier.testTag("target_sessions_$count")) }
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
        TextButton(onClick=onBackup) {Text(stringResource(R.string.backup))}
        Text(stringResource(R.string.local_privacy),style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TodayContent(snapshot: FitnessSnapshot, today: LocalDate, vm: FitnessViewModel, onTrain:()->Unit, onEat:()->Unit) {
    val profile=checkNotNull(snapshot.profile)
    val meals=snapshot.meals.filter { it.date==today }
    val review=vm.reviewEngine.review(snapshot,today)
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
internal fun InfoCard(content:@Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}

@Composable
internal fun Input(label: Int, value: String, onChange:(String)->Unit, tag: String, numeric: Boolean=true) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(stringResource(label))},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric) KeyboardType.Decimal else KeyboardType.Text,imeAction=ImeAction.Done),
        modifier=Modifier.fillMaxWidth().testTag(tag))
}

@Composable
internal fun number(value: Double, digits: Int=2): String {
    val locale=LocalConfiguration.current.locales[0]
    return NumberFormat.getNumberInstance(locale).apply {maximumFractionDigits=digits}.format(value)
}

@Composable
internal fun dateLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0]))

internal fun goalLabel(goal: Goal): Int = when(goal) {
    Goal.FAT_LOSS -> R.string.goal_fat_loss
    Goal.BUILD_STRENGTH -> R.string.goal_strength
    Goal.BUILD_HABIT -> R.string.goal_habit
}
internal fun exerciseLabel(exercise: Exercise): Int = when(exercise) {
    Exercise.SQUAT -> R.string.exercise_squat
    Exercise.LEG_PRESS -> R.string.exercise_leg_press
    Exercise.CHEST_PRESS -> R.string.exercise_chest_press
    Exercise.ROW -> R.string.exercise_row
    Exercise.LAT_PULLDOWN -> R.string.exercise_lat_pulldown
    Exercise.HIP_HINGE -> R.string.exercise_hip_hinge
    Exercise.SHOULDER_PRESS -> R.string.exercise_shoulder_press
    Exercise.SPLIT_SQUAT -> R.string.exercise_split_squat
}
internal fun actionLabel(action: NextAction): Int = when(action) {
    NextAction.LOG_FIRST_SESSION -> R.string.action_first_session
    NextAction.KEEP_TRAINING_LOG -> R.string.action_keep_training
    NextAction.ADD_MEAL_CONTEXT -> R.string.action_meals
    NextAction.ADD_MEASUREMENTS -> R.string.action_measurements
    NextAction.REVIEW_WITH_CONTEXT -> R.string.action_review
}
