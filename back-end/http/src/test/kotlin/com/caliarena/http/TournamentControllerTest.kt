package com.caliarena.http

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.tournament.CreateTournamentInput
import com.caliarena.http.model.tournament.UpdateScreenInput
import com.caliarena.http.model.tournament.UpdateTournamentStatusInput
import com.caliarena.service.ApiError
import com.caliarena.service.TournamentService
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
class TournamentControllerTest {
    @Mock
    private lateinit var tournamentService: TournamentService

    private lateinit var controller: TournamentController

    private val authUser = authenticatedUser()

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = TournamentController(tournamentService)
    }

    private fun tournament(
        id: Int = 1,
        name: String = "Ultimate Tournament",
        location: String? = "Lisbon",
        startDate: Instant? = now,
        endDate: Instant? = now,
    ) = Tournament(id, name, location, startDate, endDate, TournamentStatus.DRAFT, now)

    private fun tournamentState(id: Int = 1) =
        TournamentState(
            id = id,
            tournamentId = 1,
            currentScreen = ScreenState.WAITING,
            currentMatchId = null,
            currentBracketId = null,
            currentDivision = null,
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
    inner class CreateTournament {
        @Test
        fun `should create tournament successfully`() {
            val expectedStart = Instant.parse("2025-01-01T00:00:00Z")
            val expectedEnd = Instant.parse("2025-01-02T00:00:00Z")
            val created = tournament(id = 1, startDate = expectedStart, endDate = expectedEnd)

            whenever(
                tournamentService.createTournament("Ultimate Tournament", "Lisbon", expectedStart, expectedEnd),
            ).thenReturn(success(created))

            val response =
                controller.createTournament(
                    CreateTournamentInput(
                        name = "Ultimate Tournament",
                        location = "Lisbon",
                        startDate = "2025-01-01",
                        endDate = "2025-01-02",
                    ),
                )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/tournaments/1", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(tournamentService).createTournament("Ultimate Tournament", "Lisbon", expectedStart, expectedEnd)
        }

        @Test
        fun `should create tournament without dates`() {
            val created = tournament(id = 1, startDate = null, endDate = null)

            whenever(tournamentService.createTournament("Ultimate Tournament", null, null, null))
                .thenReturn(success(created))

            val response =
                controller.createTournament(
                    CreateTournamentInput(name = "Ultimate Tournament", location = null, startDate = null, endDate = null),
                )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            verify(tournamentService).createTournament("Ultimate Tournament", null, null, null)
        }

        @Test
        fun `should return conflict when tournament already exists`() {
            whenever(tournamentService.createTournament("Ultimate Tournament", null, null, null))
                .thenReturn(failure(ApiError.TOURNAMENT_ALREADY_EXISTS))

            val response =
                controller.createTournament(
                    CreateTournamentInput(name = "Ultimate Tournament", location = null, startDate = null, endDate = null),
                )

            assertProblem(response, HttpStatus.CONFLICT, "tournament-already-exists")
        }
    }

    @Nested
    inner class GetAllTournaments {
        @Test
        fun `should get all tournaments`() {
            whenever(tournamentService.getAllTournaments(authUser.user)).thenReturn(listOf(tournament()))

            val response = controller.getTournaments(authUser)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(tournament()), response.body)
        }
    }

    @Nested
    inner class GetTournamentById {
        @Test
        fun `should get tournament by id`() {
            whenever(tournamentService.getTournamentById(1)).thenReturn(success(tournament()))

            val response = controller.getTournamentById(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("/api/tournaments/1", response.headers.getFirst("Location"))
            assertEquals(tournament(), response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(tournamentService.getTournamentById(99)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getTournamentById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }
    }

    @Nested
    inner class GetTournamentState {
        @Test
        fun `should get tournament state`() {
            val state = tournamentState()
            whenever(tournamentService.getTournamentState(1)).thenReturn(success(state))

            val response = controller.getTournamentState(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(state, response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(tournamentService.getTournamentState(99)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getTournamentState(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return not found when tournament state does not exist`() {
            whenever(tournamentService.getTournamentState(99)).thenReturn(failure(ApiError.TOURNAMENT_STATE_NOT_FOUND))

            val response = controller.getTournamentState(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-state-not-found")
        }
    }

    @Nested
    inner class UpdateScreen {
        private val input =
            UpdateScreenInput(screen = "BATTLE", currentMatchId = 1, currentBracketId = null, currentDivision = null)

        @Test
        fun `should update screen successfully`() {
            val state = tournamentState()
            whenever(tournamentService.updateScreen(authUser.user, 1, "BATTLE", 1, null, null)).thenReturn(success(state))

            val response = controller.updateScreen(authUser, 1, input)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(state, response.body)
            verify(tournamentService).updateScreen(authUser.user, 1, "BATTLE", 1, null, null)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(
                tournamentService.updateScreen(authUser.user, 99, "BATTLE", 1, null, null),
            ).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.updateScreen(authUser, 99, input)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return bad request when screen state is invalid`() {
            whenever(
                tournamentService.updateScreen(authUser.user, 1, "BATTLE", 1, null, null),
            ).thenReturn(failure(ApiError.INVALID_SCREEN_STATE))

            val response = controller.updateScreen(authUser, 1, input)

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-screen-state")
        }

        @Test
        fun `should return not found when bracket does not exist`() {
            whenever(
                tournamentService.updateScreen(authUser.user, 1, "BATTLE", 1, null, null),
            ).thenReturn(failure(ApiError.BRACKET_NOT_FOUND))

            val response = controller.updateScreen(authUser, 1, input)

            assertProblem(response, HttpStatus.NOT_FOUND, "bracket-not-found")
        }

        @Test
        fun `should return bad request when division is invalid`() {
            whenever(
                tournamentService.updateScreen(authUser.user, 1, "BATTLE", 1, null, null),
            ).thenReturn(failure(ApiError.INVALID_BRACKET_DIVISION))

            val response = controller.updateScreen(authUser, 1, input)

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-bracket-division")
        }

        @Test
        fun `should return not found when tournament state does not exist`() {
            whenever(
                tournamentService.updateScreen(authUser.user, 1, "BATTLE", 1, null, null),
            ).thenReturn(failure(ApiError.TOURNAMENT_STATE_NOT_FOUND))

            val response = controller.updateScreen(authUser, 1, input)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-state-not-found")
        }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `should update status successfully`() {
            val updated = tournament().copy(status = TournamentStatus.LIVE)
            whenever(tournamentService.updateTournamentStatus(authUser.user, 1, "LIVE")).thenReturn(success(updated))

            val response = controller.updateTournamentStatus(authUser, 1, UpdateTournamentStatusInput(status = "LIVE"))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(updated, response.body)
        }

        @Test
        fun `should return forbidden when the user does not host the tournament`() {
            whenever(tournamentService.updateTournamentStatus(authUser.user, 1, "LIVE")).thenReturn(failure(ApiError.NOT_AUTHORIZED))

            val response = controller.updateTournamentStatus(authUser, 1, UpdateTournamentStatusInput(status = "LIVE"))

            assertProblem(response, HttpStatus.FORBIDDEN, "not-authorized")
        }

        @Test
        fun `should return bad request when the status is invalid`() {
            whenever(
                tournamentService.updateTournamentStatus(authUser.user, 1, "NOPE"),
            ).thenReturn(failure(ApiError.INVALID_TOURNAMENT_STATUS))

            val response = controller.updateTournamentStatus(authUser, 1, UpdateTournamentStatusInput(status = "NOPE"))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-tournament-status")
        }
    }

    @Nested
    inner class DeleteTournament {
        @Test
        fun `should delete tournament successfully`() {
            whenever(tournamentService.deleteTournament(1)).thenReturn(success(Unit))

            val response = controller.deleteTournament(1)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            assertEquals(null, response.body)
            verify(tournamentService).deleteTournament(1)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(tournamentService.deleteTournament(99)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.deleteTournament(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }
    }
}
