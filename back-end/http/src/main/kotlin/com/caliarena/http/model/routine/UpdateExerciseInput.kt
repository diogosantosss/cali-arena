package com.caliarena.http.model.routine

import java.math.BigDecimal

data class UpdateExerciseInput(
    val name: String,
    val targetReps: Int,
    val addedWeight: BigDecimal?,
    val exerciseOrder: Int,
    val supersetOrder: Int?,
    val type: String,
)
