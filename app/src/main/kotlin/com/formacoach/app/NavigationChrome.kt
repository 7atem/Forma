package com.formacoach.app

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
internal fun FormaScaffold(
    tab: Int,
    onTab: (Int) -> Unit,
    today: LocalDate,
    snackbarHostState: SnackbarHostState,
    onQuickLog: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    showQuickLog: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val labels = listOf(R.string.today, R.string.train, R.string.eat, R.string.progress)
    val icons = listOf(
        Icons.Default.Today,
        Icons.Default.FitnessCenter,
        Icons.Default.Restaurant,
        Icons.Default.Insights,
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
                            label = { Text(stringResource(label), fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    // Hidden during an active workout on Train, where it would cover set controls.
                    if (showQuickLog) FloatingActionButton(
                        onClick = onQuickLog,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("quick_log_fab"),
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
                                // At large text sizes the decorative logo and date give their space to the title and actions.
                                val largeText = LocalDensity.current.fontScale >= 1.5f
                                if (!wide && !largeText) {
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
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                    if (!largeText) {
                                        Text(
                                            dateLabel(today),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                TopBarAction(Icons.Default.AccountCircle, R.string.profile, onProfile)
                                TopBarAction(Icons.Default.Settings, R.string.settings_title, onSettings, Modifier.testTag("open_settings"))
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
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
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
        IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
            Icon(
                icon,
                contentDescription = stringResource(description),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
