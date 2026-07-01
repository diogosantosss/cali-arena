package com.caliarena.repo

import com.caliarena.RepositoryTournament
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.jpa.tournament.BracketRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentStateRepositoryJpa
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class TournamentRepository(
    private val bracketRepositoryJpa: BracketRepositoryJpa,
    private val tournamentRepositoryJpa: TournamentRepositoryJpa,
    private val tournamentStateRepositoryJpa: TournamentStateRepositoryJpa,
) : RepositoryTournament {
    override fun createTournament(
        name: String,
        location: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Tournament {
        TODO("Not yet implemented")
    }

    override fun findByStatus(status: TournamentStatus): List<Tournament> {
        TODO("Not yet implemented")
    }

    override fun updateStatus(
        id: Int,
        status: TournamentStatus,
    ): Tournament? {
        TODO("Not yet implemented")
    }

    override fun createTournamentState(tournamentId: Int): TournamentState {
        TODO("Not yet implemented")
    }

    override fun findStateByTournamentId(tournamentId: Int): TournamentState? {
        TODO("Not yet implemented")
    }

    override fun updateScreen(
        tournamentId: Int,
        screen: ScreenState,
        currentMatchId: Int?,
    ): TournamentState? {
        TODO("Not yet implemented")
    }

    override fun createBracket(
        tournamentId: Int,
        gender: GenderType,
        stage: BracketStage,
    ): Bracket {
        TODO("Not yet implemented")
    }

    override fun findBracketsByTournamentId(tournamentId: Int): List<Bracket> {
        TODO("Not yet implemented")
    }

    override fun findBracketsByTournamentIdAndGender(
        tournamentId: Int,
        gender: GenderType,
    ): List<Bracket> {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): Tournament? {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<Tournament> {
        TODO("Not yet implemented")
    }

    override fun save(entity: Tournament): Tournament? {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}
