package com.caliarena.http

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.athlete.CreateAthleteInput
import com.caliarena.http.model.athlete.UpdateAthleteInput
import com.caliarena.service.ApiError
import com.caliarena.service.AthleteService
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
class AthleteControllerTest {
    @Mock
    private lateinit var athleteService: AthleteService

    private lateinit var controller: AthleteController

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = AthleteController(athleteService)
    }

    private fun athlete(
        id: Int = 1,
        name: String = "Maria",
        gender: GenderType = GenderType.FEMALE,
        clubId: Int? = 1,
    ) = Athlete(id, name, gender, clubId, now)

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
    inner class CreateAthlete {
        @Test
        fun `should create athlete successfully`() {
            val created = athlete(id = 1)
            whenever(athleteService.createAthlete("Maria", "FEMALE", 1)).thenReturn(success(created))

            val response = controller.createAthlete(CreateAthleteInput(name = "Maria", gender = "FEMALE", clubId = 1))

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/athletes/1", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(athleteService).createAthlete("Maria", "FEMALE", 1)
        }

        @Test
        fun `should return not found when club does not exist`() {
            whenever(athleteService.createAthlete("Maria", "FEMALE", 99)).thenReturn(failure(ApiError.CLUB_NOT_FOUND))

            val response = controller.createAthlete(CreateAthleteInput(name = "Maria", gender = "FEMALE", clubId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "club-not-found")
        }

        @Test
        fun `should return bad request when gender is invalid`() {
            whenever(athleteService.createAthlete("Maria", "UNKNOWN", null)).thenReturn(failure(ApiError.INVALID_GENDER))

            val response = controller.createAthlete(CreateAthleteInput(name = "Maria", gender = "UNKNOWN", clubId = null))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-gender")
        }
    }

    @Nested
    inner class UpdateAthlete {
        @Test
        fun `should update athlete successfully`() {
            val updated = athlete(id = 1, name = "Maria Silva")
            whenever(athleteService.updateAthlete(1, "Maria Silva", "FEMALE", 1)).thenReturn(success(updated))

            val response = controller.updateAthlete(1, UpdateAthleteInput(name = "Maria Silva", gender = "FEMALE", clubId = 1))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(updated, response.body)
            verify(athleteService).updateAthlete(1, "Maria Silva", "FEMALE", 1)
        }

        @Test
        fun `should return not found when athlete does not exist`() {
            whenever(athleteService.updateAthlete(99, "Maria Silva", "FEMALE", null)).thenReturn(failure(ApiError.ATHLETE_NOT_FOUND))

            val response = controller.updateAthlete(99, UpdateAthleteInput(name = "Maria Silva", gender = "FEMALE", clubId = null))

            assertProblem(response, HttpStatus.NOT_FOUND, "athlete-not-found")
        }

        @Test
        fun `should return bad request when gender is invalid`() {
            whenever(athleteService.updateAthlete(1, "Maria Silva", "UNKNOWN", null)).thenReturn(failure(ApiError.INVALID_GENDER))

            val response = controller.updateAthlete(1, UpdateAthleteInput(name = "Maria Silva", gender = "UNKNOWN", clubId = null))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-gender")
        }

        @Test
        fun `should return not found when club does not exist`() {
            whenever(athleteService.updateAthlete(1, "Maria Silva", "FEMALE", 99)).thenReturn(failure(ApiError.CLUB_NOT_FOUND))

            val response = controller.updateAthlete(1, UpdateAthleteInput(name = "Maria Silva", gender = "FEMALE", clubId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "club-not-found")
        }
    }

    @Nested
    inner class GetAthleteById {
        @Test
        fun `should get athlete by id`() {
            whenever(athleteService.getAthleteById(1)).thenReturn(success(athlete()))

            val response = controller.getAthleteById(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(athlete(), response.body)
        }

        @Test
        fun `should return not found when athlete does not exist`() {
            whenever(athleteService.getAthleteById(99)).thenReturn(failure(ApiError.ATHLETE_NOT_FOUND))

            val response = controller.getAthleteById(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "athlete-not-found")
        }
    }

    @Nested
    inner class GetAllAthletes {
        @Test
        fun `should get all athletes`() {
            whenever(athleteService.getAllAthletes()).thenReturn(listOf(athlete()))

            val response = controller.getAllAthletes()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(athlete()), response.body)
        }
    }

    @Nested
    inner class GetAthletesByClub {
        @Test
        fun `should get athletes by club`() {
            whenever(athleteService.getAthletesByClub(1)).thenReturn(success(listOf(athlete())))

            val response = controller.getAthletesByClub(1)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(athlete()), response.body)
        }

        @Test
        fun `should return not found when club does not exist`() {
            whenever(athleteService.getAthletesByClub(99)).thenReturn(failure(ApiError.CLUB_NOT_FOUND))

            val response = controller.getAthletesByClub(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "club-not-found")
        }
    }

    @Nested
    inner class GetAthletesByGender {
        @Test
        fun `should get athletes by gender`() {
            whenever(athleteService.getAthletesByGender("FEMALE")).thenReturn(success(listOf(athlete())))

            val response = controller.getAthletesByGender("FEMALE")

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(athlete()), response.body)
        }

        @Test
        fun `should return bad request when gender is invalid`() {
            whenever(athleteService.getAthletesByGender("UNKNOWN")).thenReturn(failure(ApiError.INVALID_GENDER))

            val response = controller.getAthletesByGender("UNKNOWN")

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-gender")
        }
    }
}
