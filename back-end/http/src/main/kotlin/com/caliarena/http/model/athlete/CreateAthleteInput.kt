package com.caliarena.http.model.athlete

data class CreateAthleteInput(
    val name: String,
    val gender: String,
    val clubId: Int,
)
