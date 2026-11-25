package com.formacoach.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.formacoach.model.NumericInput
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
internal fun PageHeading(title: Int, subtitle: Int? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        subtitle?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun EmptyState(title: Int, body: Int, icon: ImageVector = Icons.Default.Insights) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun CircularProgressRing(
    progress: Float,
    valueText: String,
    labelText: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 9.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
) {
    val reduceMotion = rememberReduceMotion()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 550),
        label = "ring_progress",
    )
    Box(
        modifier = modifier.size(size).clearAndSetSemantics {
            contentDescription = listOf(valueText, labelText).filter { it.isNotBlank() }.joinToString(" ")
            this[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo] =
                androidx.compose.ui.semantics.ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - strokeWidth.toPx(), this.size.height - strokeWidth.toPx())
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            if (animated > 0.005f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animated * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                valueText,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (labelText.isNotBlank()) {
                Text(
                    labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun WeeklyActivityStrip(
    today: LocalDate,
    activeDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            days.forEach { day ->
                val done = day in activeDates
                val isToday = day == today
                val dayDescription = stringResource(
                    if (done) R.string.day_workout_recorded else R.string.day_no_workout_recorded,
                    day.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(locale)),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = dayDescription },
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = CircleShape,
                        color = when {
                            done -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = if (isToday && !done) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (done) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Text(
                                    number(day.dayOfMonth.toDouble(), 0),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
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
internal fun SegmentedTabs(
    selectedIndex: Int,
    labels: List<Int>,
    onSelect: (Int) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = selected,
                            role = androidx.compose.ui.semantics.Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .testTag("${tagPrefix}_$index"),
                ) {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                            // Large text may wrap a label onto two centred lines, but never mid-word.
                            FitText(
                                stringResource(label),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun StepperInput(
    label: Int,
    value: String,
    onChange: (String) -> Unit,
    tag: String,
    step: Double,
    min: Double = 0.0,
    max: Double = 1000.0,
    integer: Boolean = false,
    error: Boolean = false,
    placeholder: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    fun adjust(delta: Double) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val current = if (integer) NumericInput.integer(value)?.toDouble() else NumericInput.decimal(value)
        val base = current ?: if (delta > 0) 0.0 else min
        val next = (base + delta).coerceIn(min, max)
        val formatted = formatInputNumber(next, locale, if (integer) 0 else 2)
        onChange(formatted)
    }
    @Composable
    fun StepButton(delta: Double, modifier: Modifier) {
        val increase = delta > 0
        FilledTonalIconButton(
            onClick = { adjust(delta) },
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (increase) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (increase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(
                if (increase) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = stringResource(if (increase) R.string.increase_value else R.string.decrease_value, stringResource(label)),
                modifier = Modifier.size(18.dp),
            )
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 240.dp) {
            // Enough width (a full-width field): −  [field]  +  on one line keeps the set form short.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton(-step, Modifier.size(56.dp))
                Box(Modifier.weight(1f)) {
                    Input(label = label, value = value, onChange = onChange, tag = tag, numeric = true, error = error, placeholder = placeholder)
                }
                StepButton(step, Modifier.size(56.dp))
            }
        } else {
            // Narrow (two fields side by side): buttons go under the field so it stays readable.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Input(label = label, value = value, onChange = onChange, tag = tag, numeric = true, error = error, placeholder = placeholder)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepButton(-step, Modifier.weight(1f).height(48.dp))
                    StepButton(step, Modifier.weight(1f).height(48.dp))
                }
            }
        }
    }
}

@Composable
internal fun MacroBreakdownBar(
    protein: Double,
    carbs: Double,
    fat: Double,
    modifier: Modifier = Modifier,
) {
    val pCal = (protein * 4.0).coerceAtLeast(0.0)
    val cCal = (carbs * 4.0).coerceAtLeast(0.0)
    val fCal = (fat * 9.0).coerceAtLeast(0.0)
    val total = pCal + cCal + fCal
    if (total <= 0.0) return
    val description = stringResource(
        R.string.macro_chart_description,
        number(protein),
        number(carbs),
        number(fat),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (pCal > 0) Box(Modifier.weight(pCal.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
        if (cCal > 0) Box(Modifier.weight(cCal.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
        if (fCal > 0) Box(Modifier.weight(fCal.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
internal fun BarbellVisualizer(
    perSidePlates: List<Double>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Canvas(Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
            val cy = size.height / 2f
            val shaftH = 8.dp.toPx()
            val sleeveH = 12.dp.toPx()
            val centerSpan = size.width * 0.34f
            val leftCollarX = (size.width - centerSpan) / 2f
            val rightCollarX = (size.width + centerSpan) / 2f

            // Metallic bar shaft
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFD5E0DA), Color(0xFF7B8F85), Color(0xFFB5C4BC))),
                topLeft = Offset(6.dp.toPx(), cy - shaftH / 2f),
                size = Size(size.width - 12.dp.toPx(), shaftH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )

            // Left & right sleeves
            val collarW = 7.dp.toPx()
            val collarH = 22.dp.toPx()
            drawRoundRect(Color(0xFF94A89E), Offset(leftCollarX - collarW, cy - collarH / 2f), Size(collarW, collarH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
            drawRoundRect(Color(0xFF94A89E), Offset(rightCollarX, cy - collarH / 2f), Size(collarW, collarH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))

            // Stack plates symmetrically from collars outward
            val gap = 2.5.dp.toPx()
            var leftX = leftCollarX - collarW - gap
            var rightX = rightCollarX + collarW + gap
            perSidePlates.take(8).forEach { plate ->
                val (color, heightFrac, widthDp) = when {
                    plate >= 25.0 -> Triple(Color(0xFFEF4444), 0.96f, 12.dp)
                    plate >= 20.0 -> Triple(Color(0xFF3B82F6), 0.92f, 11.dp)
                    plate >= 15.0 -> Triple(Color(0xFFFACC15), 0.84f, 10.dp)
                    plate >= 10.0 -> Triple(Color(0xFF22C55E), 0.76f, 9.5.dp)
                    plate >= 5.0 -> Triple(Color(0xFFE2E8F0), 0.64f, 8.5.dp)
                    plate >= 2.5 -> Triple(Color(0xFF64748B), 0.54f, 7.5.dp)
                    plate >= 1.25 -> Triple(Color(0xFFCBD5E1), 0.44f, 6.5.dp)
                    else -> Triple(Color(0xFF6EE7B7), 0.36f, 5.5.dp)
                }
                val pw = widthDp.toPx()
                val ph = (size.height * heightFrac).coerceAtLeast(sleeveH + 4.dp.toPx())
                val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                if (leftX - pw >= 4.dp.toPx()) {
                    drawRoundRect(color, Offset(leftX - pw, cy - ph / 2f), Size(pw, ph), radius)
                    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(leftX - pw + 1.dp.toPx(), cy - ph / 2f + 2.dp.toPx()), Size(pw * 0.35f, ph - 4.dp.toPx()), radius)
                    leftX -= (pw + gap)
                }
                if (rightX + pw <= size.width - 4.dp.toPx()) {
                    drawRoundRect(color, Offset(rightX, cy - ph / 2f), Size(pw, ph), radius)
                    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(rightX + 1.dp.toPx(), cy - ph / 2f + 2.dp.toPx()), Size(pw * 0.35f, ph - 4.dp.toPx()), radius)
                    rightX += (pw + gap)
                }
            }
        }
    }
}

@Composable
internal fun RestCountdownRing(
    remainingSeconds: Long,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val safeTotal = totalSeconds.coerceAtLeast(1)
    val progress = (remainingSeconds.toFloat() / safeTotal).coerceIn(0f, 1f)
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formatted = if (mins > 0) "%d:%02d".format(java.util.Locale.ROOT, mins, secs) else stringResource(R.string.seconds_short, number(secs.toDouble(), 0))
    CircularProgressRing(
        progress = progress,
        valueText = formatted,
        labelText = stringResource(R.string.seconds_short, number(safeTotal.toDouble(), 0)),
        size = 86.dp,
        strokeWidth = 8.dp,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
        modifier = modifier,
    )
}

@Composable
internal fun MuscleVolumeBar(
    muscleLabelText: String,
    primarySets: Int,
    secondarySets: Int,
    maxSets: Int,
    detailText: String,
) {
    val denom = maxSets.coerceAtLeast(1).toFloat()
    val reduceMotion = rememberReduceMotion()
    val animation: androidx.compose.animation.core.AnimationSpec<Float> = if (reduceMotion) snap() else tween(500)
    val pFrac by animateFloatAsState((primarySets / denom).coerceIn(0f, 1f), animation, label = "muscle_p")
    val sFrac by animateFloatAsState((secondarySets / denom).coerceIn(0f, 1f), animation, label = "muscle_s")
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        // One spoken sentence for the row; the visible name and counts are split so long names never squeeze.
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = detailText },
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(muscleLabelText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.muscle_set_split, number(primarySets.toDouble(), 0), number(secondarySets.toDouble(), 0)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(Modifier.fillMaxSize()) {
                    if (pFrac > 0f) Box(Modifier.fillMaxWidth(pFrac).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    if (sFrac > 0f) {
                        val rem = (1f - pFrac).coerceAtLeast(0.01f)
                        Box(Modifier.fillMaxWidth((sFrac / rem).coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)))
                    }
                }
            }
        }
    }
}

@Composable
internal fun MacroDonutRing(
    protein: Double,
    carbs: Double,
    fat: Double,
    modifier: Modifier = Modifier,
) {
    val pCal = (protein * 4.0).coerceAtLeast(0.0)
    val cCal = (carbs * 4.0).coerceAtLeast(0.0)
    val fCal = (fat * 9.0).coerceAtLeast(0.0)
    val total = pCal + cCal + fCal
    if (total <= 0.0) return
    val pFrac = (pCal / total).toFloat()
    val cFrac = (cCal / total).toFloat()
    val fFrac = (fCal / total).toFloat()
    val pColor = MaterialTheme.colorScheme.secondary
    val cColor = MaterialTheme.colorScheme.tertiary
    val fColor = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerLowest
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Butt)
                val inset = 5.dp.toPx()
                val arcSize = Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx())
                drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
                var angle = -90f
                if (pFrac > 0f) {
                    drawArc(pColor, angle, pFrac * 360f, false, Offset(inset, inset), arcSize, style = stroke)
                    angle += pFrac * 360f
                }
                if (cFrac > 0f) {
                    drawArc(cColor, angle, cFrac * 360f, false, Offset(inset, inset), arcSize, style = stroke)
                    angle += cFrac * 360f
                }
                if (fFrac > 0f) {
                    drawArc(fColor, angle, fFrac * 360f, false, Offset(inset, inset), arcSize, style = stroke)
                }
            }
            // Percent only: the labelled pills beside the ring name each macro, and a word would not fit in Arabic.
            Text(number((pFrac * 100.0).toDouble(), 0) + "%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = pColor)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.macro_split_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PillBadge(stringResource(R.string.macro_share_protein, number((pFrac * 100.0).toDouble(), 0)), containerColor = MaterialTheme.colorScheme.accentContainer, contentColor = pColor)
                PillBadge(stringResource(R.string.macro_share_carbs, number((cFrac * 100.0).toDouble(), 0)), containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = cColor)
                PillBadge(stringResource(R.string.macro_share_fat, number((fFrac * 100.0).toDouble(), 0)), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = fColor)
            }
        }
    }
}

@Composable
internal fun LivePulseDot(
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val animatedPulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "pulse_alpha",
    )
    val pulse = if (reduceMotion) 1f else animatedPulse
    Box(modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color.copy(alpha = 0.25f * pulse)))
        Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = 0.7f + 0.3f * pulse)))
    }
}

@Composable
internal fun ActivityHeatmapGrid(
    today: LocalDate,
    countsByDate: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
) {
    val weeks = 12
    val totalDays = weeks * 7
    val start = today.minusDays((totalDays - 1).toLong())
    val rangeCounts = countsByDate.filterKeys { it in start..today }
    val description = stringResource(
        R.string.activity_heatmap_summary,
        number(rangeCounts.values.sum().toDouble(), 0),
        number(rangeCounts.values.count { it > 0 }.toDouble(), 0),
        dateLabel(start),
        dateLabel(today),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(weeks) { col ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(7) { row ->
                        val day = start.plusDays((col * 7 + row).toLong())
                        val count = countsByDate[day] ?: 0
                        val color = when {
                            count >= 2 -> MaterialTheme.colorScheme.primary
                            count == 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                // Today is marked by an outline only, so an empty day never looks like a recorded workout.
                                .then(
                                    if (day == today) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(4.dp))
                                    else Modifier,
                                ),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh to R.string.heatmap_legend_none,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.62f) to R.string.heatmap_legend_one,
                MaterialTheme.colorScheme.primary to R.string.heatmap_legend_multiple,
            ).forEach { (color, label) ->
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScopeInfoButton(title: Int, vararg notes: Int, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(
            Icons.Default.Info,
            contentDescription = stringResource(title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp),
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { res ->
                        Text(stringResource(res), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
internal fun FormFieldPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.25f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (largeText || maxWidth < 290.dp) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { first(); second() }
        else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { first() }
            Column(Modifier.weight(1f)) { second() }
        }
    }
}

/** Horizontal page padding shared by the main scroll column and edge-to-edge rows. */
internal val PageGutter = 18.dp

/**
 * Lets a horizontally scrolling row run under the page gutter to the screen edges, so chips scroll
 * off-screen instead of being cut at the padding. Pair with `.padding(horizontal = bleed)` after the scroll.
 */
internal fun Modifier.bleedToPageEdges(bleed: androidx.compose.ui.unit.Dp = PageGutter): Modifier =
    this.layout { measurable, constraints ->
        val extra = bleed.roundToPx()
        val placeable = measurable.measure(
            constraints.copy(minWidth = constraints.minWidth + 2 * extra, maxWidth = constraints.maxWidth + 2 * extra),
        )
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
    }

/**
 * True when the user turned animations off (Settings > Accessibility > Remove animations).
 * Looping and decorative motion must stop; state changes stay visible without animation.
 */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * A short label (tab, navigation item) that keeps every word whole at very large text sizes: if a word would be
 * cut or broken mid-word, the label steps down in size (to [minScale] at most) instead of truncating.
 */
@Composable
internal fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    minScale: Float = 0.7f,
) {
    val base = androidx.compose.material3.LocalTextStyle.current
    var scale by remember(text, base.fontSize) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val style = if (base.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) base.copy(fontSize = base.fontSize * scale, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified) else base
    Text(
        text,
        modifier,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = if (scale <= minScale) TextOverflow.Ellipsis else TextOverflow.Clip,
        style = style,
        onTextLayout = { result ->
            if (scale > minScale && (result.hasVisualOverflow || breaksInsideWord(result, text))) {
                scale = (scale - 0.08f).coerceAtLeast(minScale)
            }
        },
    )
}

private fun breaksInsideWord(result: androidx.compose.ui.text.TextLayoutResult, text: String): Boolean =
    (0 until result.lineCount - 1).any { line ->
        val end = result.getLineEnd(line, visibleEnd = false)
        end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
    }
