package com.caliarena.http.model.match

data class CreateMatchInput(
    val bracketId: Int,
    val routineId: Int,
    val judgeId: Int,
    val athleteRedId: Int,
    val athleteBlueId: Int,
)
