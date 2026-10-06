package com.caliarena.http

import com.caliarena.domain.tournament.MyTournaments
import com.caliarena.domain.tournament.StaffMember
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentStaff
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.tournament.AssignHostInput
import com.caliarena.http.model.tournament.AssignJudgeInput
import com.caliarena.service.ApiError
import com.caliarena.service.TournamentStaffService
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
class TournamentStaffControllerTest {
    @Mock
    private lateinit var staffService: TournamentStaffService

    private lateinit var controller: TournamentStaffController

    private val authUser = authenticatedUser()

    private val hostUser = authenticatedUser(id = 3, username = "host", role = UserRole.HOST)

    private val judgeUser = authenticatedUser(id = 2, username = "santos", role = UserRole.JUDGE)

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = TournamentStaffController(staffService)
    }

    private fun tournament(
        id: Int = 1,
        hostId: Int? = 3,
    ) = Tournament(
        id = id,
        name = "Ultimate Tournament",
        location = "Lisbon",
        startDate = now,
        endDate = null,
        status = TournamentStatus.LIVE,
        createdAt = now,
        hostId = hostId,
    )

    private fun staff(tournamentId: Int = 1) =
        TournamentStaff(
            tournamentId = tournamentId,
            host = StaffMember(id = 3, username = "host", role = UserRole.HOST),
            judges = listOf(StaffMember(id = 2, username = "santos", role = UserRole.JUDGE)),
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
    inner class GetMyTournaments {
        @Test
        fun `should return the tournaments of the acting user`() {
            val mine = MyTournaments(hosting = listOf(tournament(1)), judging = listOf(tournament(2, hostId = null)))
            whenever(staffService.getMyTournaments(hostUser.user)).thenReturn(success(mine))

            val response = controller.getMyTournaments(hostUser)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(mine, response.body)
        }
    }

    @Nested
    inner class GetStaff {
        @Test
        fun `should return the staff of a tournament`() {
            whenever(staffService.getStaff(authUser.user, 1)).thenReturn(success(staff()))

            val response = controller.getStaff(authUser, 1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(staff(), response.body)
        }

        @Test
        fun `should return forbidden when the user is outside the tournament`() {
            whenever(staffService.getStaff(judgeUser.user, 1)).thenReturn(failure(ApiError.NOT_AUTHORIZED))

            val response = controller.getStaff(judgeUser, 1)

            assertProblem(response, HttpStatus.FORBIDDEN, "not-authorized")
        }

        @Test
        fun `should return not found when the tournament does not exist`() {
            whenever(staffService.getStaff(authUser.user, 99)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getStaff(authUser, 99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }
    }

    @Nested
    inner class AssignHost {
        @Test
        fun `should assign the host successfully`() {
            whenever(staffService.assignHost(authUser.user, 1, 3)).thenReturn(success(staff()))

            val response = controller.assignHost(authUser, 1, AssignHostInput(userId = 3))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(staff(), response.body)
            verify(staffService).assignHost(authUser.user, 1, 3)
        }

        @Test
        fun `should return forbidden when the current host tries to hand over`() {
            whenever(staffService.assignHost(hostUser.user, 1, 3)).thenReturn(failure(ApiError.NOT_AUTHORIZED))

            val response = controller.assignHost(hostUser, 1, AssignHostInput(userId = 3))

            assertProblem(response, HttpStatus.FORBIDDEN, "not-authorized")
        }

        @Test
        fun `should return bad request when the user has no host role`() {
            whenever(staffService.assignHost(authUser.user, 1, 2)).thenReturn(failure(ApiError.INVALID_ROLE))

            val response = controller.assignHost(authUser, 1, AssignHostInput(userId = 2))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-role")
        }

        @Test
        fun `should return not found when the new host does not exist`() {
            whenever(staffService.assignHost(authUser.user, 1, 99)).thenReturn(failure(ApiError.USER_NOT_FOUND))

            val response = controller.assignHost(authUser, 1, AssignHostInput(userId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "user-not-found")
        }
    }

    @Nested
    inner class Judges {
        @Test
        fun `should add a judge successfully`() {
            whenever(staffService.addJudge(hostUser.user, 1, 2)).thenReturn(success(staff()))

            val response = controller.addJudge(hostUser, 1, AssignJudgeInput(userId = 2))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(staff(), response.body)
            verify(staffService).addJudge(hostUser.user, 1, 2)
        }

        @Test
        fun `should return conflict when the judge is already assigned`() {
            whenever(staffService.addJudge(hostUser.user, 1, 2)).thenReturn(failure(ApiError.JUDGE_ALREADY_ASSIGNED))

            val response = controller.addJudge(hostUser, 1, AssignJudgeInput(userId = 2))

            assertProblem(response, HttpStatus.CONFLICT, "judge-already-assigned")
        }

        @Test
        fun `should remove a judge successfully`() {
            whenever(staffService.removeJudge(hostUser.user, 1, 2)).thenReturn(success(Unit))

            val response = controller.removeJudge(hostUser, 1, 2)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            verify(staffService).removeJudge(hostUser.user, 1, 2)
        }

        @Test
        fun `should return not found when the judge is not assigned`() {
            whenever(staffService.removeJudge(hostUser.user, 1, 2)).thenReturn(failure(ApiError.JUDGE_NOT_ASSIGNED))

            val response = controller.removeJudge(hostUser, 1, 2)

            assertProblem(response, HttpStatus.NOT_FOUND, "judge-not-assigned")
        }
    }
}
