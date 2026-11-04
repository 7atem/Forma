package com.formacoach.coach

import com.formacoach.model.*

object PlanEditing {
    /** Reordering/removal retains only adjacent portions of the user's existing groups. */
    fun normalize(order:List<Exercise>,details:List<ExercisePrescription>):List<ExercisePrescription> {
        val byExercise=details.associateBy {it.exercise}
        val edges=order.indices.drop(1).filter {i ->
            val group=byExercise[order[i]]?.supersetGroup
            group!=null && group==byExercise[order[i-1]]?.supersetGroup
        }.toSet()
        return fromEdges(order,byExercise,edges)
    }
    fun linkPrevious(order:List<Exercise>,details:List<ExercisePrescription>,exercise:Exercise,linked:Boolean):List<ExercisePrescription> {
        val index=order.indexOf(exercise);require(index>0)
        val byExercise=details.associateBy {it.exercise}
        val edges=order.indices.drop(1).filter {i ->
            val group=byExercise[order[i]]?.supersetGroup
            group!=null && group==byExercise[order[i-1]]?.supersetGroup
        }.toMutableSet()
        if(linked) edges.add(index) else edges.remove(index)
        return fromEdges(order,byExercise,edges)
    }
    private fun fromEdges(order:List<Exercise>,details:Map<Exercise,ExercisePrescription>,edges:Set<Int>):List<ExercisePrescription> {
        val groups=mutableMapOf<Exercise,Int>();var group=0
        for(i in order.indices.drop(1)) if(i in edges) {
            if(i-1 !in edges) group++
            groups[order[i-1]]=group;groups[order[i]]=group
        }
        return order.mapNotNull {exercise ->
            (details[exercise]?:groups[exercise]?.let {ExercisePrescription(exercise)})?.copy(supersetGroup=groups[exercise])
        }.also {PlanDetails.validate(order,it)}
    }
}

object WorkoutFlow {
    data class Next(val exercise:Exercise?,val restSeconds:Int,val withinGroup:Boolean=false)
    fun counts(session:Session,sets:List<TrainingSet>)=sets.filter {it.sessionId==session.id && it.kind==SetKind.WORKING}.groupingBy {it.exercise}.eachCount()
    fun afterSet(session:Session,sets:List<TrainingSet>,current:Exercise,kind:SetKind):Next {
        val details=session.prescriptions.associateBy {it.exercise}
        val spec=details[current]
        val rest=spec?.restSeconds?:session.restSeconds
        if(kind==SetKind.WARM_UP || current !in session.plannedExercises) return Next(current,rest)
        val counts=counts(session,sets)
        fun unfinished(exercise:Exercise)=details[exercise]?.workingSets?.let {(counts[exercise]?:0)<it}?:true
        val group=spec?.supersetGroup
        if(group!=null) {
            val members=session.plannedExercises.filter {details[it]?.supersetGroup==group}
            val index=members.indexOf(current)
            for(offset in 1..members.size) {
                val nextIndex=(index+offset)%members.size
                val next=members[nextIndex]
                if(unfinished(next)) return Next(next,if(nextIndex>index) 0 else rest,true)
            }
        } else if(unfinished(current)) return Next(current,rest)
        val last=if(group==null) session.plannedExercises.indexOf(current) else session.plannedExercises.indexOfLast {details[it]?.supersetGroup==group}
        val next=session.plannedExercises.drop(last+1).firstOrNull(::unfinished)
        return Next(next,if(next==null) 0 else rest)
    }
}
