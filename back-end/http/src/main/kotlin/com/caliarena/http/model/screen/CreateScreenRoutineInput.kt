package com.caliarena.http.model.screen

data class CreateScreenRoutineInput(
    val routineId: Int,
    val displayOrder: Int,
    val label: String? = null,
)
