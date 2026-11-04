package com.formacoach.model

import kotlinx.serialization.Serializable

/** Optional targets supplied by the user, never generated training recommendations. */
@Serializable
data class ExercisePrescription(
    val exercise:Exercise,
    val workingSets:Int?=null,
    val repMin:Int?=null,
    val repMax:Int?=null,
    val restSeconds:Int?=null,
    val supersetGroup:Int?=null,
    val notes:String="",
) {
    init {
        require(workingSets==null || workingSets in 1..20)
        require((repMin==null)==(repMax==null))
        require(repMin==null || repMin in 1..1000 && repMax!! in repMin..1000)
        require(restSeconds==null || restSeconds in 15..600)
        require(supersetGroup==null || supersetGroup in 1..10)
        require(notes.length<=1000)
    }
}

object PlanDetails {
    fun validate(order:List<Exercise>,details:List<ExercisePrescription>) {
        require(details.size<=20 && details.distinctBy {it.exercise}.size==details.size)
        require(details.all {it.exercise in order})
        details.filter {it.supersetGroup!=null}.groupBy {it.supersetGroup}.values.forEach {group ->
            val positions=group.map {order.indexOf(it.exercise)}.sorted()
            require(positions.size>=2 && positions.last()-positions.first()+1==positions.size)
        }
    }
}
