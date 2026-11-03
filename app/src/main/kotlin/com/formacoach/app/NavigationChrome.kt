package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
internal fun FormaScaffold(tab:Int,onTab:(Int)->Unit,today:LocalDate,onProfile:()->Unit,onWatch:()->Unit,onBackup:()->Unit,content:@Composable (PaddingValues)->Unit) {
    val labels=listOf(R.string.today,R.string.train,R.string.eat,R.string.progress,R.string.coach)
    val icons=listOf(Icons.Default.Home,Icons.Default.FitnessCenter,Icons.Default.Restaurant,Icons.AutoMirrored.Filled.ShowChart,Icons.Default.Forum)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide=maxWidth>=840.dp
        Row(Modifier.fillMaxSize()) {
            if(wide) NavigationRail(modifier=Modifier.width(108.dp).fillMaxHeight().statusBarsPadding(),containerColor=MaterialTheme.colorScheme.background,header={
                Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(vertical=18.dp)) {
                    Icon(Icons.Default.FitnessCenter,null,tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.padding(14.dp).size(28.dp))
                }
            }) {
                labels.forEachIndexed {index,label ->
                    NavigationRailItem(selected=tab==index,onClick={onTab(index)},icon={Icon(icons[index],null)},label={Text(stringResource(label))},modifier=Modifier.padding(vertical=6.dp).testTag("tab_$index"))
                }
            }
            Scaffold(modifier=Modifier.weight(1f).fillMaxHeight(),containerColor=MaterialTheme.colorScheme.background,
                topBar={Surface(color=MaterialTheme.colorScheme.background,shadowElevation=1.dp) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.app_name),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                            Text(dateLabel(today),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick=onWatch,modifier=Modifier.testTag("watch_settings")) {Icon(Icons.Default.Watch,stringResource(R.string.apple_watch))}
                        IconButton(onClick=onProfile) {Icon(Icons.Default.AccountCircle,stringResource(R.string.profile))}
                        IconButton(onClick=onBackup,modifier=Modifier.testTag("backup_settings")) {Icon(Icons.Default.SaveAlt,stringResource(R.string.backup))}
                    }
                }},
                bottomBar={if(!wide) NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
                    labels.forEachIndexed {index,label ->NavigationBarItem(selected=tab==index,onClick={onTab(index)},icon={Icon(icons[index],null)},label={Text(stringResource(label))},modifier=Modifier.testTag("tab_$index"))}
                }},content=content)
        }
    }
}
