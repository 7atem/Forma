package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.formacoach.coach.WorkoutCalendar
import com.formacoach.model.FitnessSnapshot
import java.time.*
import java.time.format.*
import java.time.temporal.WeekFields

@Composable
internal fun WorkoutCalendarPanel(data:FitnessSnapshot,today:LocalDate,selected:LocalDate?,onDay:(LocalDate?)->Unit) {
    var expanded by rememberSaveable {mutableStateOf(false)}
    var monthIndex by rememberSaveable {mutableLongStateOf(today.year*12L+today.monthValue-1)}
    val month=YearMonth.of((monthIndex/12).toInt(),(monthIndex%12+1).toInt())
    val locale=LocalConfiguration.current.locales[0]
    InfoCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.workout_calendar),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
            TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("toggle_workout_calendar")) {Text(stringResource(if(expanded) R.string.show_less else R.string.browse_dates))}
        }
        if(selected!=null) {
            Text(stringResource(R.string.workout_date_filter,dateLabel(selected)),modifier=Modifier.testTag("workout_date_filter"))
            TextButton(onClick={onDay(null)},modifier=Modifier.testTag("clear_workout_date")) {Text(stringResource(R.string.all_workout_dates))}
        }
        if(expanded) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={monthIndex--},enabled=month>YearMonth.of(1970,1),modifier=Modifier.testTag("calendar_previous")) {Icon(Icons.AutoMirrored.Filled.ArrowBack,stringResource(R.string.previous_month))}
                Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",locale)),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                IconButton(onClick={monthIndex++},enabled=month<YearMonth.from(today),modifier=Modifier.testTag("calendar_next")) {Icon(Icons.AutoMirrored.Filled.ArrowForward,stringResource(R.string.next_month))}
            }
            val first=WeekFields.of(locale).firstDayOfWeek
            Row(Modifier.fillMaxWidth()) {repeat(7) {i ->Text(first.plus(i.toLong()).getDisplayName(TextStyle.NARROW,locale),Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)}}
            val days=remember(data.sessions,month,today) {WorkoutCalendar.month(data,month,today)}
            val lead=(month.atDay(1).dayOfWeek.value-first.value+7)%7
            val cells=List<WorkoutCalendar.Day?>(lead){null}+days
            cells.chunked(7).forEach {week ->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                week.forEach {day ->
                    if(day==null) Spacer(Modifier.weight(1f))
                    else {
                        val description=stringResource(R.string.calendar_day_accessibility,dateLabel(day.date),number(day.completed.toDouble(),0))
                        OutlinedButton(onClick={onDay(day.date);expanded=false},enabled=day.date<=today,shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(2.dp),
                            colors=ButtonDefaults.outlinedButtonColors(containerColor=if(day.date==selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                            modifier=Modifier.weight(1f).heightIn(min=58.dp).semantics {contentDescription=description}.testTag("calendar_day_${day.date}")) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(number(day.date.dayOfMonth.toDouble(),0),style=MaterialTheme.typography.labelLarge)
                                if(day.completed>0) Text(number(day.completed.toDouble(),0),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                repeat(7-week.size) {Spacer(Modifier.weight(1f))}
            }}
            Text(stringResource(R.string.calendar_scope_note),style=MaterialTheme.typography.bodySmall)
        }
    }
}
