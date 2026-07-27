package com.caliarena.http.model.athlete

data class UpdateAthleteInput(
    val name: String,
    val gender: String,
    val clubId: Int,
)
