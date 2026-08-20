package com.caliarena.domain.routine

data class ScreenRoutine(
    val id: Int,
    val tournamentId: Int,
    val routineId: Int,
    val displayOrder: Int,
    val isVisible: Boolean,
    val label: String?,
    val createdAt: Long,
    val updatedAt: Long,
)