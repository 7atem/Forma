package com.formacoach.app

import androidx.compose.foundation.BorderStroke
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
import com.formacoach.model.WeightUnit
import java.time.LocalDate

@Composable
internal fun FormaScaffold(
    tab: Int,
    onTab: (Int) -> Unit,
    today: LocalDate,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    weightUnit: WeightUnit,
    onToggleUnit: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onQuickLog: () -> Unit,
    onProfile: () -> Unit,
    onWatch: () -> Unit,
    onBackup: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val labels = listOf(R.string.exercises_tab, R.string.train, R.string.eat, R.string.you_tab)
    val icons = listOf(
        Icons.Default.AccessibilityNew,
        Icons.Default.FitnessCenter,
        Icons.Default.Restaurant,
        Icons.Default.Person,
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                NavigationRail(
                    modifier = Modifier.width(104.dp).fillMaxHeight().statusBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    header = {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 18.dp),
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(12.dp).size(26.dp),
                            )
                        }
                    },
                ) {
                    labels.forEachIndexed { index, label ->
                        NavigationRailItem(
                            selected = tab == index,
                            onClick = { onTab(index) },
                            icon = { Icon(icons[index], null) },
                            label = { Text(stringResource(label), fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.padding(vertical = 6.dp).testTag("tab_$index"),
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onQuickLog,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_log))
                    }
                },
                topBar = {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (!wide) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Text(
                                        dateLabel(today),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    onClick = onToggleUnit,
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                ) {
                                    Text(
                                        stringResource(if (weightUnit == WeightUnit.KG) R.string.kg else R.string.lb),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    )
                                }
                                TopBarAction(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, R.string.toggle_theme, onToggleTheme)
                                TopBarAction(Icons.Default.Watch, R.string.apple_watch, onWatch, Modifier.testTag("watch_settings"))
                                TopBarAction(Icons.Default.AccountCircle, R.string.profile, onProfile)
                                TopBarAction(Icons.Default.SaveAlt, R.string.backup, onBackup, Modifier.testTag("backup_settings"))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        }
                    }
                },
                bottomBar = {
                    if (!wide) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                tonalElevation = 0.dp,
                            ) {
                                labels.forEachIndexed { index, label ->
                                    NavigationBarItem(
                                        selected = tab == index,
                                        onClick = { onTab(index) },
                                        icon = { Icon(icons[index], null) },
                                        label = {
                                            Text(
                                                stringResource(label),
                                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier.testTag("tab_$index"),
                                    )
                                }
                            }
                        }
                    }
                },
                content = content,
            )
        }
    }
}

@Composable
private fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        IconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
            Icon(
                icon,
                contentDescription = stringResource(description),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
