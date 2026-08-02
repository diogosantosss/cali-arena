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
        judgeId: Int,
        athleteRed: Int,
        athleteBlue: Int,
        createdAt: Instant,
    ): Match?

    fun findByBracketId(bracketId: Int): List<Match>

    fun findByStatus(status: MatchStatus): List<Match>

    // MatchProgress
    fun createMatchProgress(matchId: Int, updatedAt: Instant): MatchProgress?

    fun findProgressByMatchId(matchId: Int): MatchProgress?

    fun updateMatchProgress(
        progress: MatchProgress,
        redCurrentExerciseId: Int? = null,
        blueCurrentExerciseId: Int? = null
    ): MatchProgress?
}