package com.caliarena.http.model.tournament

data class CreateBracketInput(
    val tournamentId: Int,
    val gender: String,
    val stage: String,
)
