package com.caliarena

import com.caliarena.domain.routine.ScreenRoutine
import java.time.Instant

interface RepoScreenRoutine : Repository<ScreenRoutine> {
    fun findByTournamentId(tournamentId: Int): List<ScreenRoutine>

    fun create(tournamentId: Int, routineId: Int, displayOrder: Int, label: String?,
               now: Instant): ScreenRoutine

    fun update(id: Int, isVisible: Boolean?, displayOrder: Int?, label: String?, now: Instant): ScreenRoutine?
}