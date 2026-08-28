package com.caliarena.http.model.tournament

data class CreateBracketInput(
    val tournamentId: Int,
    val division: String,
    val stage: String,
)
