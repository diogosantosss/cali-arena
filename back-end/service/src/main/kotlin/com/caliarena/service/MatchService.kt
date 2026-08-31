package com.caliarena.service

import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.RepSide
import com.caliarena.domain.match.StartedMatch
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
    ): Either<ApiError, Match> =
        trxManager.run {
            val bracket =
                brackets.findByIdOrNull(bracketId)
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            routines.findByIdOrNull(routineId)
                ?: return@run failure(ApiError.ROUTINE_NOT_FOUND)

            val judge =
                users.findByIdOrNull(judgeId)
                    ?: return@run failure(ApiError.JUDGE_NOT_FOUND)

            val red =
                athletes.findByIdOrNull(athleteRedId)
                    ?: return@run failure(ApiError.ATHLETE_NOT_FOUND)

            val blue =
                athletes.findByIdOrNull(athleteBlueId)
                    ?: return@run failure(ApiError.ATHLETE_NOT_FOUND)

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

    fun startMatch(matchId: Int): Either<ApiError, StartedMatch> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            if (matchProgresses.findByMatchId(matchId) != null) {
                return@run failure(ApiError.PROGRESS_ALREADY_EXISTS)
            }

            if (match.athleteRed == null || match.athleteBlue == null) {
                return@run failure(ApiError.ATHLETES_NOT_ASSIGNED)
            }

            if (match.status == MatchStatus.RUNNING || match.status == MatchStatus.FINISHED) {
                return@run failure(ApiError.MATCH_ALREADY_STARTED)
            }

            val firstExercise =
                exercises
                    .findExercisesByRoutineId(match.routineId)
                    .minWithOrNull(compareBy({ it.exerciseOrder }, { it.supersetOrder ?: 0 }))
                    ?: return@run failure(ApiError.ROUTINE_NOT_FOUND)

            val nowMillis = clock.instant().toEpochMilli()
            val nowSeconds = clock.instant().epochSecond

            match.status = MatchStatus.RUNNING
            match.startedAt = nowMillis
            matches.save(match)

            val progress =
                matchProgresses.save(
                    MatchProgressEntity(
                        match = match,
                        redCurrentExercise = firstExercise,
                        blueCurrentExercise = firstExercise,
                        timerStartedAt = nowMillis,
                        updatedAt = nowSeconds,
                    ),
                )

            val tournamentId =
                brackets.findByIdOrNull(match.bracket.id)?.tournament?.id
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = progress.toDomain(),
            ).let { publisher.publish(it) }

            success(StartedMatch(match = match.toDomain(), progress = progress.toDomain()))
        }

    fun updateAthletesReps(
        matchId: Int,
        redReps: Int? = null,
        blueReps: Int? = null,
    ): Either<ApiError, MatchProgress> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(ApiError.MATCH_NOT_RUNNING)
            }

            val prog =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(ApiError.PROGRESS_NOT_FOUND)

            val progDomain = prog.toDomain()

            val exerciseDomains =
                exercises.findExercisesByRoutineId(match.routineId).map(ExerciseEntity::toDomain)

            if (redReps != null && progDomain.redFinishedAt == null && exerciseDomains.none { it.id == progDomain.redCurrentExerciseId }) {
                return@run failure(ApiError.EXERCISE_NOT_FOUND)
            }
            if (blueReps != null &&
                progDomain.blueFinishedAt == null &&
                exerciseDomains.none { it.id == progDomain.blueCurrentExerciseId }
            ) {
                return@run failure(ApiError.EXERCISE_NOT_FOUND)
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
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = updated,
            ).let { publisher.publish(it) }

            success(updated)
        }

    fun forceFinishSide(
        matchId: Int,
        side: com.caliarena.domain.match.RepSide,
    ): Either<ApiError, MatchProgress> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            if (match.status != MatchStatus.RUNNING) {
                return@run failure(ApiError.MATCH_NOT_RUNNING)
            }

            val prog =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(ApiError.PROGRESS_NOT_FOUND)

            val progDomain = prog.toDomain()

            val isRed = side == RepSide.RED

            val opponentFinishedAt = if (isRed) progDomain.blueFinishedAt else progDomain.redFinishedAt
            if (opponentFinishedAt == null) {
                return@run failure(ApiError.OPPONENT_NOT_FINISHED)
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
            match.finishedAt = now.toEpochMilli()
            matches.save(match)

            val tournamentId =
                brackets.findByIdOrNull(match.bracket.id)?.tournament?.id
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

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
                (if (redWon) blueFinishedAt else redFinishedAt).toEpochMilli()
            } else {
                null
            }
        matches.save(match)
    }

    fun getMatchById(id: Int): Either<ApiError, Match> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(id)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            success(match.toDomain())
        }

    fun getMatchesByBracket(bracketId: Int): Either<ApiError, List<Match>> =
        trxManager.run {
            brackets.findByIdOrNull(bracketId)
                ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            success(matches.findByBracketId(bracketId).map(MatchEntity::toDomain))
        }

    fun getMatchProgress(matchId: Int): Either<ApiError, MatchProgress> =
        trxManager.run {
            matches.findByIdOrNull(matchId)
                ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            val progress =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(ApiError.PROGRESS_NOT_FOUND)

            success(progress.toDomain())
        }
}
