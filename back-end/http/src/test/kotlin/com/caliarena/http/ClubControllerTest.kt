package com.caliarena.http

import com.caliarena.domain.club.Club
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.club.CreateClubInput
import com.caliarena.http.model.club.UpdateClubInput
import com.caliarena.service.ApiError
import com.caliarena.service.ClubService
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
class ClubControllerTest {
    @Mock
    private lateinit var clubService: ClubService

    private lateinit var controller: ClubController

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = ClubController(clubService)
    }

    private fun club(
        id: Int = 1,
        name: String = "CrossFit Lisbon",
        shortName: String? = "CFL",
    ) = Club(id, name, shortName, now)

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
    inner class CreateClub {
        @Test
        fun `should create club successfully`() {
            val created = club(id = 1)
            whenever(clubService.createClub("CrossFit Lisbon", "CFL")).thenReturn(success(created))

            val response = controller.createClub(CreateClubInput(name = "CrossFit Lisbon", shortName = "CFL"))

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/clubs/1", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(clubService).createClub("CrossFit Lisbon", "CFL")
        }

        @Test
        fun `should return conflict when club already exists`() {
            whenever(clubService.createClub("CrossFit Lisbon", "CFL")).thenReturn(failure(ApiError.CLUB_ALREADY_EXISTS))

            val response = controller.createClub(CreateClubInput(name = "CrossFit Lisbon", shortName = "CFL"))

            assertProblem(response, HttpStatus.CONFLICT, "club-already-exists")
        }
    }

    @Nested
    inner class GetClubById {
        @Test
        fun `should get club by id`() {
            whenever(clubService.getClubById(1)).thenReturn(success(club()))

            val response = controller.getClubById(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(club(), response.body)
        }

        @Test
        fun `should return not found when club does not exist`() {
            whenever(clubService.getClubById(99)).thenReturn(failure(ApiError.CLUB_NOT_FOUND))

            val response = controller.getClubById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "club-not-found")
        }
    }

    @Nested
    inner class GetAllClubs {
        @Test
        fun `should get all clubs`() {
            whenever(clubService.getAllClubs()).thenReturn(listOf(club()))

            val response = controller.getAllClubs()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(club()), response.body)
        }
    }

    @Nested
    inner class UpdateClub {
        @Test
        fun `should update club successfully`() {
            val updated = club(id = 1, name = "CrossFit Porto")
            whenever(clubService.updateClub(1, "CrossFit Porto", "CFP")).thenReturn(success(updated))

            val response = controller.updateClub(1, UpdateClubInput(name = "CrossFit Porto", shortName = "CFP"))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(updated, response.body)
            verify(clubService).updateClub(1, "CrossFit Porto", "CFP")
        }

        @Test
        fun `should return not found when club does not exist`() {
            whenever(clubService.updateClub(99, "CrossFit Porto", "CFP")).thenReturn(failure(ApiError.CLUB_NOT_FOUND))

            val response = controller.updateClub(99, UpdateClubInput(name = "CrossFit Porto", shortName = "CFP"))

            assertProblem(response, HttpStatus.NOT_FOUND, "club-not-found")
        }

        @Test
        fun `should return conflict when name is already used by another club`() {
            whenever(clubService.updateClub(1, "CrossFit Porto", "CFP")).thenReturn(failure(ApiError.CLUB_ALREADY_EXISTS))

            val response = controller.updateClub(1, UpdateClubInput(name = "CrossFit Porto", shortName = "CFP"))

            assertProblem(response, HttpStatus.CONFLICT, "club-already-exists")
        }
    }
}
