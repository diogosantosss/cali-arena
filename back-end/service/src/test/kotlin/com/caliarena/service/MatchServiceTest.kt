package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.RepSide
import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class MatchServiceTest : ServiceTest() {
    private lateinit var service: MatchService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoTournament).thenReturn(repoTournament)
        lenient().whenever(transaction.repoMatch).thenReturn(repoMatch)
        lenient().whenever(transaction.repoEnduranceRoutine).thenReturn(repoEnduranceRoutine)
        lenient().whenever(transaction.repoUser).thenReturn(repoUser)
        lenient().whenever(transaction.repoAthlete).thenReturn(repoAthlete)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = MatchService(trxManager, clock, mock())
    }

    private val now = clock.instant()

    private fun mockBracket(id: Int = 1) = Bracket(id, 1, GenderType.MALE, BracketStage.QUARTERFINALS, now)

    private fun mockRoutine(id: Int = 2) = EnduranceRoutine(id, "Routine", 5, now)

    private fun mockUser(id: Int = 3) = User(id, "judge", PasswordValidationInfo("hash"), UserRole.JUDGE, now)

    private fun mockAthlete(id: Int = 10) = Athlete(id, "Athlete $id", GenderType.MALE, 1, now)

    private fun mockMatch(
        id: Int = 1,
        athleteRedId: Int? = 10,
        athleteBlueId: Int? = 20,
        status: MatchStatus = MatchStatus.PENDING,
    ) = Match(id, 1, 2, 3, athleteRedId, athleteBlueId, null, status, null, null, now)

    private fun mockProgress(
        id: Int = 1,
        matchId: Int = 1,
        redCurrentExerciseId: Int? = 1,
        blueCurrentExerciseId: Int? = 2,
        redCurrentReps: Int = 0,
        blueCurrentReps: Int = 0,
        redFinishedAt: Instant? = null,
        blueFinishedAt: Instant? = null,
    ) = MatchProgress(
        id,
        matchId,
        redCurrentExerciseId,
        blueCurrentExerciseId,
        redCurrentReps,
        blueCurrentReps,
        redFinishedAt,
        blueFinishedAt,
        null,
        null,
        now,
    )

    private fun exercises(vararg ids: Int) =
        ids.map { id ->
            Exercise(id, 2, "Ex$id", 10, null, id, null, ExerciseType.NORMAL)
        }

    @Nested
    inner class CreateMatch {
        @Test
        fun `should fail when bracket not found`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(null)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.BracketNotFound), result)
            verify(repoMatch, never()).createMatch(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `should fail when routine not found`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoEnduranceRoutine.findById(2)).thenReturn(null)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.RoutineNotFound), result)
            verify(repoMatch, never()).createMatch(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `should fail when judge not found`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoEnduranceRoutine.findById(2)).thenReturn(mockRoutine())
            whenever(repoUser.findById(3)).thenReturn(null)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.JudgeNotFound), result)
            verify(repoMatch, never()).createMatch(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `should fail when red athlete not found`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoEnduranceRoutine.findById(2)).thenReturn(mockRoutine())
            whenever(repoUser.findById(3)).thenReturn(mockUser())
            whenever(repoAthlete.findById(10)).thenReturn(null)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.AthleteNotFound), result)
            verify(repoMatch, never()).createMatch(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `should fail when blue athlete not found`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoEnduranceRoutine.findById(2)).thenReturn(mockRoutine())
            whenever(repoUser.findById(3)).thenReturn(mockUser())
            whenever(repoAthlete.findById(10)).thenReturn(mockAthlete(10))
            whenever(repoAthlete.findById(20)).thenReturn(null)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.AthleteNotFound), result)
            verify(repoMatch, never()).createMatch(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `should fail when createMatch returns null`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoEnduranceRoutine.findById(2)).thenReturn(mockRoutine())
            whenever(repoUser.findById(3)).thenReturn(mockUser())
            whenever(repoAthlete.findById(10)).thenReturn(mockAthlete(10))
            whenever(repoAthlete.findById(20)).thenReturn(mockAthlete(20))
            whenever(repoMatch.createMatch(1, 2, 3, 10, 20, now)).thenReturn(null)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(failure(MatchError.BracketNotFound), result)
        }

        @Test
        fun `should succeed`() {
            val match = mockMatch()

            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoEnduranceRoutine.findById(2)).thenReturn(mockRoutine())
            whenever(repoUser.findById(3)).thenReturn(mockUser())
            whenever(repoAthlete.findById(10)).thenReturn(mockAthlete(10))
            whenever(repoAthlete.findById(20)).thenReturn(mockAthlete(20))
            whenever(repoMatch.createMatch(1, 2, 3, 10, 20, now)).thenReturn(match)

            val result = service.createMatch(1, 2, 3, 10, 20)

            assertEquals(success(match), result)
            verify(repoMatch).createMatch(1, 2, 3, 10, 20, now)
        }
    }

    @Nested
    inner class StartMatch {
        @Test
        fun `should fail when match not found`() {
            whenever(repoMatch.findById(1)).thenReturn(null)
            val result = service.startMatch(1)
            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when progress already exists`() {
            whenever(repoMatch.findById(1)).thenReturn(mockMatch())
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(mockProgress())
            val result = service.startMatch(1)
            assertEquals(failure(MatchError.ProgressAlreadyExists), result)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should fail when athletes not assigned`() {
            val match = mockMatch(athleteRedId = null, athleteBlueId = null)
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            val result = service.startMatch(1)
            assertEquals(failure(MatchError.AthletesNotAssigned), result)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should fail when save match returns null`() {
            val match = mockMatch()
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exercises(1))
            whenever(repoMatch.save(any())).thenReturn(null)
            val result = service.startMatch(1)
            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when create progress returns null`() {
            val match = mockMatch()
            val updated = match.copy(status = MatchStatus.RUNNING, startedAt = now)
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exercises(1))
            whenever(repoMatch.save(any())).thenReturn(updated)
            whenever(repoMatch.createMatchProgress(1, 1, now)).thenReturn(null)
            val result = service.startMatch(1)
            assertEquals(failure(MatchError.ErrorCreatingMatchProg), result)
        }

        @Test
        fun `should succeed`() {
            val match = mockMatch()
            val updated = match.copy(status = MatchStatus.RUNNING, startedAt = now)
            val progress = mockProgress()
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exercises(1))
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            whenever(repoMatch.save(any())).thenReturn(updated)
            whenever(repoMatch.createMatchProgress(1, 1, now)).thenReturn(progress)
            val result = service.startMatch(1)
            assertEquals(success(progress), result)
            verify(repoMatch).save(match.copy(status = MatchStatus.RUNNING, startedAt = now))
            verify(repoMatch).createMatchProgress(1, 1, now)
        }
    }

    @Nested
    inner class UpdateAthletesReps {
        private val match = mockMatch(status = MatchStatus.RUNNING)
        private val prog = mockProgress()
        private val exs = exercises(1, 2, 3)

        @Test
        fun `should fail when match not found`() {
            whenever(repoMatch.findById(1)).thenReturn(null)
            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when match not running`() {
            whenever(repoMatch.findById(1)).thenReturn(mockMatch(status = MatchStatus.PENDING))
            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(failure(MatchError.MatchNotRunning), result)
            verify(repoMatch, never()).findProgressByMatchId(any())
        }

        @Test
        fun `should fail when progress not found`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(failure(MatchError.ProgressNotFound), result)
        }

        @Test
        fun `should fail when red exercise not found`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(prog.copy(redCurrentExerciseId = 99))
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(failure(MatchError.ExerciseNotFound), result)
        }

        @Test
        fun `should fail when blue exercise not found`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(prog.copy(blueCurrentExerciseId = 99))
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(failure(MatchError.ExerciseNotFound), result)
        }

        @Test
        fun `should fail when updateMatchProgress returns null`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(prog)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            // Stubbing leniente com any()
            lenient().whenever(repoMatch.updateMatchProgress(any(), any(), any())).thenReturn(null)
            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(failure(MatchError.ProgressNotFound), result)
        }

        @Test
        fun `should update and finish when red completes`() {
            val progLast = prog.copy(redCurrentExerciseId = 3, redCurrentReps = 8)
            val updatedProg =
                progLast.copy(
                    redCurrentExerciseId = null,
                    redCurrentReps = 10,
                    redFinishedAt = now,
                    updatedAt = now,
                )
            val finishedMatch =
                match.copy(
                    status = MatchStatus.RUNNING,
                    winnerAthleteId = match.athleteRedId,
                )

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progLast)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())

            // Stubbing genérico com doReturn e any() – sem argumentos concretos
            doReturn(updatedProg)
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
            doReturn(finishedMatch).whenever(repoMatch).save(any<Match>())

            val result = service.updateAthletesReps(1, 10, null)
            assertEquals(success(updatedProg), result)
            verify(repoMatch).save(finishedMatch)
        }

        @Test
        fun `should update and finish when blue completes`() {
            val progLast =
                prog.copy(
                    blueCurrentExerciseId = 3,
                    blueCurrentReps = 9,
                )
            val updatedProg =
                progLast.copy(
                    blueCurrentExerciseId = null,
                    blueCurrentReps = 10,
                    blueFinishedAt = now,
                    updatedAt = now,
                )
            val finishedMatch =
                match.copy(
                    status = MatchStatus.RUNNING,
                    winnerAthleteId = match.athleteBlueId,
                )

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progLast)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())

            doReturn(updatedProg)
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
            doReturn(finishedMatch).whenever(repoMatch).save(any<Match>())

            val result = service.updateAthletesReps(1, null, 10)
            assertEquals(success(updatedProg), result)
            verify(repoMatch).save(finishedMatch)
        }

        @Test
        fun `should set FINISHED only when both athletes finish`() {
            val progLast =
                prog.copy(
                    redCurrentExerciseId = 3,
                    blueCurrentExerciseId = 3,
                    redCurrentReps = 8,
                    blueCurrentReps = 8,
                )
            val updatedProg =
                progLast.copy(
                    redCurrentExerciseId = null,
                    blueCurrentExerciseId = null,
                    redCurrentReps = 10,
                    blueCurrentReps = 10,
                    redFinishedAt = now,
                    blueFinishedAt = now,
                    updatedAt = now,
                )
            val finishedMatch =
                match.copy(
                    status = MatchStatus.FINISHED,
                    winnerAthleteId = match.athleteRedId,
                    finishedAt = now,
                )

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progLast)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())

            doAnswer { invocation -> invocation.getArgument<MatchProgress>(0) }
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
            doReturn(finishedMatch).whenever(repoMatch).save(any<Match>())

            val result = service.updateAthletesReps(1, 10, 10)

            assertEquals(success(updatedProg), result)
            verify(repoMatch).save(finishedMatch)
        }

        @Test
        fun `should update without finishing`() {
            val updatedProg = prog.copy(redCurrentReps = 5, blueCurrentReps = 3, updatedAt = now)
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(prog)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            lenient().whenever(repoMatch.updateMatchProgress(any(), any(), any())).thenReturn(updatedProg)

            val result = service.updateAthletesReps(1, 5, 3)
            assertEquals(success(updatedProg), result)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should still advance the other athlete after the first one finishes`() {
            val progRedDone =
                prog.copy(
                    redCurrentExerciseId = null,
                    redCurrentReps = 10,
                    redFinishedAt = now,
                )
            val updatedProg =
                progRedDone.copy(
                    blueCurrentReps = 5,
                    updatedAt = now,
                )

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progRedDone)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            doAnswer { invocation -> invocation.getArgument<MatchProgress>(0) }
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())

            // Frontend sends both sides; red is already finished (current exercise null)
            val result = service.updateAthletesReps(1, 10, 5)

            assertEquals(success(updatedProg), result)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should save match finishedAt only when both finished and use the last finisher's time`() {
            val redFinishedAt = now.minusSeconds(5)
            val progRedDone =
                prog.copy(
                    redCurrentExerciseId = null,
                    redCurrentReps = 10,
                    redFinishedAt = redFinishedAt,
                    blueCurrentExerciseId = 3,
                    blueCurrentReps = 9,
                )
            val updatedProg =
                progRedDone.copy(
                    blueCurrentExerciseId = null,
                    blueCurrentReps = 10,
                    blueFinishedAt = now,
                    updatedAt = now,
                )
            val finishedMatch =
                match.copy(
                    status = MatchStatus.FINISHED,
                    winnerAthleteId = match.athleteRedId,
                    finishedAt = now,
                )

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progRedDone)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            doAnswer { invocation -> invocation.getArgument<MatchProgress>(0) }
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
            doReturn(finishedMatch).whenever(repoMatch).save(any<Match>())

            val result = service.updateAthletesReps(1, null, 10)

            assertEquals(success(updatedProg), result)
            verify(repoMatch).save(finishedMatch)
        }

        @Test
        fun `should fail when saving finished match returns null`() {
            val progLast = prog.copy(redCurrentExerciseId = 3, redCurrentReps = 8)
            val updatedProg =
                progLast.copy(
                    redCurrentExerciseId = null,
                    redCurrentReps = 10,
                    redFinishedAt = now,
                    updatedAt = now,
                )

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progLast)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(exs)

            doReturn(updatedProg)
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
            doReturn(null).whenever(repoMatch).save(any<Match>())

            val result = service.updateAthletesReps(1, 10, null)
            assertEquals(failure(MatchError.MatchNotFound), result)
        }
    }

    @Nested
    inner class ForceFinishSide {
        private val match = mockMatch(status = MatchStatus.RUNNING)
        private val progBlueDone =
            mockProgress().copy(
                blueCurrentExerciseId = null,
                blueCurrentReps = 10,
                blueFinishedAt = now.minusSeconds(10),
            )

        private fun stubHappyPath() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progBlueDone)
            doAnswer { invocation -> invocation.getArgument<MatchProgress>(0) }
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
        }

        @Test
        fun `should fail when match not found`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when match not running`() {
            whenever(repoMatch.findById(1)).thenReturn(mockMatch(status = MatchStatus.PENDING))

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.MatchNotRunning), result)
            verify(repoMatch, never()).findProgressByMatchId(any())
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should fail when progress not found`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.ProgressNotFound), result)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should fail when opponent has not finished`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(mockProgress())

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.OpponentNotFinished), result)
            verify(repoMatch, never()).save(any())
            verify(repoMatch, never()).updateMatchProgress(any(), any(), any())
        }

        @Test
        fun `should force finish red and blue wins when blue already finished`() {
            stubHappyPath()
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            val expectedProg =
                progBlueDone.copy(
                    redCurrentExerciseId = null,
                    redFinishedAt = now,
                    updatedAt = now,
                )
            val finishedMatch =
                match.copy(
                    status = MatchStatus.FINISHED,
                    winnerAthleteId = match.athleteBlueId,
                    finishedAt = now,
                )
            whenever(repoMatch.save(any())).thenReturn(finishedMatch)

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(success(expectedProg), result)
            verify(repoMatch).save(finishedMatch)
        }

        @Test
        fun `should force finish blue and red wins when red already finished`() {
            val progRedDone =
                mockProgress().copy(
                    redCurrentExerciseId = null,
                    redCurrentReps = 10,
                    redFinishedAt = now.minusSeconds(10),
                )
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progRedDone)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            doAnswer { invocation -> invocation.getArgument<MatchProgress>(0) }
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())

            val expectedProg =
                progRedDone.copy(
                    blueCurrentExerciseId = null,
                    blueFinishedAt = now,
                    updatedAt = now,
                )
            val finishedMatch =
                match.copy(
                    status = MatchStatus.FINISHED,
                    winnerAthleteId = match.athleteRedId,
                    finishedAt = now,
                )
            whenever(repoMatch.save(any())).thenReturn(finishedMatch)

            val result = service.forceFinishSide(1, RepSide.BLUE)

            assertEquals(success(expectedProg), result)
            verify(repoMatch).save(finishedMatch)
        }

        @Test
        fun `should fail when saving finished match returns null`() {
            stubHappyPath()
            doReturn(null).whenever(repoMatch).save(any<Match>())

            val result = service.forceFinishSide(1, RepSide.RED)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }
    }

    @Nested
    inner class GetMatchById {
        @Test
        fun `should fail when not found`() {
            whenever(repoMatch.findById(1)).thenReturn(null)
            val result = service.getMatchById(1)
            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should succeed`() {
            val match = mockMatch()
            whenever(repoMatch.findById(1)).thenReturn(match)
            val result = service.getMatchById(1)
            assertEquals(success(match), result)
        }
    }

    @Nested
    inner class GetMatchesByBracket {
        @Test
        fun `should fail when bracket not found`() {
            whenever(repoTournament.findByBracketId(1)).thenReturn(null)
            val result = service.getMatchesByBracket(1)
            assertEquals(failure(MatchError.BracketNotFound), result)
        }

        @Test
        fun `should succeed with list`() {
            val bracket = mockBracket()
            val matches = listOf(mockMatch(1), mockMatch(2))
            whenever(repoTournament.findByBracketId(1)).thenReturn(bracket)
            whenever(repoMatch.findByBracketId(1)).thenReturn(matches)
            val result = service.getMatchesByBracket(1)
            assertEquals(success(matches), result)
        }
    }

    @Nested
    inner class GetMatchProgress {
        @Test
        fun `should fail when match not found`() {
            whenever(repoMatch.findById(1)).thenReturn(null)
            val result = service.getMatchProgress(1)
            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when progress not found`() {
            whenever(repoMatch.findById(1)).thenReturn(mockMatch())
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            val result = service.getMatchProgress(1)
            assertEquals(failure(MatchError.ProgressNotFound), result)
        }

        @Test
        fun `should succeed`() {
            val progress = mockProgress()
            whenever(repoMatch.findById(1)).thenReturn(mockMatch())
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progress)
            val result = service.getMatchProgress(1)
            assertEquals(success(progress), result)
        }
    }

    @Nested
    inner class SupersetAdvance {
        private val match = mockMatch(status = MatchStatus.RUNNING)
        private val supersetExs =
            listOf(
                Exercise(1, 2, "Push Up", 20, null, 1, null, ExerciseType.NORMAL),
                Exercise(2, 2, "Pull ups", 10, null, 2, null, ExerciseType.NORMAL),
                Exercise(3, 2, "Squats", 30, null, 3, null, ExerciseType.NORMAL),
                Exercise(4, 2, "Muscle Up", 2, null, 4, 1, ExerciseType.SUPERSET),
                Exercise(5, 2, "Straight Bar Dip", 10, null, 4, 2, ExerciseType.SUPERSET),
                Exercise(6, 2, "Pull up", 10, null, 4, 3, ExerciseType.SUPERSET),
            )

        private fun progressOn(
            exerciseId: Int,
            reps: Int,
        ) = mockProgress().copy(redCurrentExerciseId = exerciseId, redCurrentReps = reps)

        private fun realAdvance() {
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(2)).thenReturn(supersetExs)
            whenever(repoTournament.findByBracketId(1)).thenReturn(mockBracket())
            doAnswer { invocation -> invocation.getArgument<MatchProgress>(0) }
                .whenever(repoMatch)
                .updateMatchProgress(any<MatchProgress>(), anyOrNull<Int>(), anyOrNull<Int>())
        }

        private fun expectSuccess(result: Either<MatchError, MatchProgress>): MatchProgress =
            when (result) {
                is Either.Right -> result.value
                is Either.Left -> throw AssertionError("Expected success but got ${result.value}")
            }

        @Test
        fun `should advance to second superset exercise after first completes`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progressOn(4, 1))
            realAdvance()

            val progress = expectSuccess(service.updateAthletesReps(1, 2, null))

            assertEquals(5, progress.redCurrentExerciseId)
            assertEquals(0, progress.redCurrentReps)
            assertEquals(null, progress.redFinishedAt)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should advance to third superset exercise after second completes`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progressOn(5, 9))
            realAdvance()

            val progress = expectSuccess(service.updateAthletesReps(1, 10, null))

            assertEquals(6, progress.redCurrentExerciseId)
            assertEquals(0, progress.redCurrentReps)
            assertEquals(null, progress.redFinishedAt)
            verify(repoMatch, never()).save(any())
        }

        @Test
        fun `should finish only after last superset exercise completes`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progressOn(6, 9))
            realAdvance()
            val finishedMatch =
                match.copy(
                    status = MatchStatus.RUNNING,
                    winnerAthleteId = match.athleteRedId,
                )
            doReturn(finishedMatch).whenever(repoMatch).save(any<Match>())

            val progress = expectSuccess(service.updateAthletesReps(1, 10, null))

            assertEquals(null, progress.redCurrentExerciseId)
            assertEquals(10, progress.redCurrentReps)
            assertEquals(now, progress.redFinishedAt)
            verify(repoMatch).save(finishedMatch)
        }
    }
}
