package com.caliarena.domain.routine

import java.math.BigDecimal

data class Exercise(
    val id: Int,
    val routineId: Int,
    val name: String,
    val targetReps: Int,
    val addedWeight: BigDecimal?,
    val exerciseOrder: Int,
    val supersetOrder: Int?,
    val type: ExerciseType,
)
