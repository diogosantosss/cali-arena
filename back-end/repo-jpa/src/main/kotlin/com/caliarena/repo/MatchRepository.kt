package com.caliarena.repo

import com.caliarena.RepositoryMatch
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchEvent
import com.caliarena.domain.match.MatchEventType
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchEntity.Companion.fromDomain
import com.caliarena.repo.entities.match.MatchEventEntity
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.jpa.match.MatchEventRepositoryJpa
import com.caliarena.repo.jpa.match.MatchProgRepositoryJpa
import com.caliarena.repo.jpa.match.MatchRepositoryJpa
import com.caliarena.repo.jpa.tournament.BracketRepositoryJpa
import com.caliarena.repo.jpa.user.UserRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MatchRepository(
    private val matchEventJpa: MatchEventRepositoryJpa,
    private val matchProgJpa: MatchProgRepositoryJpa,
    private val matchJpa: MatchRepositoryJpa,
    private val bracketJpa: BracketRepositoryJpa,
    private val userJpa: UserRepositoryJpa,
) : RepositoryMatch {
    override fun createMatch(
        bracketId: Int,
        routineId: Int,
        redFromMatchId: Int?,
        blueFromMatchId: Int?,
        createdAt: Instant,
    ): Match? {
        val bracket = bracketJpa.findByIdOrNull(bracketId) ?: return null
        return matchJpa
            .save(
                MatchEntity(
                    bracket = bracket,
                    routineId = routineId,
                    redFromMatchId = redFromMatchId,
                    blueFromMatchId = blueFromMatchId,
                    status = MatchStatus.PENDING,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findByBracketId(bracketId: Int): List<Match> = matchJpa.findByBracketId(bracketId).map(MatchEntity::toDomain)

    override fun findByStatus(status: MatchStatus): List<Match> = matchJpa.findByStatus(status).map(MatchEntity::toDomain)

    override fun updateStatus(
        id: Int,
        status: MatchStatus,
    ): Match? {
        val entity = matchJpa.findByIdOrNull(id) ?: return null
        entity.status = status
        return matchJpa.save(entity).toDomain()
    }

    override fun updateWinner(
        id: Int,
        winnerAthleteId: Int,
    ): Match? {
        val entity = matchJpa.findByIdOrNull(id) ?: return null
        entity.winnerAthlete =
            when (winnerAthleteId) {
                entity.athleteRed?.id -> entity.athleteRed
                entity.athleteBlue?.id -> entity.athleteBlue
                else -> return null
            }
        return matchJpa.save(entity).toDomain()
    }

    override fun createMatchProgress(
        matchId: Int,
        updatedAt: Instant,
    ): MatchProgress? {
        val match = matchJpa.findByIdOrNull(matchId) ?: return null
        return matchProgJpa
            .save(
                MatchProgressEntity(
                    match = match,
                    redCurrentReps = 0,
                    blueCurrentReps = 0,
                    updatedAt = updatedAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findProgressByMatchId(matchId: Int): MatchProgress? = matchProgJpa.findByMatchId(matchId)?.toDomain()

    override fun updateReps(
        matchId: Int,
        redReps: Int,
        blueReps: Int,
        updatedAt: Instant,
    ): MatchProgress? {
        val entity = matchProgJpa.findByMatchId(matchId) ?: return null
        entity.redCurrentReps = redReps
        entity.blueCurrentReps = blueReps
        entity.updatedAt = updatedAt.epochSecond
        return matchProgJpa.save(entity).toDomain()
    }

    override fun updateTimer(
        matchId: Int,
        timerStartedAt: Instant?,
        timerRemainingSeconds: Int?,
        updatedAt: Instant,
    ): MatchProgress? {
        val entity = matchProgJpa.findByMatchId(matchId) ?: return null
        entity.timerStartedAt = timerStartedAt?.epochSecond
        entity.timerRemainingSeconds = timerRemainingSeconds
        entity.updatedAt = updatedAt.epochSecond
        return matchProgJpa.save(entity).toDomain()
    }

    override fun createEvent(
        matchId: Int,
        judgeId: Int,
        eventType: MatchEventType,
        payload: String?,
        createdAt: Instant,
    ): MatchEvent? {
        val match = matchJpa.findByIdOrNull(matchId) ?: return null
        val judge = userJpa.findByIdOrNull(judgeId) ?: return null
        return matchEventJpa
            .save(
                MatchEventEntity(
                    match = match,
                    judge = judge,
                    eventType = eventType,
                    payload = payload,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findEventsByMatchId(matchId: Int): List<MatchEvent> = matchEventJpa.findByMatchId(matchId).map(MatchEventEntity::toDomain)

    override fun findById(id: Int): Match? = matchJpa.findByIdOrNull(id)?.toDomain()

    override fun findAll(): List<Match> = matchJpa.findAll().map(MatchEntity::toDomain)

    override fun save(entity: Match): Match? {
        val bracket = bracketJpa.findByIdOrNull(entity.bracketId) ?: return null
        return matchJpa.save(entity.fromDomain(bracket, null, null, null)).toDomain()
    }

    override fun deleteById(id: Int) = matchJpa.deleteById(id)

    override fun clear() {
        matchEventJpa.deleteAll()
        matchProgJpa.deleteAll()
        matchJpa.deleteAll()
    }
}
