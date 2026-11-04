package com.formacoach.coach

import com.formacoach.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

/** Exact, balanced loading using the user's available pairs. No load recommendation. */
object PlateCalculator {
    data class Result(val perSide:List<Double>)
    fun calculate(targetKg:Double,barKg:Double,pairs:Map<Double,Int>):Result? {
        fun grams(value:Double):Int {
            require(value.isFinite() && value in 0.0..1000.0)
            val scaled=value*1000
            require(abs(scaled-scaled.roundToInt())<0.0001)
            return scaled.roundToInt()
        }
        val target=grams(targetKg);val bar=grams(barKg)
        require(target>=bar && pairs.size<=10)
        val available=pairs.map {(kg,count)->
            require(count in 0..20)
            val mass=grams(kg);require(mass>=250 && mass%250==0);mass to count
        }.sortedByDescending {it.first}
        if((target-bar)%2!=0) return null
        val needed=(target-bar)/2
        if(needed%250!=0) return null
        // At most 200 physical pairs; bounded states and exact integer grams avoid drift.
        var states=mapOf(0 to emptyList<Int>())
        for((mass,count) in available) {
            val next=states.toMutableMap()
            for((total,plates) in states) for(n in 1..count) {
                val sum=total+mass*n
                if(sum>needed) break
                val candidate=plates+List(n){mass}
                if(next[sum]==null || candidate.size<next.getValue(sum).size) next[sum]=candidate
            }
            states=next
        }
        return states[needed]?.let {Result(it.map {mass->mass/1000.0})}
    }
}

/** Portable, canonical kg values; spreadsheet-sensitive user cells are neutralized. */
object TrainingCsv {
    fun export(data:FitnessSnapshot):String {
        fun cell(value:String):String {
            val safe=if(value.trimStart().firstOrNull() in listOf('=','+','-','@') || value.startsWith('\t') || value.startsWith('\r') || value.startsWith('\n')) "'"+value else value
            return "\""+safe.replace("\"","\"\"")+"\""
        }
        val sessions=data.sessions.filter {it.completedAt!=null}.associateBy {it.id}
        return buildString {
            append('\uFEFF')
            append("session_id,date,session_label,source,exercise_id,reps,weight_kg,set_kind,rpe\r\n")
            for(set in data.sets) {
                val s=sessions[set.sessionId]?:continue
                append(listOf(s.id,s.date.toString(),s.label,s.source.name,set.exercise.name,set.reps.toString(),set.weightKg.toString(),set.kind.name,set.rpe?.toString().orEmpty()).joinToString(",",transform=::cell));append("\r\n")
            }
        }
    }
}
