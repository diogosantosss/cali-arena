package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.routine.Exercise
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
}

@Service
class MatchService(
    private val trxManager: TransactionManager,
    private val clock: Clock,
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
                repoEnduranceRoutine.findExercisesByRoutineId(match.routineId).firstOrNull()
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

            if (redReps != null && exercises.none { it.id == prog.redCurrentExerciseId }) {
                return@run failure(MatchError.ExerciseNotFound)
            }
            if (blueReps != null && exercises.none { it.id == prog.blueCurrentExerciseId }) {
                return@run failure(MatchError.ExerciseNotFound)
            }

            val now = clock.instant()
            val newProg = prog.advance(redReps, blueReps, exercises, now)

            val updated =
                repoMatch.updateMatchProgress(
                    progress = newProg,
                    redCurrentExerciseId = newProg.redCurrentExerciseId,
                    blueCurrentExerciseId = newProg.blueCurrentExerciseId,
                ) ?: return@run failure(MatchError.ProgressNotFound)

            val (redFinishedAt, blueFinishedAt) = updated.redFinishedAt to updated.blueFinishedAt

            if (redFinishedAt != null || blueFinishedAt != null) {
                val redWon = redFinishedAt != null

                repoMatch.save(
                    match.copy(
                        status = if (redFinishedAt != null && blueFinishedAt != null) MatchStatus.FINISHED else MatchStatus.RUNNING,
                        winnerAthleteId = if (redWon) match.athleteRedId else match.athleteBlueId,
                        finishedAt = if (redWon) redFinishedAt else blueFinishedAt,
                    ),
                ) ?: return@run failure(MatchError.MatchNotFound)
            }

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
