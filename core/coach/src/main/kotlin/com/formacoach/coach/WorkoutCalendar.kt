package com.formacoach.coach

import com.formacoach.model.*
import java.time.*
import java.text.Normalizer
import java.util.Locale

object WorkoutCalendar {
    data class Day(val date:LocalDate,val completed:Int)
    fun month(data:FitnessSnapshot,month:YearMonth,today:LocalDate):List<Day> {
        val counts=data.sessions.filter {it.completedAt!=null && it.date<=today}.groupingBy {it.date}.eachCount()
        return (1..month.lengthOfMonth()).map {day ->val date=month.atDay(day);Day(date,counts[date]?:0)}
    }
}

object ExerciseSearch {
    private fun normalize(value:String):String=buildString {
        Normalizer.normalize(value,Normalizer.Form.NFKD).forEach {c ->
            if(c=='_' || c=='-') append(' ')
            else if(Character.getType(c)!=Character.NON_SPACING_MARK.toInt() && c!='\u0640') append(c)
        }
    }.lowercase(Locale.ROOT)
    fun matches(query:String,text:String):Boolean {
        val normalized=normalize(text)
        return normalize(query).split(' ').filter {it.isNotBlank()}.all {it in normalized}
    }
}
