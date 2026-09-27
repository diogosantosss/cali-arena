package com.caliarena.service.screen

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.repo.trx.Transaction
import com.caliarena.service.mapper.BracketSummaryMapper
import jakarta.inject.Named

@Named
class BracketsScreenHandler(
    private val mapper: BracketSummaryMapper,
) : ScreenEffectHandler {
    override fun supports(screen: ScreenState) = screen == ScreenState.BRACKETS

    override fun handle(
        trx: Transaction,
        tournamentId: Int,
        state: TournamentState,
    ): ScreenEffect {
        val division = state.currentDivision ?: return ScreenEffect.None
        return ScreenEffect.Brackets(mapper.build(trx, tournamentId, division))
    }
}
