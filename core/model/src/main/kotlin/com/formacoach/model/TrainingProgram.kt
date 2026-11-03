package com.formacoach.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class TrainingProgram(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<Exercise>,
    val restSeconds: Int = 90,
) {
    init {
        require(id.isNotBlank() && id.length <= 80 && id !in listOf("A", "B"))
        require(name.isNotBlank() && name.length <= 120)
        require(exercises.isNotEmpty() && exercises.size <= 20 && exercises.distinct().size == exercises.size)
        require(restSeconds in 15..600)
    }
}
