package com.caliarena.service

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.RepSide
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class MatchServiceTest : ServiceTest() {
    private lateinit var service: MatchService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.matches).thenReturn(matches)
        lenient().whenever(transaction.matchProgresses).thenReturn(matchProgresses)
        lenient().whenever(transaction.routines).thenReturn(routines)
        lenient().whenever(transaction.exercises).thenReturn(exercises)
        lenient().whenever(transaction.users).thenReturn(users)
        lenient().whenever(transaction.tokens).thenReturn(tokens)
        lenient().whenever(transaction.athletes).thenReturn(athletes)
        lenient().whenever(transaction.brackets).thenReturn(brackets)

        // relações do progresso resolvidas por id ao gravar
        lenient().whenever(exercises.findById(any())).thenAnswer { Optional.of(exerciseEntity(it.getArgument<Int>(0))) }

        // por defeito, qualquer bracket existe (testes individuais podem sobrepor)
        lenient().whenever(brackets.findById(any())).thenAnswer { Optional.of(bracketEntity(it.getArgument<Int>(0))) }

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = MatchService(trxManager, clock, mock())
    }

    private val now = clock.instant()

    // ---------- fixtures ----------

    private val routineForExercises = EnduranceRoutineEntity(2, "Routine", 5, now.epochSecond)

    private fun exerciseEntity(
        id: Int,
        supersetOrder: Int? = null,
    ): ExerciseEntity = ExerciseEntity(id, routineForExercises, "Ex$id", 10, null, id, supersetOrder, ExerciseType.NORMAL)

    private val exsEntities: List<ExerciseEntity> =
        listOf(exerciseEntity(1), exerciseEntity(2), exerciseEntity(3))

    private val supersetExsEntities: List<ExerciseEntity> =
        listOf(
            exerciseEntity(1),
            exerciseEntity(2),
            exerciseEntity(3),
            ExerciseEntity(4, routineForExercises, "Muscle Up", 2, null, 4, 1, ExerciseType.SUPERSET),
            ExerciseEntity(5, routineForExercises, "Straight Bar Dip", 10, null, 4, 2, ExerciseType.SUPERSET),
            ExerciseEntity(6, routineForExercises, "Pull up", 10, null, 4, 3, ExerciseType.SUPERSET),
        )

    private fun bracketEntity(id: Int = 1) = BracketEntity(id = id, stage = BracketStage.QUALIFIERS, createdAt = now.epochSecond)

    private fun judgeEntity(id: Int = 3) = UserEntity(id, "judge-$id", "hash", UserRole.JUDGE, now.epochSecond)

    private fun athleteEntity(
        id: Int,
        name: String = "athlete-$id",
    ) = AthleteEntity(id, name, GenderType.MALE, ClubEntity(name = "c$id", createdAt = now.epochSecond), now.epochSecond)

    private fun matchEntity(
        id: Int = 1,
        status: MatchStatus = MatchStatus.PENDING,
    ): MatchEntity =
        MatchEntity(
            id = 1,
            bracket = bracketEntity(),
            routineId = 2,
            judge = judgeEntity(),
            athleteRed = athleteEntity(10),
            athleteBlue = athleteEntity(20),
            status = status,
            createdAt = now.epochSecond,
        )

    private fun progEntity(
        match: MatchEntity,
        redEx: Int? = 1,
        blueEx: Int? = 1,
        redReps: Int = 0,
        blueReps: Int = 0,
        redFinishedAtEpoch: Long? = null,
        blueFinishedAtEpoch: Long? = null,
    ): MatchProgressEntity =
        MatchProgressEntity(
            match = match,
            redCurrentExercise = redEx?.let { exerciseEntity(it) },
            blueCurrentExercise = blueEx?.let { exerciseEntity(it + 100) },
            redCurrentReps = redReps,
            blueCurrentReps = blueReps,
            redFinishedAt = redFinishedAtEpoch,
            blueFinishedAt = blueFinishedAtEpoch,
            updatedAt = now.epochSecond,
        ).also { it.id = 1 }

    // red/blue current exercises must map to ids within exsEntities (1..3)
    private fun progOn(
        match: MatchEntity,
        redEx: Int?,
        blueEx: Int?,
        redReps: Int = 0,
        blueReps: Int = 0,
    ): MatchProgressEntity =
        MatchProgressEntity(
            match = match,
            redCurrentExercise = redEx?.let { exerciseEntity(it) },
            blueCurrentExercise = blueEx?.let { exerciseEntity(it) },
            redCurrentReps = redReps,
            blueCurrentReps = blueReps,
            updatedAt = now.epochSecond,
        ).also { it.id = 1 }

    private fun stubBracketExists() {
        whenever(brackets.findById(1)).thenReturn(Optional.of(bracketEntity()))
    }

    private fun stubUpdateProgressReturnsArg() {
        doAnswer { invocation -> invocation.getArgument<MatchProgressEntity>(0) }
            .whenever(matchProgresses)
            .save(any<MatchProgressEntity>())
    }

    @Nested
    inner class CreateMatch {
        @Test
        fun `should fail when bracket not found`() {
            whenever(brackets.findById(1)).thenReturn(Optional.empty())

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.BracketNotFound), result)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should fail when routine not found`() {
            stubBracketExists()
            whenever(routines.findById(2)).thenReturn(Optional.empty())

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.RoutineNotFound), result)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should fail when judge not found`() {
            stubBracketExists()
            whenever(routines.findById(2)).thenReturn(Optional.of(mockRoutineEntity()))
            whenever(users.findById(3)).thenReturn(Optional.empty())

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.JudgeNotFound), result)
        }

        @Test
        fun `should fail when red athlete not found`() {
            stubBracketExists()
            whenever(routines.findById(2)).thenReturn(Optional.of(mockRoutineEntity()))
            whenever(users.findById(3)).thenReturn(Optional.of(judgeEntity()))
            whenever(athletes.findById(10)).thenReturn(Optional.empty())

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.AthleteNotFound), result)
        }

        @Test
        fun `should fail when blue athlete not found`() {
            stubBracketExists()
            whenever(routines.findById(2)).thenReturn(Optional.of(mockRoutineEntity()))
            whenever(users.findById(3)).thenReturn(Optional.of(judgeEntity()))
            whenever(athletes.findById(10)).thenReturn(Optional.of(athleteEntity(10)))
            whenever(athletes.findById(20)).thenReturn(Optional.empty())

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.AthleteNotFound), result)
        }

        @Test
        fun `should succeed`() {
            stubBracketExists()
            whenever(routines.findById(2)).thenReturn(Optional.of(mockRoutineEntity()))
            whenever(users.findById(3)).thenReturn(Optional.of(judgeEntity()))
            whenever(athletes.findById(10)).thenReturn(Optional.of(athleteEntity(10)))
            whenever(athletes.findById(20)).thenReturn(Optional.of(athleteEntity(20)))
            whenever(matches.save(any())).thenReturn(matchEntity(status = MatchStatus.PENDING))

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertTrue(result is Either.Right)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches).save(captor.capture())
            assertEquals(MatchStatus.PENDING, captor.firstValue.status)
            assertEquals(2, captor.firstValue.routineId)
        }
    }

    @Nested
    inner class StartMatch {
        @Test
        fun `should fail when match not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.empty())

            val result = service.startMatch(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when progress already exists`() {
            val match = matchEntity(status = MatchStatus.PENDING)
            whenever(matches.findById(1)).thenReturn(Optional.of(match))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progOn(match, 1, 1))

            val result = service.startMatch(1)

            assertEquals(failure(MatchError.ProgressAlreadyExists), result)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should fail when athletes not assigned`() {
            val match = matchEntity(status = MatchStatus.PENDING)
            match.athleteRed = null
            match.athleteBlue = null
            whenever(matches.findById(1)).thenReturn(Optional.of(match))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(null)

            val result = service.startMatch(1)

            assertEquals(failure(MatchError.AthletesNotAssigned), result)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should fail when match already started`() {
            val match = matchEntity(status = MatchStatus.RUNNING)
            whenever(matches.findById(1)).thenReturn(Optional.of(match))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(null)

            val result = service.startMatch(1)

            assertEquals(failure(MatchError.MatchAlreadyStarted), result)
        }

        @Test
        fun `should fail when no exercises exist`() {
            val match = matchEntity(status = MatchStatus.PENDING)
            whenever(matches.findById(1)).thenReturn(Optional.of(match))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(null)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(emptyList())

            val result = service.startMatch(1)

            assertEquals(failure(MatchError.RoutineNotFound), result)
        }

        @Test
        fun `should succeed`() {
            val match = matchEntity(status = MatchStatus.PENDING)
            val firstExercise = exerciseEntity(1)
            val progress =
                MatchProgressEntity(
                    match = match,
                    redCurrentExercise = firstExercise,
                    blueCurrentExercise = firstExercise,
                    timerStartedAt = now.epochSecond,
                    updatedAt = now.epochSecond,
                ).also { it.id = 1 }

            whenever(matches.findById(1)).thenReturn(Optional.of(match))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(null)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(listOf(firstExercise))
            stubBracketExists()
            whenever(matchProgresses.save(any())).thenReturn(progress)

            val result = service.startMatch(1)

            assertEquals(success(progress.toDomain()), result)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches).save(captor.capture())
            assertEquals(MatchStatus.RUNNING, captor.firstValue.status)
            assertEquals(now.epochSecond, captor.firstValue.startedAt)
        }
    }

    @Nested
    inner class UpdateAthletesReps {
        private val running = matchEntity(status = MatchStatus.RUNNING)

        @Test
        fun `should fail when match not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.empty())

            val result = service.updateAthletesReps(1, 5, 3)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when match not running`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(matchEntity(status = MatchStatus.PENDING)))

            val result = service.updateAthletesReps(1, 5, 3)

            assertEquals(failure(MatchError.MatchNotRunning), result)
            verify(matchProgresses, never()).findByMatchId(any())
        }

        @Test
        fun `should fail when progress not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(null)

            val result = service.updateAthletesReps(1, 5, 3)

            assertEquals(failure(MatchError.ProgressNotFound), result)
        }

        @Test
        fun `should fail when red exercise not found`() {
            val badProg = progOn(running, redEx = null, blueEx = 1)
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(badProg)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(exsEntities)

            val result = service.updateAthletesReps(1, 5, 3)

            assertEquals(failure(MatchError.ExerciseNotFound), result)
        }

        @Test
        fun `should update without finishing`() {
            val prog = progOn(running, 1, 1, redReps = 0, blueReps = 0)
            val updated = progOn(running, 1, 1, redReps = 5, blueReps = 3)
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(prog)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(exsEntities)
            whenever(matchProgresses.save(any())).thenReturn(updated)

            val result = service.updateAthletesReps(1, 5, 3)

            assertEquals(success(updated.toDomain()), result)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should set provisional winner when red finishes`() {
            val progLast = progOn(running, redEx = 3, blueEx = 1, redReps = 8)
            val updated =
                progOn(running, redEx = null, blueEx = 1, redReps = 10)
                    .apply {
                        redFinishedAt = now.epochSecond
                        updatedAt = now.epochSecond
                    }

            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progLast)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(exsEntities)
            whenever(matchProgresses.save(any())).thenReturn(updated)

            val result = service.updateAthletesReps(1, 10, null)

            assertEquals(success(updated.toDomain()), result)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches, atLeastOnce()).save(captor.capture())
            assertEquals(MatchStatus.RUNNING, captor.firstValue.status)
            assertEquals(running.athleteRed, captor.firstValue.winnerAthlete)
        }

        @Test
        fun `should set FINISHED only when both athletes finish`() {
            val progLast = progOn(running, redEx = 3, blueEx = 3, redReps = 8, blueReps = 8)
            val updated =
                progOn(running, redEx = null, blueEx = null, redReps = 10, blueReps = 10)
                    .apply {
                        redFinishedAt = now.epochSecond
                        blueFinishedAt = now.epochSecond
                        updatedAt = now.epochSecond
                    }

            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progLast)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(exsEntities)
            whenever(matchProgresses.save(any())).thenReturn(updated)

            val result = service.updateAthletesReps(1, 10, 10)

            assertEquals(success(updated.toDomain()), result)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches, atLeastOnce()).save(captor.capture())
            assertEquals(MatchStatus.FINISHED, captor.firstValue.status)
            assertEquals(running.athleteRed, captor.firstValue.winnerAthlete)
            assertEquals(now.epochSecond, captor.firstValue.finishedAt)
        }

        @Test
        fun `should still advance the other athlete after the first one finishes`() {
            val progRedDone =
                progOn(running, redEx = null, blueEx = 3, redReps = 10)
                    .apply { redFinishedAt = now.epochSecond }
            val updated =
                progOn(running, redEx = null, blueEx = 3, redReps = 10, blueReps = 5)
                    .apply { redFinishedAt = now.epochSecond }
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progRedDone)
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(exsEntities)
            stubUpdateProgressReturnsArg()

            val result = service.updateAthletesReps(1, 10, 5)

            assertEquals(success(updated.toDomain()), result)
            verify(matches, never()).save(any())
        }
    }

    @Nested
    inner class ForceFinishSide {
        private val running = matchEntity(status = MatchStatus.RUNNING)

        @Test
        fun `should fail when match not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.empty())

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when match not running`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(matchEntity(status = MatchStatus.PENDING)))

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.MatchNotRunning), result)
            verify(matchProgresses, never()).findByMatchId(any())
            verify(matches, never()).save(any())
        }

        @Test
        fun `should fail when opponent has not finished`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progOn(running, 1, 1))

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.OpponentNotFinished), result)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should force finish red and blue wins when blue already finished`() {
            val progBlueDone =
                progOn(running, redEx = 1, blueEx = null, blueReps = 10)
                    .apply { blueFinishedAt = now.minusSeconds(10).epochSecond }
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progBlueDone)
            stubBracketExists()
            stubUpdateProgressReturnsArg()

            val result = service.forceFinishSide(1, RepSide.RED)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches, atLeastOnce()).save(captor.capture())
            val saved = captor.firstValue
            assertEquals(MatchStatus.FINISHED, saved.status)
            assertEquals(running.athleteBlue, saved.winnerAthlete)
            assertEquals(now.epochSecond, saved.finishedAt)
        }

        @Test
        fun `should force finish blue and red wins when red already finished`() {
            val progRedDone =
                progOn(running, redEx = null, blueEx = 1, redReps = 10)
                    .apply { redFinishedAt = now.minusSeconds(10).epochSecond }
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progRedDone)
            stubBracketExists()
            stubUpdateProgressReturnsArg()

            val result = service.forceFinishSide(1, RepSide.BLUE)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches, atLeastOnce()).save(captor.capture())
            assertEquals(MatchStatus.FINISHED, captor.firstValue.status)
            assertEquals(running.athleteRed, captor.firstValue.winnerAthlete)
        }
    }

    @Nested
    inner class SupersetAdvance {
        private val running = matchEntity(status = MatchStatus.RUNNING)

        private fun progressOn(
            exerciseId: Int,
            reps: Int,
        ): MatchProgressEntity =
            MatchProgressEntity(
                match = running,
                redCurrentExercise = exerciseEntity(exerciseId),
                blueCurrentExercise = exerciseEntity(1),
                redCurrentReps = reps,
                updatedAt = now.epochSecond,
            ).also { it.id = 1 }

        private fun realAdvance() {
            whenever(exercises.findExercisesByRoutineId(2)).thenReturn(supersetExsEntities)
            stubUpdateProgressReturnsArg()
        }

        @Test
        fun `should advance to second superset exercise after first completes`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progressOn(4, 1))
            realAdvance()

            val progress = expectSuccess(service.updateAthletesReps(1, 2, null))

            assertEquals(5, progress.redCurrentExerciseId)
            assertEquals(0, progress.redCurrentReps)
            assertNull(progress.redFinishedAt)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should advance to third superset exercise after second completes`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progressOn(5, 9))
            realAdvance()

            val progress = expectSuccess(service.updateAthletesReps(1, 10, null))

            assertEquals(6, progress.redCurrentExerciseId)
            assertEquals(0, progress.redCurrentReps)
            verify(matches, never()).save(any())
        }

        @Test
        fun `should finish only after last superset exercise completes`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(running))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progressOn(6, 9))
            realAdvance()

            val progress = expectSuccess(service.updateAthletesReps(1, 10, null))

            assertNull(progress.redCurrentExerciseId)
            assertEquals(10, progress.redCurrentReps)
            assertNotNull(progress.redFinishedAt)

            val captor = argumentCaptor<MatchEntity>()
            verify(matches, atLeastOnce()).save(captor.capture())
            assertEquals(MatchStatus.RUNNING, captor.firstValue.status)
            assertEquals(running.athleteRed, captor.firstValue.winnerAthlete)
        }

        private fun expectSuccess(result: Either<MatchError, MatchProgress>): MatchProgress =
            when (result) {
                is Either.Right -> result.value
                is Either.Left -> throw AssertionError("Expected success but got ${result.value}")
            }
    }

    @Nested
    inner class GetMatchById {
        @Test
        fun `should fail when not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.empty())

            val result = service.getMatchById(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should succeed`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(matchEntity()))

            assertTrue(service.getMatchById(1) is Either.Right)
        }
    }

    @Nested
    inner class GetMatchesByBracket {
        @Test
        fun `should fail when bracket not found`() {
            whenever(brackets.findById(1)).thenReturn(Optional.empty())

            val result = service.getMatchesByBracket(1)

            assertEquals(failure(MatchError.BracketNotFound), result)
        }

        @Test
        fun `should succeed with list`() {
            stubBracketExists()
            val list = listOf(matchEntity(1), matchEntity(2))
            whenever(matches.findByBracketId(1)).thenReturn(list)

            val result = service.getMatchesByBracket(1)

            assertEquals(success(list.map { it.toDomain() }), result)
        }
    }

    @Nested
    inner class GetMatchProgress {
        @Test
        fun `should fail when match not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.empty())

            val result = service.getMatchProgress(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when progress not found`() {
            whenever(matches.findById(1)).thenReturn(Optional.of(matchEntity()))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(null)

            val result = service.getMatchProgress(1)

            assertEquals(failure(MatchError.ProgressNotFound), result)
        }

        @Test
        fun `should succeed`() {
            val match = matchEntity()
            val progress = progOn(match, 1, 1)
            whenever(matches.findById(1)).thenReturn(Optional.of(match))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progress)

            val result = service.getMatchProgress(1)

            assertEquals(success(progress.toDomain()), result)
        }
    }

    private fun mockRoutineEntity(id: Int = 2) = EnduranceRoutineEntity(id, "Routine", 5, now.epochSecond)
}
