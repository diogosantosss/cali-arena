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
        athleteRedId: Int?,
        athleteBlueId: Int?,
    ): Either<ApiError, Match> =
        trxManager.run {
            val bracket =
                brackets.findByIdOrNull(bracketId)
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            routines.findByIdOrNull(routineId)
                ?: return@run failure(ApiError.ROUTINE_NOT_FOUND)

            if (athleteRedId == null && athleteBlueId == null) {
                return@run failure(ApiError.ATHLETES_NOT_ASSIGNED)
            }

            val red =
                athleteRedId?.let { id ->
                    athletes.findByIdOrNull(id)
                        ?: return@run failure(ApiError.ATHLETE_NOT_FOUND)
                }

            val blue =
                athleteBlueId?.let { id ->
                    athletes.findByIdOrNull(id)
                        ?: return@run failure(ApiError.ATHLETE_NOT_FOUND)
                }

            if (red != null && red == blue) {
                return@run failure(ApiError.SAME_ATHLETE_ON_BOTH_SIDES)
            }

            val firstExercise =
                exercises
                    .findExercisesByRoutineId(routineId)
                    .minWithOrNull(compareBy({ it.exerciseOrder }, { it.supersetOrder ?: 0 }))
                    ?: return@run failure(ApiError.ROUTINE_NOT_FOUND)

            val now = clock.instant()

            val match =
                matches.save(
                    MatchEntity(
                        bracket = bracket,
                        routineId = routineId,
                        athleteRed = red,
                        athleteBlue = blue,
                        status = MatchStatus.PENDING,
                        createdAt = now.epochSecond,
                    ),
                )

            matchProgresses.save(
                MatchProgressEntity(
                    match = match,
                    redCurrentExercise = firstExercise,
                    blueCurrentExercise = firstExercise,
                    updatedAt = now.epochSecond,
                ),
            )

            success(match.toDomain())
        }

    fun startMatch(matchId: Int): Either<ApiError, StartedMatch> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            val tournamentId =
                brackets.findByIdOrNull(match.bracket.id)?.tournament?.id
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            if (match.athleteRed == null && match.athleteBlue == null) {
                return@run failure(ApiError.ATHLETES_NOT_ASSIGNED)
            }

            if (match.status == MatchStatus.RUNNING || match.status == MatchStatus.FINISHED) {
                return@run failure(ApiError.MATCH_ALREADY_STARTED)
            }

            val prog =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(ApiError.PROGRESS_NOT_FOUND)

            val now = clock.instant()

            match.status = MatchStatus.RUNNING
            match.startedAt = now.toEpochMilli()
            matches.save(match)

            prog.timerStartedAt = now.toEpochMilli()
            prog.updatedAt = now.epochSecond
            val updatedProg = matchProgresses.save(prog).toDomain()

            MatchUpdatedEvent(
                tournamentId = tournamentId,
                matchProgress = updatedProg,
            ).let { publisher.publish(it) }

            success(StartedMatch(match = match.toDomain(), progress = updatedProg))
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

            if (redReps != null && match.athleteRed == null) {
                return@run failure(ApiError.ATHLETE_NOT_IN_MATCH)
            }
            if (blueReps != null && match.athleteBlue == null) {
                return@run failure(ApiError.ATHLETE_NOT_IN_MATCH)
            }

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

    private fun Transaction.applyFinishTransition(
        match: MatchEntity,
        before: MatchProgress,
        updated: MatchProgress,
    ) {
        val (redTime, blueTime) = updated.redFinishedAt to updated.blueFinishedAt

        val newFinish =
            (redTime != null && before.redFinishedAt == null) ||
                (blueTime != null && before.blueFinishedAt == null)

        if (!newFinish) return

        val hasRed = match.athleteRed != null
        val hasBlue = match.athleteBlue != null
        val allFinished = (!hasRed || redTime != null) && (!hasBlue || blueTime != null)

        val redWon =
            when {
                !allFinished -> redTime != null
                !hasRed -> false
                !hasBlue -> true
                else -> !redTime!!.isAfter(blueTime!!)
            }

        match.status = if (allFinished) MatchStatus.FINISHED else MatchStatus.RUNNING

        match.winnerAthlete = if (redWon) match.athleteRed else match.athleteBlue

        match.finishedAt =
            if (allFinished) listOfNotNull(redTime, blueTime).maxOrNull()?.toEpochMilli() else null

        matches.save(match)
    }

    fun forceFinishSide(
        matchId: Int,
        side: RepSide,
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

            if (isRed && match.athleteRed == null) {
                return@run failure(ApiError.ATHLETE_NOT_IN_MATCH)
            }

            if (!isRed && match.athleteBlue == null) {
                return@run failure(ApiError.ATHLETE_NOT_IN_MATCH)
            }

            val opponentAthlete = if (isRed) match.athleteBlue else match.athleteRed
            val opponentFinishedAt = if (isRed) progDomain.blueFinishedAt else progDomain.redFinishedAt
            if (opponentAthlete != null && opponentFinishedAt == null) {
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

    fun getAllMatches(): Either<ApiError, List<Match>> =
        trxManager.run {
            success(matches.findAll().map(MatchEntity::toDomain))
        }

    fun deleteMatch(matchId: Int): Either<ApiError, Unit> =
        trxManager.run {
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            val state = tournamentStates.findByCurrentMatchId(matchId)
            if (state != null) {
                state.currentMatch = null
                tournamentStates.save(state)
            }

            matchProgresses.findByMatchId(matchId)?.let { matchProgresses.delete(it) }
            matches.delete(match)

            success(Unit)
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
            val match =
                matches.findByIdOrNull(matchId)
                    ?: return@run failure(ApiError.MATCH_NOT_FOUND)

            val progress =
                matchProgresses.findByMatchId(matchId)
                    ?: return@run failure(ApiError.PROGRESS_NOT_FOUND)

            success(progress.toDomain())
        }
}
