package com.caliarena.http.model.match

data class CreateMatchInput(
    val bracketId: Int,
    val routineId: Int,
    val redFromMatchId: Int?,
    val blueFromMatchId: Int?,
)
