@file:kotlinx.serialization.UseSerializers(com.formacoach.model.LocalDateSerializer::class)

package com.formacoach.wearables

import com.formacoach.model.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class WatchProgram(val id:String,val name:String,val exercises:List<Exercise>,val restSeconds:Int,val prescriptions:List<ExercisePrescription> = emptyList()) {
    init {
        require(id.isNotBlank() && id.length<=80 && name.isNotBlank() && name.length<=120)
        require(exercises.isNotEmpty() && exercises.size<=20 && exercises.distinct().size==exercises.size)
        require(restSeconds in 15..600)
        PlanDetails.validate(exercises,prescriptions)
    }
}

@Serializable
data class WatchSet(val id:String,val exercise:Exercise,val reps:Int,val weightKg:Double,val kind:SetKind=SetKind.WORKING,val rpe:Double?=null) {
    init {TrainingSet(id,"wire",exercise,reps,weightKg,kind,rpe)}
}

@Serializable
data class WatchWorkout(
    val id:String,val date:LocalDate,val programId:String,val programName:String,val exercises:List<Exercise>,
    val restSeconds:Int,val completedAtMillis:Long,val sets:List<WatchSet>,
    val averageHeartRateBpm:Double?=null,val activeEnergyKcal:Double?=null,val healthKitWorkoutId:String?=null,
    val prescriptions:List<ExercisePrescription> = emptyList(),
) {
    init {
        require(programName.isNotBlank() && programName.length<=120)
        require(date>=LocalDate.of(1970,1,1) && exercises.isNotEmpty())
        require(sets.isNotEmpty() && sets.size<=300 && sets.distinctBy {it.id}.size==sets.size)
        require(completedAtMillis>0)
        session()
    }
    fun session()=Session(id,date,programId,completedAtMillis,programName,exercises,restSeconds,
        SessionSource.APPLE_WATCH_IMPORT,averageHeartRateBpm,activeEnergyKcal,healthKitWorkoutId,prescriptions)
    fun trainingSets()=sets.map {TrainingSet(it.id,id,it.exercise,it.reps,it.weightKg,it.kind,it.rpe)}
}

@Serializable
data class WatchTransfer(
    val format:String="forma.workouts",val version:Int=2,val units:String="KG",val catalogVersion:Int=1,
    val packetId:String=UUID.randomUUID().toString(),val programs:List<WatchProgram> = emptyList(),val workouts:List<WatchWorkout> = emptyList(),
) {
    init {
        require(format=="forma.workouts" && version in 1..2 && units=="KG" && catalogVersion==ExerciseMuscles.CATALOG_VERSION)
        require(version!=1 || programs.all {it.prescriptions.isEmpty()} && workouts.all {it.prescriptions.isEmpty()})
        require(packetId.isNotBlank() && packetId.length<=80)
        require(programs.size<=20 && workouts.size<=100 && (programs.isNotEmpty() || workouts.isNotEmpty()))
        require(programs.distinctBy {it.id}.size==programs.size && workouts.distinctBy {it.id}.size==workouts.size)
        val sets=workouts.flatMap {it.sets}
        require(sets.size<=3000 && sets.distinctBy {it.id}.size==sets.size)
    }
}

object WatchTransferCodec {
    const val MAX_BYTES=1024*1024
    private val json=Json {encodeDefaults=true;ignoreUnknownKeys=false}
    fun encode(transfer:WatchTransfer):ByteArray {
        // Legacy v1 readers reject new fields, including empty arrays. Retain their exact shape.
        val value=json.encodeToJsonElement(transfer)
        val text=if(transfer.version==1) {
            val root=kotlinx.serialization.json.JsonObject(value.jsonObject.mapValues {(key,entry)->
                if(key=="programs"||key=="workouts") kotlinx.serialization.json.JsonArray(entry.jsonArray.map {row ->kotlinx.serialization.json.JsonObject(row.jsonObject.filterKeys {it!="prescriptions"})}) else entry
            });root.toString()
        } else value.toString()
        return text.toByteArray(Charsets.UTF_8).also {require(it.size<=MAX_BYTES)}
    }
    fun decode(bytes:ByteArray):WatchTransfer {
        require(bytes.size<=MAX_BYTES)
        val text=bytes.toString(Charsets.UTF_8)
        var depth=0;var quoted=false;var escaped=false
        text.forEach {c ->
            if(quoted) {if(escaped) escaped=false else if(c=='\\') escaped=true else if(c=='"') quoted=false}
            else when(c) {'"'->quoted=true;'[','{'->{depth++;require(depth<=24)};']','}'->depth--}
        }
        return json.decodeFromString(text)
    }
    fun read(input:InputStream):ByteArray {
        val result=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) {val n=input.read(buffer);if(n<0) break;if(n==0) continue;require(result.size().toLong()+n<=MAX_BYTES);result.write(buffer,0,n)}
        return result.toByteArray()
    }
}
