package com.caliarena.http

import com.caliarena.domain.match.JudgeStartedEvent
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.match.StartedMatch
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.match.CreateMatchInput
import com.caliarena.http.model.match.UpdateRepsInput
import com.caliarena.service.ApiError
import com.caliarena.service.MatchService
import com.caliarena.service.failure
import com.caliarena.service.success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class MatchControllerTest {
    @Mock
    private lateinit var matchService: MatchService

    @Mock
    private lateinit var messaging: SimpMessagingTemplate

    private lateinit var controller: MatchController

    private val authUser = authenticatedUser()

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = MatchController(matchService, messaging)
    }

    private fun match(
        id: Int = 1,
        status: MatchStatus = MatchStatus.PENDING,
    ) = Match(
        id = id,
        bracketId = 10,
        routineId = 5,
        athleteRedId = 1,
        athleteBlueId = 2,
        winnerAthleteId = null,
        status = status,
        startedAt = null,
        finishedAt = null,
        createdAt = now,
    )

    private fun progress(
        redReps: Int = 0,
        blueReps: Int = 0,
    ) = MatchProgress(
        id = 1,
        matchId = 1,
        redCurrentReps = redReps,
        blueCurrentReps = blueReps,
        updatedAt = now,
    )

    private fun assertProblem(
        response: ResponseEntity<Any>,
        status: HttpStatus,
        problemType: String,
    ) {
        assertEquals(status, response.statusCode)
        assertEquals(PROBLEM_MEDIA_TYPE, response.headers.contentType?.toString())
        assertEquals(ProblemBody(type = "/problem/$problemType", title = problemType, status = status.value()), response.body)
    }

    @Nested
    inner class CreateMatch {
        private val input = CreateMatchInput(bracketId = 10, routineId = 5, athleteRedId = 1, athleteBlueId = 2)

        @Test
        fun `should create match successfully`() {
            val created = match(id = 1)
            whenever(matchService.createMatch(authUser.user, 10, 5, 1, 2)).thenReturn(success(created))

            val response = controller.createMatch(authUser, input)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/matches/1", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(matchService).createMatch(authUser.user, 10, 5, 1, 2)
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(matchService.createMatch(authUser.user, 99, 5, 1, 2)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.createMatch(authUser, input.copy(bracketId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }

        @Test
        fun `should return not found when routine does not exist`() {
            whenever(matchService.createMatch(authUser.user, 10, 99, 1, 2)).thenReturn(failure(ApiError.ROUTINE_NOT_FOUND))

            val response = controller.createMatch(authUser, input.copy(routineId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "routine-not-found")
        }

        @Test
        fun `should return bad request when athletes are not assigned`() {
            whenever(matchService.createMatch(authUser.user, 10, 5, null, null)).thenReturn(failure(ApiError.ATHLETES_NOT_ASSIGNED))

            val response = controller.createMatch(authUser, input.copy(athleteRedId = null, athleteBlueId = null))

            assertProblem(response, HttpStatus.BAD_REQUEST, "athletes-not-assigned")
        }

        @Test
        fun `should return not found when an athlete does not exist`() {
            whenever(matchService.createMatch(authUser.user, 10, 5, 99, 2)).thenReturn(failure(ApiError.ATHLETE_NOT_FOUND))

            val response = controller.createMatch(authUser, input.copy(athleteRedId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "athlete-not-found")
        }

        @Test
        fun `should return bad request when the same athlete is on both sides`() {
            whenever(matchService.createMatch(authUser.user, 10, 5, 1, 1)).thenReturn(failure(ApiError.SAME_ATHLETE_ON_BOTH_SIDES))

            val response = controller.createMatch(authUser, input.copy(athleteBlueId = 1))

            assertProblem(response, HttpStatus.BAD_REQUEST, "same-athlete-on-both-sides")
        }
    }

    @Nested
    inner class StartMatch {
        @Test
        fun `should start match and broadcast started event`() {
            val startedMatch = StartedMatch(match = match(), progress = progress())
            whenever(matchService.startMatch(authUser.user, 1)).thenReturn(success(startedMatch))

            val response = controller.startMatch(authUser, 1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("/api/matches/1", response.headers.getFirst("Location"))
            assertEquals(progress(), response.body)
            verify(messaging).convertAndSend("/topic/matches/1", JudgeStartedEvent(match = match(), progress = progress()))
        }

        @Test
        fun `should return not found when match does not exist`() {
            whenever(matchService.startMatch(authUser.user, 99)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            val response = controller.startMatch(authUser, 99)

            assertProblem(response, HttpStatus.NOT_FOUND, "match-not-found")
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(matchService.startMatch(authUser.user, 1)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.startMatch(authUser, 1)

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }

        @Test
        fun `should return bad request when athletes are not assigned`() {
            whenever(matchService.startMatch(authUser.user, 1)).thenReturn(failure(ApiError.ATHLETES_NOT_ASSIGNED))

            val response = controller.startMatch(authUser, 1)

            assertProblem(response, HttpStatus.BAD_REQUEST, "athletes-not-assigned")
        }

        @Test
        fun `should return conflict when match is already started`() {
            whenever(matchService.startMatch(authUser.user, 1)).thenReturn(failure(ApiError.MATCH_ALREADY_STARTED))

            val response = controller.startMatch(authUser, 1)

            assertProblem(response, HttpStatus.CONFLICT, "match-already-started")
        }

        @Test
        fun `should return not found when progress does not exist`() {
            whenever(matchService.startMatch(authUser.user, 1)).thenReturn(failure(ApiError.PROGRESS_NOT_FOUND))

            val response = controller.startMatch(authUser, 1)

            assertProblem(response, HttpStatus.NOT_FOUND, "progress-not-found")
        }
    }

    @Nested
    inner class UpdateMatchReps {
        @Test
        fun `should update match reps successfully`() {
            val updated = progress(redReps = 5, blueReps = 4)
            whenever(matchService.updateAthletesReps(authUser.user, 1, 5, 4)).thenReturn(success(updated))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput(redReps = 5, blueReps = 4))

            assertEquals(HttpStatus.ACCEPTED, response.statusCode)
            assertEquals("/api/matches/1", response.headers.getFirst("Location"))
            assertEquals(updated, response.body)
            verify(matchService).updateAthletesReps(authUser.user, 1, 5, 4)
        }

        @Test
        fun `should return not found when match does not exist`() {
            whenever(matchService.updateAthletesReps(authUser.user, 1, null, null)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput())

            assertProblem(response, HttpStatus.NOT_FOUND, "match-not-found")
        }

        @Test
        fun `should return conflict when match is not running`() {
            whenever(matchService.updateAthletesReps(authUser.user, 1, null, null)).thenReturn(failure(ApiError.MATCH_NOT_RUNNING))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput())

            assertProblem(response, HttpStatus.CONFLICT, "match-not-running")
        }

        @Test
        fun `should return not found when progress does not exist`() {
            whenever(matchService.updateAthletesReps(authUser.user, 1, null, null)).thenReturn(failure(ApiError.PROGRESS_NOT_FOUND))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput())

            assertProblem(response, HttpStatus.NOT_FOUND, "progress-not-found")
        }

        @Test
        fun `should return bad request when athlete is not in match`() {
            whenever(matchService.updateAthletesReps(authUser.user, 1, 5, null)).thenReturn(failure(ApiError.ATHLETE_NOT_IN_MATCH))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput(redReps = 5))

            assertProblem(response, HttpStatus.BAD_REQUEST, "athlete-not-in-match")
        }

        @Test
        fun `should return not found when exercise does not exist`() {
            whenever(matchService.updateAthletesReps(authUser.user, 1, 5, null)).thenReturn(failure(ApiError.EXERCISE_NOT_FOUND))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput(redReps = 5))

            assertProblem(response, HttpStatus.NOT_FOUND, "exercise-not-found")
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(matchService.updateAthletesReps(authUser.user, 1, 5, null)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.updateMatchReps(authUser, 1, UpdateRepsInput(redReps = 5))

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }
    }

    @Nested
    inner class DeleteMatch {
        @Test
        fun `should delete match successfully`() {
            whenever(matchService.deleteMatch(1)).thenReturn(success(Unit))

            val response = controller.deleteMatch(1)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            assertNull(response.body)
            verify(matchService).deleteMatch(1)
        }

        @Test
        fun `should return not found when match does not exist`() {
            whenever(matchService.deleteMatch(99)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            val response = controller.deleteMatch(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "match-not-found")
        }
    }

    @Nested
    inner class GetAllMatches {
        @Test
        fun `should get all matches`() {
            whenever(matchService.getAllMatches(authUser.user)).thenReturn(success(listOf(match())))

            val response = controller.getAllMatches(authUser)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(match()), response.body)
        }
    }

    @Nested
    inner class GetMatchById {
        @Test
        fun `should get match by id`() {
            whenever(matchService.getMatchById(1)).thenReturn(success(match()))

            val response = controller.getMatchById(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(match(), response.body)
        }

        @Test
        fun `should return not found when match does not exist`() {
            whenever(matchService.getMatchById(99)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            val response = controller.getMatchById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "match-not-found")
        }
    }

    @Nested
    inner class GetMatchProgress {
        @Test
        fun `should get match progress`() {
            whenever(matchService.getMatchProgress(1)).thenReturn(success(progress()))

            val response = controller.getMatchProgressById(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("/api/matches/1/progress", response.headers.getFirst("Location"))
            assertEquals(progress(), response.body)
        }

        @Test
        fun `should return not found when match does not exist`() {
            whenever(matchService.getMatchProgress(99)).thenReturn(failure(ApiError.MATCH_NOT_FOUND))

            val response = controller.getMatchProgressById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "match-not-found")
        }

        @Test
        fun `should return not found when progress does not exist`() {
            whenever(matchService.getMatchProgress(99)).thenReturn(failure(ApiError.PROGRESS_NOT_FOUND))

            val response = controller.getMatchProgressById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "progress-not-found")
        }
    }

    @Nested
    inner class GetMatchesByBracket {
        @Test
        fun `should get matches by bracket`() {
            whenever(matchService.getMatchesByBracket(10)).thenReturn(success(listOf(match())))

            val response = controller.getMatchesByBracketId(10)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(match()), response.body)
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(matchService.getMatchesByBracket(99)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.getMatchesByBracketId(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }
    }
}
