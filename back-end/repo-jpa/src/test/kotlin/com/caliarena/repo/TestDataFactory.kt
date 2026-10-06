package com.caliarena.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.entities.routine.ScreenRoutineEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentJudgeEntity
import com.caliarena.repo.entities.user.TokenEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

internal fun now(): Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)

internal fun Transaction.newUser(username: String = "user-${System.nanoTime()}"): UserEntity =
    users.save(
        UserEntity(
            username = username,
            password = "hashed_pw",
            role = UserRole.JUDGE,
            createdAt = now().epochSecond,
        ),
    )

internal fun Transaction.createToken(
    user: UserEntity,
    validation: String,
    lastUsedAt: Instant = now(),
): TokenEntity {
    val token =
        TokenEntity(
            tokenValidation = validation,
            user = user,
            createdAt = lastUsedAt.epochSecond,
            lastUsedAt = lastUsedAt.epochSecond,
        )
    tokens.deleteOldestTokensExceeding(user.id, 1)
    return tokens.save(token)
}

internal fun Transaction.newClub(
    name: String = "club-${System.nanoTime()}",
    shortName: String? = null,
): ClubEntity =
    clubs.save(
        ClubEntity(
            name = name,
            shortName = shortName,
            createdAt = now().epochSecond,
        ),
    )

internal fun Transaction.newAthlete(
    name: String = "athlete-${System.nanoTime()}",
    gender: GenderType = GenderType.MALE,
    club: ClubEntity? = null,
): AthleteEntity {
    val athleteClub = club ?: newClub()
    return athletes.save(
        AthleteEntity(
            name = name,
            gender = gender,
            club = athleteClub,
            createdAt = now().epochSecond,
        ),
    )
}

internal fun Transaction.newTournament(
    status: TournamentStatus = TournamentStatus.DRAFT,
    host: UserEntity? = null,
): TournamentEntity =
    tournaments.save(
        TournamentEntity(
            name = "t-${System.nanoTime()}",
            hostId = host?.id,
            status = status,
            createdAt = now().epochSecond,
        ),
    )

internal fun Transaction.assignJudge(
    tournament: TournamentEntity,
    judge: UserEntity,
): TournamentJudgeEntity =
    tournamentJudges.save(
        TournamentJudgeEntity(
            tournamentId = tournament.id,
            userId = judge.id,
            createdAt = now().epochSecond,
        ),
    )

internal fun Transaction.newBracket(
    tournament: TournamentEntity,
    division: String = "ELITE MALE",
    stage: BracketStage = BracketStage.QUALIFIERS,
): BracketEntity =
    brackets.save(
        BracketEntity(
            tournament = tournament,
            division = division,
            stage = stage,
            createdAt = now().epochSecond,
        ),
    )

internal fun Transaction.newRoutine(name: String = "routine-${System.nanoTime()}"): EnduranceRoutineEntity =
    routines.save(
        EnduranceRoutineEntity(
            name = name,
            timeCapSeconds = 600,
            createdAt = now().epochSecond,
        ),
    )

internal fun Transaction.newExercise(
    routine: EnduranceRoutineEntity,
    order: Int = 1,
): ExerciseEntity =
    exercises.save(
        ExerciseEntity(
            routine = routine,
            name = "ex-$order",
            targetReps = 10,
            exerciseOrder = order,
            type = ExerciseType.NORMAL,
        ),
    )

internal fun Transaction.newExerciseWithRoutine(): ExerciseEntity {
    val routine = newRoutine("r-${System.nanoTime()}")
    return newExercise(routine)
}

internal fun Transaction.newMatch(status: MatchStatus = MatchStatus.RUNNING): MatchEntity {
    val tournament = newTournament()
    val bracket = newBracket(tournament)
    val club = newClub()
    val red = newAthlete("red-${System.nanoTime()}", club = club)
    val blue = newAthlete("blue-${System.nanoTime()}", club = club)
    val routine = newRoutine("mr-${System.nanoTime()}")
    return matches.save(
        MatchEntity(
            bracket = bracket,
            routineId = routine.id,
            athleteRed = red,
            athleteBlue = blue,
            status = status,
            createdAt = now().epochSecond,
        ),
    )
}

internal fun Transaction.newRunningMatch(tournament: TournamentEntity): MatchEntity {
    val bracket = newBracket(tournament)
    val red = newAthlete("red-${System.nanoTime()}")
    val blue = newAthlete("blue-${System.nanoTime()}")
    val routine = newRoutine("rt-${System.nanoTime()}")
    return matches.save(
        MatchEntity(
            bracket = bracket,
            routineId = routine.id,
            athleteRed = red,
            athleteBlue = blue,
            status = MatchStatus.RUNNING,
            createdAt = now().epochSecond,
        ),
    )
}

internal fun Transaction.newProgress(match: MatchEntity): MatchProgressEntity {
    val exercise = newExerciseWithRoutine()
    return matchProgresses.save(
        MatchProgressEntity(
            match = match,
            redCurrentExercise = exercise,
            blueCurrentExercise = exercise,
            timerStartedAt = now().epochSecond,
            updatedAt = now().epochSecond,
        ),
    )
}

internal fun Transaction.newScreenRoutine(
    tournament: TournamentEntity,
    displayOrder: Int = 1,
    label: String? = null,
): ScreenRoutineEntity {
    val routine = newRoutine("sr-${System.nanoTime()}")
    return screenRoutines.save(
        ScreenRoutineEntity(
            tournamentId = tournament.id,
            routineId = routine.id,
            displayOrder = displayOrder,
            label = label,
            createdAt = now().epochSecond,
            updatedAt = now().epochSecond,
        ),
    )
}
