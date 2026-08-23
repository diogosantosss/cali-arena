package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.RepSide
import com.caliarena.domain.routine.Exercise
import com.caliarena.service.sse.MatchUpdatedEvent
import com.caliarena.service.sse.SpectatorPublisher
import org.springframework.stereotype.Service
import java.time.Clock

sealed class MatchError {
    data object MatchNotFound : MatchError()

    data object BracketNotFound : MatchError()

    data object RoutineNotFound : MatchError()

    data object AthleteNotInMatch : MatchError()

    data object MatchNotRunning : MatchError()

    data object ProgressNotFound : MatchError()

    data object ProgressAlreadyExists : MatchError()

    data object AthleteNotFound : MatchError()

    data object AthletesNotAssigned : MatchError()

    data object JudgeNotFound : MatchError()

    data object SameAthleteOnBothSides : MatchError()

    data object ErrorCreatingMatchProg : MatchError()

    data object ExerciseNotFound : MatchError()

    data object MatchAlreadyStarted : MatchError()

    data object OpponentNotFinished : MatchError()
}

@Service
class MatchService(
    private val trxManager: TransactionManager,
    private val clock: Clock,
    private val publisher: SpectatorPublisher,
) {
    fun createMatch(
        bracketId: Int,
        routineId: Int,
        judgeId: Int,
        athleteRedId: Int,
        athleteBlueId: Int,
    ): Either<MatchError, Match> =
        trxManager.run {
            repoTournament.findByBracketId(bracketId)
                ?: return@run failure(MatchError.BracketNotFound)

            repoEnduranceRoutine.findById(routineId)
                ?: return@run failure(MatchError.RoutineNotFound)

            repoUser.findById(judgeId)
                ?: return@run failure(MatchError.JudgeNotFound)

            repoAthlete.findById(athleteRedId)
                ?: return@run failure(MatchError.AthleteNotFound)

            repoAthlete.findById(athleteBlueId)
                ?: return@run failure(MatchError.AthleteNotFound)

            val match =
                repoMatch.createMatch(
                    bracketId = bracketId,
                    routineId = routineId,
                    judgeId = judgeId,
                    athleteRed = athleteRedId,
                    athleteBlue = athleteBlueId,
                    createdAt = clock.instant(),
                ) ?: return@run failure(MatchError.BracketNotFound)

            success(match)
        }

    fun startMatch(matchId: Int): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                repoMatch.findById(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (repoMatch.findProgressByMatchId(matchId) != null) {
                return@run failure(MatchError.ProgressAlreadyExists)
            }

            if (match.athleteBlueId == null || match.athleteRedId == null) {
                return@run failure(MatchError.AthletesNotAssigned)
            }

            if (match.status == MatchStatus.RUNNING || match.status == MatchStatus.FINISHED) {
                return@run failure(MatchError.MatchAlreadyStarted)
            }

            val firstExercise: Exercise =
                repoEnduranceRoutine
                    .findExercisesByRoutineId(match.routineId)
                    .minWithOrNull(compareBy(Exercise::exerciseOrder).thenBy { it.supersetOrder ?: 0 })
                    ?: return@run failure(MatchError.RoutineNotFound)

            val now = clock.instant()

            repoMatch.save(
                match.copy(
                    status = MatchStatus.RUNNING,
                    startedAt = now,
                ),
            ) ?: return@run failure(MatchError.MatchNotFound)

            val progress =
                repoMatch.createMatchProgress(
                    matchId = matchId,
                    firstExerciseId = firstExercise.id,
                    now = now,
                ) ?: return@run failure(MatchError.ErrorCreatingMatchProg)

            val tournamentId =
                repoTournament.findByBracketId(match.bracketId)?.tournamentId
                    ?: return@run failure(MatchError.BracketNotFound)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = progress,
            ).let { publisher.publish(it) }

            success(progress)
        }

    fun updateAthletesReps(
        matchId: Int,
        redReps: Int? = null,
        blueReps: Int? = null,
    ): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                repoMatch.findById(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(MatchError.MatchNotRunning)
            }

            val prog =
                repoMatch.findProgressByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            val exercises = repoEnduranceRoutine.findExercisesByRoutineId(match.routineId)

            if (redReps != null && prog.redFinishedAt == null && exercises.none { it.id == prog.redCurrentExerciseId }) {
                return@run failure(MatchError.ExerciseNotFound)
            }
            if (blueReps != null && prog.blueFinishedAt == null && exercises.none { it.id == prog.blueCurrentExerciseId }) {
                return@run failure(MatchError.ExerciseNotFound)
            }

            val now = clock.instant()
            val newProg = prog.advance(redReps, blueReps, exercises, now)

            val updated: MatchProgress =
                repoMatch.updateMatchProgress(
                    progress = newProg,
                    redCurrentExerciseId = newProg.redCurrentExerciseId,
                    blueCurrentExerciseId = newProg.blueCurrentExerciseId,
                ) ?: return@run failure(MatchError.ProgressNotFound)

            val redFinishedAt = updated.redFinishedAt
            val blueFinishedAt = updated.blueFinishedAt

            val newFinish =
                (redFinishedAt != null && prog.redFinishedAt == null) ||
                    (blueFinishedAt != null && prog.blueFinishedAt == null)

            if (newFinish) {
                val matchFinished = redFinishedAt != null && blueFinishedAt != null
                val redWon =
                    if (matchFinished) {
                        !redFinishedAt.isAfter(blueFinishedAt)
                    } else {
                        redFinishedAt != null
                    }

                repoMatch.save(
                    match.copy(
                        status = if (matchFinished) MatchStatus.FINISHED else MatchStatus.RUNNING,
                        winnerAthleteId = if (redWon) match.athleteRedId else match.athleteBlueId,
                        finishedAt =
                            if (matchFinished) {
                                if (redWon) blueFinishedAt else redFinishedAt
                            } else {
                                null
                            },
                    ),
                ) ?: return@run failure(MatchError.MatchNotFound)
            }

            val tournamentId =
                repoTournament.findByBracketId(match.bracketId)?.tournamentId
                    ?: return@run failure(MatchError.BracketNotFound)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = updated,
            ).let { publisher.publish(it) }

            success(updated)
        }

    fun forceFinishSide(
        matchId: Int,
        side: RepSide,
    ): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                repoMatch.findById(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(MatchError.MatchNotRunning)
            }

            val prog =
                repoMatch.findProgressByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            val opponentFinishedAt = if (side == RepSide.RED) prog.blueFinishedAt else prog.redFinishedAt
            if (opponentFinishedAt == null) {
                return@run failure(MatchError.OpponentNotFinished)
            }

            val now = clock.instant()

            val newProg: MatchProgress =
                when (side) {
                    RepSide.RED ->
                        prog.copy(redCurrentExerciseId = null, redFinishedAt = now, updatedAt = now)
                    RepSide.BLUE ->
                        prog.copy(blueCurrentExerciseId = null, blueFinishedAt = now, updatedAt = now)
                }

            val updated: MatchProgress =
                repoMatch
                    .updateMatchProgress(
                        progress = newProg,
                        redCurrentExerciseId = newProg.redCurrentExerciseId,
                        blueCurrentExerciseId = newProg.blueCurrentExerciseId,
                    ) ?: return@run failure(MatchError.ProgressNotFound)

            repoMatch.save(
                match.copy(
                    status = MatchStatus.FINISHED,
                    winnerAthleteId = if (side == RepSide.RED) match.athleteBlueId else match.athleteRedId,
                    finishedAt = now,
                ),
            ) ?: return@run failure(MatchError.MatchNotFound)

            val tournamentId =
                repoTournament.findByBracketId(match.bracketId)?.tournamentId
                    ?: return@run failure(MatchError.BracketNotFound)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = updated,
            ).let { publisher.publish(it) }

            success(updated)
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

    fun getMatchProgress(matchId: Int): Either<MatchError, MatchProgress> =
        trxManager.run {
            repoMatch.findById(matchId)
                ?: return@run failure(MatchError.MatchNotFound)

            val progress =
                repoMatch.findProgressByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            success(progress)
        }
}
