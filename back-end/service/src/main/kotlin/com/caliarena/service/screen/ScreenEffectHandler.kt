package com.caliarena.service.screen

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.repo.trx.Transaction

interface ScreenEffectHandler {
    fun supports(screen: ScreenState): Boolean

    fun handle(
        trx: Transaction,
        tournamentId: Int,
        state: TournamentState,
    ): ScreenEffect
}
