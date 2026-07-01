package pt.isel.repo

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
    ): Tournament

    fun findByStatus(status: TournamentStatus): List<Tournament>

    fun updateStatus(id: Int, status: TournamentStatus): Tournament?

    // TournamentState
    fun createTournamentState(tournamentId: Int): TournamentState

    fun findStateByTournamentId(tournamentId: Int): TournamentState?

    fun updateScreen(tournamentId: Int, screen: ScreenState, currentMatchId: Int?): TournamentState?
}