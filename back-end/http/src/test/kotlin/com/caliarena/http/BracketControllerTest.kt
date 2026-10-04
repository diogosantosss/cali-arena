package com.caliarena.http

import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.bracket.BracketSummary
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.tournament.CreateBracketInput
import com.caliarena.service.ApiError
import com.caliarena.service.BracketService
import com.caliarena.service.failure
import com.caliarena.service.success
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class BracketControllerTest {
    @Mock
    private lateinit var bracketService: BracketService

    private lateinit var controller: BracketController

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = BracketController(bracketService)
    }

    private fun bracket(
        id: Int = 1,
        tournamentId: Int = 10,
        division: String = "Open",
        stage: BracketStage = BracketStage.QUALIFIERS,
    ) = Bracket(id, tournamentId, division, stage, now)

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
    inner class CreateBracket {
        private val input = CreateBracketInput(tournamentId = 10, division = "Open", stage = "QUALIFIERS")

        @Test
        fun `should create bracket successfully`() {
            val created = bracket()
            whenever(bracketService.createBracket(10, "Open", "QUALIFIERS")).thenReturn(success(created))

            val response = controller.createBracket(input)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/brackets/1", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(bracketService).createBracket(10, "Open", "QUALIFIERS")
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(bracketService.createBracket(99, "Open", "QUALIFIERS")).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.createBracket(CreateBracketInput(tournamentId = 99, division = "Open", stage = "QUALIFIERS"))

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return bad request when division is invalid`() {
            whenever(bracketService.createBracket(10, "  ", "QUALIFIERS")).thenReturn(failure(ApiError.INVALID_BRACKET_DIVISION))

            val response = controller.createBracket(CreateBracketInput(tournamentId = 10, division = "  ", stage = "QUALIFIERS"))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-bracket-division")
        }

        @Test
        fun `should return bad request when stage is invalid`() {
            whenever(bracketService.createBracket(10, "Open", "INVALID")).thenReturn(failure(ApiError.INVALID_BRACKET_STAGE))

            val response = controller.createBracket(CreateBracketInput(tournamentId = 10, division = "Open", stage = "INVALID"))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-bracket-stage")
        }

        @Test
        fun `should return conflict when bracket already exists`() {
            whenever(bracketService.createBracket(10, "Open", "QUALIFIERS")).thenReturn(failure(ApiError.BRACKET_ALREADY_EXISTS))

            val response = controller.createBracket(input)

            assertProblem(response, HttpStatus.CONFLICT, "bracket-already-exists")
        }
    }

    @Nested
    inner class GetBracketsByTournament {
        @Test
        fun `should get brackets by tournament`() {
            whenever(bracketService.getBracketsByTournament(10)).thenReturn(success(listOf(bracket())))

            val response = controller.getBracketsByTournamentId(10)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(bracket()), response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(bracketService.getBracketsByTournament(99)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getBracketsByTournamentId(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }
    }

    @Nested
    inner class GetBracketById {
        @Test
        fun `should get bracket by id`() {
            whenever(bracketService.getBracketById(1)).thenReturn(success(bracket()))

            val response = controller.getBracketById(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(bracket(), response.body)
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(bracketService.getBracketById(99)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.getBracketById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }
    }

    @Nested
    inner class GetBracketsByTournamentAndDivision {
        @Test
        fun `should get brackets by tournament and division`() {
            whenever(bracketService.getBracketsByTournamentAndDivision(10, "Open")).thenReturn(success(listOf(bracket())))

            val response = controller.getBracketsByTournamentAndDivision(10, "Open")

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(bracket()), response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(bracketService.getBracketsByTournamentAndDivision(99, "Open")).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getBracketsByTournamentAndDivision(99, "Open")

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return bad request when division is invalid`() {
            whenever(bracketService.getBracketsByTournamentAndDivision(10, "  ")).thenReturn(failure(ApiError.INVALID_BRACKET_DIVISION))

            val response = controller.getBracketsByTournamentAndDivision(10, "  ")

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-bracket-division")
        }
    }

    @Nested
    inner class GetBracketOverview {
        @Test
        fun `should get bracket overview`() {
            val overview = BracketOverview(bracket = bracket(), matches = emptyList())
            whenever(bracketService.getBracketOverview(10, "Open")).thenReturn(success(listOf(overview)))

            val response = controller.getBracketOverview(10, "Open")

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(overview), response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(bracketService.getBracketOverview(99, "Open")).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getBracketOverview(99, "Open")

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return bad request when division is invalid`() {
            whenever(bracketService.getBracketOverview(10, "  ")).thenReturn(failure(ApiError.INVALID_BRACKET_DIVISION))

            val response = controller.getBracketOverview(10, "  ")

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-bracket-division")
        }
    }

    @Nested
    inner class GetBracketLeaderboard {
        @Test
        fun `should get bracket leaderboard`() {
            val leaderboard = BracketLeaderboard(bracketId = 1, division = "Open", stage = BracketStage.QUALIFIERS, entries = emptyList())
            whenever(bracketService.getBracketLeaderboard(1)).thenReturn(success(leaderboard))

            val response = controller.getBracketLeaderboard(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(leaderboard, response.body)
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(bracketService.getBracketLeaderboard(99)).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.getBracketLeaderboard(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }
    }

    @Nested
    inner class GetBracketsSummary {
        @Test
        fun `should get brackets summary`() {
            val summary =
                TournamentBracketsResponse(
                    tournamentId = 10,
                    division = "Open",
                    brackets = listOf(BracketSummary(BracketStage.QUALIFIERS, emptyList())),
                )
            whenever(bracketService.getTournamentBracketsSummary(10, "Open")).thenReturn(success(summary))

            val response = controller.getBracketsSummary(10, "Open")

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(summary, response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(bracketService.getTournamentBracketsSummary(99, "Open")).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getBracketsSummary(99, "Open")

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return bad request when division is invalid`() {
            whenever(bracketService.getTournamentBracketsSummary(10, "  ")).thenReturn(failure(ApiError.INVALID_BRACKET_DIVISION))

            val response = controller.getBracketsSummary(10, "  ")

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-bracket-division")
        }
    }
}
