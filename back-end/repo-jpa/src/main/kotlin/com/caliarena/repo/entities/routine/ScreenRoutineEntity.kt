package com.caliarena.repo.entities.routine

import com.caliarena.domain.routine.ScreenRoutine
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "screen_routines")
class ScreenRoutineEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @Column(name = "tournament_id", nullable = false)
    var tournamentId: Int,
    @Column(name = "routine_id", nullable = false)
    var routineId: Int,
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,
    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = true,
    @Column(name = "label")
    var label: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long,
) {
    fun toDomain() =
        ScreenRoutine(
            id = id,
            tournamentId = tournamentId,
            routineId = routineId,
            displayOrder = displayOrder,
            isVisible = isVisible,
            label = label,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
