package com.caliarena.repo

import com.caliarena.RepositoryMatch
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchEvent
import com.caliarena.domain.match.MatchEventType
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.jpa.match.MatchEventRepositoryJpa
import com.caliarena.repo.jpa.match.MatchProgRepositoryJpa
import com.caliarena.repo.jpa.match.MatchRepositoryJpa
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MatchRepository(
    private val matchEventRepositoryJpa: MatchEventRepositoryJpa,
    private val matchProgRepositoryJpa: MatchProgRepositoryJpa,
    private val matchRepository: MatchRepositoryJpa,
) : RepositoryMatch {
    override fun createMatch(
        bracketId: Int,
        routineId: Int,
        athleteRedId: Int?,
        athleteBlueId: Int?,
        redFromMatchId: Int?,
        blueFromMatchId: Int?,
    ): Match {
        TODO("Not yet implemented")
    }

    override fun findByBracketId(bracketId: Int): List<Match> {
        TODO("Not yet implemented")
    }

    override fun findByStatus(status: MatchStatus): List<Match> {
        TODO("Not yet implemented")
    }

    override fun updateStatus(
        id: Int,
        status: MatchStatus,
    ): Match? {
        TODO("Not yet implemented")
    }

    override fun updateWinner(
        id: Int,
        winnerAthleteId: Int,
    ): Match? {
        TODO("Not yet implemented")
    }

    override fun createMatchProgress(matchId: Int): MatchProgress {
        TODO("Not yet implemented")
    }

    override fun findProgressByMatchId(matchId: Int): MatchProgress? {
        TODO("Not yet implemented")
    }

    override fun updateReps(
        matchId: Int,
        redReps: Int,
        blueReps: Int,
    ): MatchProgress? {
        TODO("Not yet implemented")
    }

    override fun updateTimer(
        matchId: Int,
        timerStartedAt: Instant?,
        timerRemainingSeconds: Int?,
    ): MatchProgress? {
        TODO("Not yet implemented")
    }

    override fun createEvent(
        matchId: Int,
        judgeId: Int,
        eventType: MatchEventType,
        payload: String?,
    ): MatchEvent {
        TODO("Not yet implemented")
    }

    override fun findEventsByMatchId(matchId: Int): List<MatchEvent> {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): Match? {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<Match> {
        TODO("Not yet implemented")
    }

    override fun save(entity: Match): Match? {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Int) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}
