package com.caliarena

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import java.time.Instant

interface RepositoryTournament : Repository<Tournament> {

    fun createTournament(
        name: String,
        location: String?,
        startDate: Instant?,
        endDate: Instant?,
        createdAt: Instant,
    ): Tournament

    fun findByStatus(status: TournamentStatus): List<Tournament>

    fun updateStatus(id: Int, status: TournamentStatus): Tournament?

    // TournamentState
    fun createTournamentState(tournamentId: Int, updatedAt: Instant): TournamentState?

    fun findStateByTournamentId(tournamentId: Int): TournamentState?

    fun updateScreen(tournamentId: Int, screen: ScreenState, currentMatchId: Int?, updatedAt: Instant): TournamentState?

    // Bracket
    fun createBracket(tournamentId: Int, gender: GenderType, stage: BracketStage, createdAt: Instant): Bracket?

    fun findByBracketId(bracketId: Int): Bracket?

    fun findBracketsByTournamentId(tournamentId: Int): List<Bracket>

    fun findBracketsByTournamentIdAndGender(tournamentId: Int, gender: GenderType): List<Bracket>
}