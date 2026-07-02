package com.caliarena

import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchEvent
import com.caliarena.domain.match.MatchEventType
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import java.time.Instant

interface RepositoryMatch : Repository<Match> {

    fun createMatch(
        bracketId: Int,
        routineId: Int,
        redFromMatchId: Int?,
        blueFromMatchId: Int?,
        createdAt: Instant,
    ): Match?

    fun findByBracketId(bracketId: Int): List<Match>

    fun findByStatus(status: MatchStatus): List<Match>

    fun updateStatus(id: Int, status: MatchStatus): Match?

    fun updateWinner(id: Int, winnerAthleteId: Int): Match?

    // MatchProgress
    fun createMatchProgress(matchId: Int, updatedAt: Instant): MatchProgress?

    fun findProgressByMatchId(matchId: Int): MatchProgress?

    fun updateReps(matchId: Int, redReps: Int, blueReps: Int,
                   updatedAt: Instant): MatchProgress?

    fun updateTimer(matchId: Int, timerStartedAt: Instant?, timerRemainingSeconds: Int?,
                    updatedAt: Instant): MatchProgress?

    // MatchEvent
    fun createEvent(
        matchId: Int,
        judgeId: Int,
        eventType: MatchEventType,
        payload: String?,
        createdAt: Instant
    ): MatchEvent?

    fun findEventsByMatchId(matchId: Int): List<MatchEvent>
}