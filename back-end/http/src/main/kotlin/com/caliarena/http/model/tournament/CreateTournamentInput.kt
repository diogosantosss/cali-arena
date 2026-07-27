package com.caliarena.http.model.tournament

data class CreateTournamentInput(
    val name: String,
    val location: String?,
    val startDate: String?,
    val endDate: String?,
)
