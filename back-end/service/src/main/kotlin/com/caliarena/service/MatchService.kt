package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchEvent
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

sealed class MatchError {
    data object MatchNotFound : MatchError()

    data object BracketNotFound : MatchError()

    data object RoutineNotFound : MatchError()

    data object AthleteNotInMatch : MatchError()

    data object InvalidStatusTransition : MatchError()

    data object MatchNotRunning : MatchError()

    data object ProgressNotFound : MatchError()

    data object ProgressAlreadyExists : MatchError()
}

@Service
class MatchService(
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {
    fun createMatch(
        bracketId: Int,
        routineId: Int,
        redFromMatchId: Int?,
        blueFromMatchId: Int?,
    ): Either<MatchError, Match> =
        trxManager.run {
            repoTournament.findByBracketId(bracketId)
                ?: return@run failure(MatchError.BracketNotFound)

            repoEnduranceRoutine.findById(routineId)
                ?: return@run failure(MatchError.RoutineNotFound)

            val match =
                repoMatch.createMatch(
                    bracketId = bracketId,
                    routineId = routineId,
                    redFromMatchId = redFromMatchId,
                    blueFromMatchId = blueFromMatchId,
                    createdAt = clock.instant(),
                ) ?: return@run failure(MatchError.BracketNotFound)

            success(match)
        }

    fun getMatchById(id: Int): Either<MatchError, Match> =
        trxManager.run {
            val match =
                repoMatch.findById(id)
                    ?: return@run failure(MatchError.MatchNotFound)

            success(match)
        }

    fun getMatchesByBracket(bracketId: Int): Either<MatchError, List<Match>> =
        trxManager.run {
            repoTournament.findByBracketId(bracketId)
                ?: return@run failure(MatchError.BracketNotFound)

            success(repoMatch.findByBracketId(bracketId))
        }

    fun updateMatchStatus(
        id: Int,
        newStatus: MatchStatus,
    ): Either<MatchError, Match> =
        trxManager.run {
            val match =
                repoMatch.findById(id)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (!isValidStatusTransition(match.status, newStatus)) {
                return@run failure(MatchError.InvalidStatusTransition)
            }

            val updated =
                repoMatch.updateStatus(id, newStatus)
                    ?: return@run failure(MatchError.MatchNotFound)

            success(updated)
        }

    fun setMatchWinner(
        id: Int,
        winnerAthleteId: Int,
    ): Either<MatchError, Match> =
        trxManager.run {
            val match =
                repoMatch.findById(id)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.athleteRedId != winnerAthleteId && match.athleteBlueId != winnerAthleteId) {
                return@run failure(MatchError.AthleteNotInMatch)
            }

            val updated =
                repoMatch.updateWinner(id, winnerAthleteId)
                    ?: return@run failure(MatchError.MatchNotFound)

            success(updated)
        }

    fun initMatchProgress(matchId: Int): Either<MatchError, MatchProgress> =
        trxManager.run {
            repoMatch.findById(matchId)
                ?: return@run failure(MatchError.MatchNotFound)

            if (repoMatch.findProgressByMatchId(matchId) != null) {
                return@run failure(MatchError.ProgressAlreadyExists)
            }

            val progress =
                repoMatch.createMatchProgress(
                    matchId = matchId,
                    updatedAt = clock.instant(),
                ) ?: return@run failure(MatchError.MatchNotFound)

            success(progress)
        }

    fun getMatchProgress(matchId: Int): Either<MatchError, MatchProgress> =
        trxManager.run {
            repoMatch.findById(matchId)
                ?: return@run failure(MatchError.MatchNotFound)

            val progress =
                repoMatch.findProgressByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            success(progress)
        }

    fun updateReps(
        matchId: Int,
        redReps: Int,
        blueReps: Int,
    ): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                repoMatch.findById(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(MatchError.MatchNotRunning)
            }

            val updated =
                repoMatch.updateReps(
                    matchId = matchId,
                    redReps = redReps,
                    blueReps = blueReps,
                    updatedAt = clock.instant(),
                ) ?: return@run failure(MatchError.ProgressNotFound)

            success(updated)
        }

    fun updateTimer(
        matchId: Int,
        timerStartedAt: Instant?,
        timerRemainingSeconds: Int?,
    ): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                repoMatch.findById(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(MatchError.MatchNotRunning)
            }

            val updated =
                repoMatch.updateTimer(
                    matchId = matchId,
                    timerStartedAt = timerStartedAt,
                    timerRemainingSeconds = timerRemainingSeconds,
                    updatedAt = clock.instant(),
                ) ?: return@run failure(MatchError.ProgressNotFound)

            success(updated)
        }

    fun getMatchEvents(matchId: Int): Either<MatchError, List<MatchEvent>> =
        trxManager.run {
            repoMatch.findById(matchId)
                ?: return@run failure(MatchError.MatchNotFound)

            success(repoMatch.findEventsByMatchId(matchId))
        }

    private fun isValidStatusTransition(
        current: MatchStatus,
        next: MatchStatus,
    ): Boolean =
        when (current) {
            MatchStatus.PENDING -> next == MatchStatus.READY
            MatchStatus.READY -> next == MatchStatus.RUNNING
            MatchStatus.RUNNING -> next == MatchStatus.PAUSED || next == MatchStatus.FINISHED
            MatchStatus.PAUSED -> next == MatchStatus.RUNNING
            MatchStatus.FINISHED -> false
        }
}
