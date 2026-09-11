package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class AthleteOutput(
    val id: Int,
    val name: String,
    val gender: AthleteGender,
    val clubId: Int,
    val createdAt: String,
)

@Serializable
enum class AthleteGender {
    MALE,
    FEMALE,
}
