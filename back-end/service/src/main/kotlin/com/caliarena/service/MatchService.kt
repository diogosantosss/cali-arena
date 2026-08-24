package com.caliarena.service

import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.RepSide
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.entities.match.MatchProgressEntity.Companion.fromDomain
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManager
import com.caliarena.service.sse.MatchUpdatedEvent
import com.caliarena.service.sse.SpectatorPublisher
import org.springframework.data.repository.findByIdOrNull
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
            val bracket =
                brackets.findByIdOrNull(bracketId)
                    ?: return@run failure(MatchError.BracketNotFound)

            routines.findByIdOrNull(routineId)
                ?: return@run failure(MatchError.RoutineNotFound)

            val judge =
                users.findByIdOrNull(judgeId)
                    ?: return@run failure(MatchError.JudgeNotFound)

            val red =
                athletes.findByIdOrNull(athleteRedId)
                    ?: return@run failure(MatchError.AthleteNotFound)

            val blue =
                athletes.findByIdOrNull(athleteBlueId)
                    ?: return@run failure(MatchError.AthleteNotFound)

            val match =
                matches.save(
                    MatchEntity(
                        bracket = bracket,
                        routineId = routineId,
                        judge = judge,
                        athleteRed = red,
                        athleteBlue = blue,
                        status = MatchStatus.PENDING,
                        createdAt = clock.instant().epochSecond,
                    ),
                )

            success(match.toDomain())
        }

    fun startMatch(matchId: Int): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (matchProgresses.findByMatchId(matchId) != null) {
                return@run failure(MatchError.ProgressAlreadyExists)
            }

            if (match.athleteRed == null || match.athleteBlue == null) {
                return@run failure(MatchError.AthletesNotAssigned)
            }

            if (match.status == MatchStatus.RUNNING || match.status == MatchStatus.FINISHED) {
                return@run failure(MatchError.MatchAlreadyStarted)
            }

            val firstExercise =
                exercises
                    .findExercisesByRoutineId(match.routineId)
                    .minWithOrNull(compareBy({ it.exerciseOrder }, { it.supersetOrder ?: 0 }))
                    ?: return@run failure(MatchError.RoutineNotFound)

            val now = clock.instant().epochSecond

            match.status = MatchStatus.RUNNING
            match.startedAt = now
            matches.save(match)

            val progress =
                matchProgresses.save(
                    MatchProgressEntity(
                        match = match,
                        redCurrentExercise = firstExercise,
                        blueCurrentExercise = firstExercise,
                        timerStartedAt = now,
                        updatedAt = now,
                    ),
                )

            val tournamentId =
                brackets.findByIdOrNull(match.bracket.id)?.tournament?.id
                    ?: return@run failure(MatchError.BracketNotFound)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = progress.toDomain(),
            ).let { publisher.publish(it) }

            success(progress.toDomain())
        }

    fun updateAthletesReps(
        matchId: Int,
        redReps: Int? = null,
        blueReps: Int? = null,
    ): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(MatchError.MatchNotRunning)
            }

            val prog =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            val progDomain = prog.toDomain()

            val exerciseDomains =
                exercises.findExercisesByRoutineId(match.routineId).map(ExerciseEntity::toDomain)

            if (redReps != null && progDomain.redFinishedAt == null && exerciseDomains.none { it.id == progDomain.redCurrentExerciseId }) {
                return@run failure(MatchError.ExerciseNotFound)
            }
            if (blueReps != null &&
                progDomain.blueFinishedAt == null &&
                exerciseDomains.none { it.id == progDomain.blueCurrentExerciseId }
            ) {
                return@run failure(MatchError.ExerciseNotFound)
            }

            val now = clock.instant()
            val newProg = progDomain.advance(redReps, blueReps, exerciseDomains, now)

            val redExercise = newProg.redCurrentExerciseId?.let { exercises.findByIdOrNull(it) }
            val blueExercise = newProg.blueCurrentExerciseId?.let { exercises.findByIdOrNull(it) }

            val updated =
                matchProgresses.save(newProg.fromDomain(match, redExercise, blueExercise)).toDomain()

            applyFinishTransition(match, progDomain, updated)

            val tournamentId =
                brackets.findByIdOrNull(match.bracket.id)?.tournament?.id
                    ?: return@run failure(MatchError.BracketNotFound)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = updated,
            ).let { publisher.publish(it) }

            success(updated)
        }

    fun forceFinishSide(
        matchId: Int,
        side: com.caliarena.domain.match.RepSide,
    ): Either<MatchError, MatchProgress> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(MatchError.MatchNotFound)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(MatchError.MatchNotRunning)
            }

            val prog =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            val progDomain = prog.toDomain()

            val isRed = side == RepSide.RED

            val opponentFinishedAt = if (isRed) progDomain.blueFinishedAt else progDomain.redFinishedAt
            if (opponentFinishedAt == null) {
                return@run failure(MatchError.OpponentNotFinished)
            }

            val now = clock.instant()

            val newProg =
                progDomain.copy(
                    redCurrentExerciseId = if (isRed) null else progDomain.redCurrentExerciseId,
                    redFinishedAt = if (isRed) progDomain.redFinishedAt ?: now else progDomain.redFinishedAt,
                    blueCurrentExerciseId = if (!isRed) null else progDomain.blueCurrentExerciseId,
                    blueFinishedAt = if (!isRed) progDomain.blueFinishedAt ?: now else progDomain.blueFinishedAt,
                    updatedAt = now,
                )

            val redExercise = newProg.redCurrentExerciseId?.let { exercises.findByIdOrNull(it) }
            val blueExercise = newProg.blueCurrentExerciseId?.let { exercises.findByIdOrNull(it) }

            val updated =
                matchProgresses.save(newProg.fromDomain(match, redExercise, blueExercise)).toDomain()

            // o lado forçado nunca é vencedor: o outro atleta ganha e a partida acaba já
            match.status = MatchStatus.FINISHED
            match.winnerAthlete = (if (isRed) match.athleteBlue else match.athleteRed)
            match.finishedAt = now.epochSecond
            matches.save(match)

            val tournamentId =
                brackets.findByIdOrNull(match.bracket.id)?.tournament?.id
                    ?: return@run failure(MatchError.BracketNotFound)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = updated,
            ).let { publisher.publish(it) }

            success(updated)
        }

    private fun Transaction.applyFinishTransition(
        match: MatchEntity,
        before: MatchProgress,
        updated: MatchProgress,
    ) {
        val redFinishedAt = updated.redFinishedAt
        val blueFinishedAt = updated.blueFinishedAt

        val newFinish =
            (redFinishedAt != null && before.redFinishedAt == null) ||
                (blueFinishedAt != null && before.blueFinishedAt == null)

        if (!newFinish) return

        val matchFinished = redFinishedAt != null && blueFinishedAt != null
        val redWon =
            if (matchFinished) {
                !redFinishedAt.isAfter(blueFinishedAt)
            } else {
                redFinishedAt != null
            }

        match.status = if (matchFinished) MatchStatus.FINISHED else MatchStatus.RUNNING
        match.winnerAthlete =
            if (redWon) match.athleteRed else match.athleteBlue
        match.finishedAt =
            if (matchFinished) {
                (if (redWon) blueFinishedAt else redFinishedAt).epochSecond
            } else {
                null
            }
        matches.save(match)
    }

    fun getMatchById(id: Int): Either<MatchError, Match> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(id)
                    ?: return@run failure(MatchError.MatchNotFound)

            success(match.toDomain())
        }

    fun getMatchesByBracket(bracketId: Int): Either<MatchError, List<Match>> =
        trxManager.run {
            brackets.findByIdOrNull(bracketId)
                ?: return@run failure(MatchError.BracketNotFound)

            success(matches.findByBracketId(bracketId).map(MatchEntity::toDomain))
        }

    fun getMatchProgress(matchId: Int): Either<MatchError, MatchProgress> =
        trxManager.run {
            matches.findByIdOrNull(matchId)
                ?: return@run failure(MatchError.MatchNotFound)

            val progress =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(MatchError.ProgressNotFound)

            success(progress.toDomain())
        }
}
