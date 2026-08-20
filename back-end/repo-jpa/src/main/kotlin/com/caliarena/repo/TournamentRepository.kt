package com.caliarena.repo

import com.caliarena.RepositoryTournament
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentEntity.Companion.fromDomain
import com.caliarena.repo.entities.tournament.TournamentStateEntity
import com.caliarena.repo.jpa.match.MatchRepositoryJpa
import com.caliarena.repo.jpa.tournament.BracketRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentStateRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class TournamentRepository(
    private val bracketJpa: BracketRepositoryJpa,
    private val tournamentJpa: TournamentRepositoryJpa,
    private val tournamentStateJpa: TournamentStateRepositoryJpa,
    private val matchJpa: MatchRepositoryJpa,
) : RepositoryTournament {
    override fun createTournament(
        name: String,
        location: String?,
        startDate: Instant?,
        endDate: Instant?,
        createdAt: Instant,
    ): Tournament =
        tournamentJpa
            .save(
                TournamentEntity(
                    name = name,
                    location = location,
                    startDate = startDate?.epochSecond,
                    endDate = endDate?.epochSecond,
                    status = TournamentStatus.DRAFT,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()

    override fun findByStatus(status: TournamentStatus): List<Tournament> =
        tournamentJpa.findByStatus(status).map(TournamentEntity::toDomain)

    override fun findByName(name: String): Tournament? = tournamentJpa.findByName(name)?.toDomain()

    override fun updateStatus(
        id: Int,
        status: TournamentStatus,
    ): Tournament? {
        val entity = tournamentJpa.findByIdOrNull(id) ?: return null
        entity.status = status
        return tournamentJpa.save(entity).toDomain()
    }

    override fun createTournamentState(
        tournamentId: Int,
        updatedAt: Instant,
    ): TournamentState? {
        val tournament = tournamentJpa.findByIdOrNull(tournamentId) ?: return null
        return tournamentStateJpa
            .save(
                TournamentStateEntity(
                    tournament = tournament,
                    currentScreen = ScreenState.WAITING,
                    currentMatch = null,
                    updatedAt = updatedAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findStateByTournamentId(tournamentId: Int): TournamentState? =
        tournamentStateJpa.findByTournamentId(tournamentId)?.toDomain()

    override fun updateScreen(
        tournamentId: Int,
        screen: ScreenState,
        currentMatchId: Int?,
        updatedAt: Instant,
    ): TournamentState? {
        val entity = tournamentStateJpa.findByTournamentId(tournamentId) ?: return null
        entity.currentScreen = screen
        entity.updatedAt = updatedAt.epochSecond
        entity.currentMatch =
            currentMatchId?.let { matchId ->
                matchJpa.findByIdOrNull(matchId)
            }
        return tournamentStateJpa.save(entity).toDomain()
    }

    override fun createBracket(
        tournamentId: Int,
        gender: GenderType,
        stage: BracketStage,
        createdAt: Instant,
    ): Bracket? {
        val tournament = tournamentJpa.findByIdOrNull(tournamentId) ?: return null
        return bracketJpa
            .save(
                BracketEntity(
                    tournament = tournament,
                    gender = gender,
                    stage = stage,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findBracketsByTournamentId(tournamentId: Int): List<Bracket> =
        bracketJpa.findByTournamentId(tournamentId).map(BracketEntity::toDomain)

    override fun findBracketsByTournamentIdAndGender(
        tournamentId: Int,
        gender: GenderType,
    ): List<Bracket> = bracketJpa.findByTournamentIdAndGender(tournamentId, gender).map(BracketEntity::toDomain)

    override fun findById(id: Int): Tournament? = tournamentJpa.findByIdOrNull(id)?.toDomain()

    override fun findByBracketId(bracketId: Int): Bracket? = bracketJpa.findByIdOrNull(bracketId)?.toDomain()

    override fun findAll(): List<Tournament> = tournamentJpa.findAll().map(TournamentEntity::toDomain)

    override fun save(entity: Tournament): Tournament? = tournamentJpa.save(entity.fromDomain()).toDomain()

    override fun deleteById(id: Int) = tournamentJpa.deleteById(id)

    override fun clear() {
        bracketJpa.deleteAll()
        tournamentStateJpa.deleteAll()
        tournamentJpa.deleteAll()
    }
}
