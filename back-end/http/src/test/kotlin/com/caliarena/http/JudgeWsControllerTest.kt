package com.caliarena.http

import com.caliarena.domain.match.JudgeActionInput
import com.caliarena.domain.match.JudgeActionType
import com.caliarena.domain.match.JudgeErrorEvent
import com.caliarena.domain.match.JudgeFinishedEvent
import com.caliarena.domain.match.JudgeRepsEvent
import com.caliarena.domain.match.JudgeStartedEvent
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.RepSide
import com.caliarena.domain.match.StartedMatch
import com.caliarena.service.ApiError
import com.caliarena.service.MatchService
import com.caliarena.service.failure
import com.caliarena.service.success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class JudgeWsControllerTest {
    @Mock
    private lateinit var matchService: MatchService

    @Mock
    private lateinit var messaging: SimpMessagingTemplate

    private lateinit var controller: JudgeWsController

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = JudgeWsController(matchService, messaging)
    }

    private fun match(id: Int = 1) =
        Match(
            id = id,
            bracketId = 10,
            routineId = 5,
            athleteRedId = 1,
            athleteBlueId = 2,
            winnerAthleteId = null,
            status = MatchStatus.RUNNING,
            startedAt = now,
            finishedAt = null,
            createdAt = now,
        )

    private fun progress(
        redReps: Int = 0,
        blueReps: Int = 0,
        redFinishedAt: Instant? = null,
        blueFinishedAt: Instant? = null,
        redExerciseId: Int? = null,
        blueExerciseId: Int? = null,
    ) = MatchProgress(
        id = 1,
        matchId = 1,
        redCurrentExerciseId = redExerciseId,
        blueCurrentExerciseId = blueExerciseId,
        redCurrentReps = redReps,
        blueCurrentReps = blueReps,
        redFinishedAt = redFinishedAt,
        blueFinishedAt = blueFinishedAt,
        updatedAt = now,
    )

    private fun action(
        type: JudgeActionType,
        side: RepSide,
        reps: Int? = null,
    ) = JudgeActionInput(type, side, reps)

    private fun assertError(
        topic: String,
        problemType: String,
    ) {
        val captor = argumentCaptor<JudgeErrorEvent>()
        verify(messaging).convertAndSend(eq(topic), captor.capture())
        assertEquals(JudgeErrorEvent(message = problemType), captor.firstValue)
    }

    @Nested
    inner class Start {
        @Test
        fun `should broadcast started event on start action`() {
            val started = StartedMatch(match = match(), progress = progress())
            whenever(matchService.startMatch(1)).thenReturn(success(started))

            controller.onJudgeAction(1, action(JudgeActionType.START, RepSide.RED))

            verify(messaging).convertAndSend("/topic/matches/1", JudgeStartedEvent(match = match(), progress = progress()))
        }

        @Test
        fun `should broadcast error event when match does not exist`() {
            whenever(matchService.startMatch(1)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.START, RepSide.RED))

            assertError("/topic/matches/1", "match-not-found")
        }

        @Test
        fun `should broadcast error event when bracket does not exist`() {
            whenever(matchService.startMatch(1)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.START, RepSide.RED))

            assertError("/topic/matches/1", "bracket-not-found")
        }

        @Test
        fun `should broadcast error event when athletes are not assigned`() {
            whenever(matchService.startMatch(1)).thenReturn(failure(ApiError.ATHLETES_NOT_ASSIGNED))

            controller.onJudgeAction(1, action(JudgeActionType.START, RepSide.RED))

            assertError("/topic/matches/1", "athletes-not-assigned")
        }

        @Test
        fun `should broadcast error event when match is already started`() {
            whenever(matchService.startMatch(1)).thenReturn(failure(ApiError.MATCH_ALREADY_STARTED))

            controller.onJudgeAction(1, action(JudgeActionType.START, RepSide.RED))

            assertError("/topic/matches/1", "match-already-started")
        }

        @Test
        fun `should broadcast error event when progress does not exist`() {
            whenever(matchService.startMatch(1)).thenReturn(failure(ApiError.PROGRESS_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.START, RepSide.RED))

            assertError("/topic/matches/1", "progress-not-found")
        }
    }

    @Nested
    inner class Adjust {
        @Test
        fun `should broadcast reps event on adjust of red side`() {
            val updated = progress(redReps = 3, redExerciseId = 5)
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(success(updated))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            verify(messaging).convertAndSend("/topic/matches/1", JudgeRepsEvent(side = RepSide.RED, reps = 3, exerciseId = 5))
        }

        @Test
        fun `should broadcast reps event on adjust of blue side`() {
            val updated = progress(blueReps = 4, blueExerciseId = 6)
            whenever(matchService.updateAthletesReps(1, blueReps = 4)).thenReturn(success(updated))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.BLUE, reps = 4))

            verify(messaging).convertAndSend("/topic/matches/1", JudgeRepsEvent(side = RepSide.BLUE, reps = 4, exerciseId = 6))
        }

        @Test
        fun `should broadcast finished event when adjust completes the side`() {
            val updated = progress(redReps = 10, redFinishedAt = now, redExerciseId = 5)
            whenever(matchService.updateAthletesReps(1, redReps = 10)).thenReturn(success(updated))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 10))

            verify(messaging).convertAndSend("/topic/matches/1", JudgeRepsEvent(side = RepSide.RED, reps = 10, exerciseId = 5))
            verify(messaging).convertAndSend("/topic/matches/1", JudgeFinishedEvent(side = RepSide.RED, finishedAt = now))
        }

        @Test
        fun `should broadcast error event when match does not exist`() {
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            assertError("/topic/matches/1", "match-not-found")
        }

        @Test
        fun `should broadcast error event when match is not running`() {
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(failure(ApiError.MATCH_NOT_RUNNING))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            assertError("/topic/matches/1", "match-not-running")
        }

        @Test
        fun `should broadcast error event when progress does not exist`() {
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(failure(ApiError.PROGRESS_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            assertError("/topic/matches/1", "progress-not-found")
        }

        @Test
        fun `should broadcast error event when athlete is not in match`() {
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(failure(ApiError.ATHLETE_NOT_IN_MATCH))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            assertError("/topic/matches/1", "athlete-not-in-match")
        }

        @Test
        fun `should broadcast error event when exercise does not exist`() {
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(failure(ApiError.EXERCISE_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            assertError("/topic/matches/1", "exercise-not-found")
        }

        @Test
        fun `should broadcast error event when bracket does not exist`() {
            whenever(matchService.updateAthletesReps(1, redReps = 3)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.ADJUST, RepSide.RED, reps = 3))

            assertError("/topic/matches/1", "bracket-not-found")
        }
    }

    @Nested
    inner class Finish {
        @Test
        fun `should broadcast finished event when side is finished`() {
            val finished = progress(redReps = 10, redFinishedAt = now)
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(success(finished))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            verify(messaging).convertAndSend("/topic/matches/1", JudgeFinishedEvent(side = RepSide.RED, finishedAt = now))
        }

        @Test
        fun `should not broadcast when finish does not finish the side`() {
            val notFinished = progress(blueReps = 5, blueFinishedAt = null)
            whenever(matchService.forceFinishSide(1, RepSide.BLUE)).thenReturn(success(notFinished))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.BLUE))

            verifyNoInteractions(messaging)
        }

        @Test
        fun `should broadcast error event when match does not exist`() {
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            assertError("/topic/matches/1", "match-not-found")
        }

        @Test
        fun `should broadcast error event when match is not running`() {
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(failure(ApiError.MATCH_NOT_RUNNING))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            assertError("/topic/matches/1", "match-not-running")
        }

        @Test
        fun `should broadcast error event when progress does not exist`() {
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(failure(ApiError.PROGRESS_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            assertError("/topic/matches/1", "progress-not-found")
        }

        @Test
        fun `should broadcast error event when athlete is not in match`() {
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(failure(ApiError.ATHLETE_NOT_IN_MATCH))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            assertError("/topic/matches/1", "athlete-not-in-match")
        }

        @Test
        fun `should broadcast error event when opponent is not finished`() {
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(failure(ApiError.OPPONENT_NOT_FINISHED))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            assertError("/topic/matches/1", "opponent-not-finished")
        }

        @Test
        fun `should broadcast error event when bracket does not exist`() {
            whenever(matchService.forceFinishSide(1, RepSide.RED)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            controller.onJudgeAction(1, action(JudgeActionType.FINISH, RepSide.RED))

            assertError("/topic/matches/1", "bracket-not-found")
        }
    }
}
