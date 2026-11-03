package com.formacoach.coach

import com.formacoach.model.FitnessSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

enum class ModelAvailability { NOT_CHECKED, UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, READY, ERROR }

interface LocalTextModel {
    suspend fun status():ModelAvailability
    suspend fun generate(prompt:String):String?
    fun download():Flow<ModelAvailability>
}

data class CoachFact(val id:String,val statement:String)

object LocalHighlights {
    fun facts(snapshot:FitnessSnapshot,review:WeeklyReview):List<CoachFact> = buildList {
        add(CoachFact("training","${review.completedSessions} completed sessions logged from ${review.start} through ${review.end}; user scheduling target ${snapshot.profile?.sessionsPerWeek}."))
        if(review.mealDays>0) add(CoachFact("food","Food entries on ${review.mealDays} of 7 days; mean LOGGED kcal per LOGGED day ${review.meanLoggedCalories}. This is not total intake or adherence."))
        if(review.meanWeight!=null) add(CoachFact("weight","${review.weightObservations} weight observations; mean ${review.meanWeight} kg. Weekly difference ${review.weightDelta ?: "unknown"}; causes are unknown."))
        add(CoachFact("coverage","Missing entries are unknown. No change in targets is inferred from this review."))
    }

    fun verify(raw:String?,allowed:Set<String>):List<String>? {
        if(raw==null || raw.length>256) return null
        val ids=runCatching {Json.decodeFromString<List<String>>(raw.trim())}.getOrNull()?:return null
        return ids.takeIf {it.size in 1..3 && it.distinct().size==it.size && it.all(allowed::contains)}
    }

    suspend fun select(model:LocalTextModel,facts:List<CoachFact>,isForeground:()->Boolean):List<String>? {
        return withTimeoutOrNull(15_000) {
            if(!isForeground() || model.status()!=ModelAvailability.READY) return@withTimeoutOrNull null
            val input=facts.joinToString("\n") {"${it.id}: ${it.statement}"}.take(3000)
            val prompt="Choose up to 3 most useful existing fact IDs from this list. Return ONLY a JSON array of IDs. Do not add advice, new facts, numbers or explanations.\n$input"
            val raw=model.generate(prompt)
            if(!isForeground()) null else verify(raw,facts.map {it.id}.toSet())
        }
    }
}
