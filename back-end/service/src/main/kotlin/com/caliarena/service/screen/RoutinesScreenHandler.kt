package com.caliarena.service.screen

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.repo.trx.Transaction
import com.caliarena.service.mapper.ScreenRoutinesMapper
import jakarta.inject.Named

@Named
class RoutinesScreenHandler(
    private val mapper: ScreenRoutinesMapper,
) : ScreenEffectHandler {
    override fun supports(screen: ScreenState) = screen == ScreenState.ROUTINES

    override fun handle(
        trx: Transaction,
        tournamentId: Int,
        state: TournamentState,
    ): ScreenEffect = ScreenEffect.Routines(mapper.build(trx, tournamentId))
}
