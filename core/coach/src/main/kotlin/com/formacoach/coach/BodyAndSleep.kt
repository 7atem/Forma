package com.formacoach.coach

import com.formacoach.model.ImportedSleep
import java.time.Instant
import java.time.ZoneId

/** BMI from a logged weight and the height the user entered. A population screening number only (mirrors Swift). */
object BodyMetrics {
    fun bmi(weightKg: Double, heightCm: Double): Double? {
        if (!weightKg.isFinite() || !heightCm.isFinite() || weightKg <= 0 || weightKg > 1000 || heightCm !in 50.0..272.0) return null
        val meters = heightCm / 100
        return weightKg / (meters * meters)
    }
}

enum class SleepStage { IN_BED, AWAKE, ASLEEP, CORE, DEEP, REM }

/** One recorded sleep segment read from a health app. */
data class SleepSegment(val start: Instant, val end: Instant, val stage: SleepStage, val source: String)

object SleepNights {
    private val asleep = setOf(SleepStage.ASLEEP, SleepStage.CORE, SleepStage.DEEP, SleepStage.REM)

    /**
     * Groups segments by local wake-up date and keeps one source per night (the one with the most recorded sleep),
     * so two devices recording the same night are never added together. Mirrors FormaCore's SleepNights.
     */
    fun summarize(segments: List<SleepSegment>, zone: ZoneId): List<ImportedSleep> =
        segments.filter { it.end.isAfter(it.start) && it.end.epochSecond - it.start.epochSecond <= 24 * 3600 }
            .groupBy { it.end.atZone(zone).toLocalDate() }
            .mapNotNull { (date, items) ->
                fun minutes(list: List<SleepSegment>, stages: Set<SleepStage>) =
                    list.filter { it.stage in stages }.sumOf { (it.end.toEpochMilli() - it.start.toEpochMilli()) / 60_000.0 }
                val (source, chosen) = items.groupBy { it.source }.maxByOrNull { minutes(it.value, asleep) } ?: return@mapNotNull null
                val total = minutes(chosen, asleep)
                if (total <= 0) return@mapNotNull null
                fun optional(stage: SleepStage) = minutes(chosen, setOf(stage)).takeIf { it > 0 }
                ImportedSleep(
                    date = date,
                    asleepMinutes = total,
                    deepMinutes = optional(SleepStage.DEEP),
                    remMinutes = optional(SleepStage.REM),
                    lightMinutes = optional(SleepStage.CORE),
                    awakeMinutes = optional(SleepStage.AWAKE),
                    source = source.take(200),
                )
            }
            .sortedBy { it.date }
}
