package com.caliarena.http.model.tournament

data class UpdateScreenInput(
    val screen: String,
    val currentMatchId: Int?,
    val currentBracketId: Int?,
    val gender: String?,
)
