package com.caliarena.http

import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.screen.CreateScreenRoutineInput
import com.caliarena.http.model.screen.UpdateDisplayOrderInput
import com.caliarena.http.model.screen.UpdateVisibilityInput
import com.caliarena.service.ApiError
import com.caliarena.service.ScreenRoutineService
import com.caliarena.service.failure
import com.caliarena.service.sse.SpectatorEmitter
import com.caliarena.service.sse.SpectatorPublisher
import com.caliarena.service.success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@ExtendWith(MockitoExtension::class)
class ScreenRoutineControllerTest {
    @Mock
    private lateinit var service: ScreenRoutineService

    @Mock
    private lateinit var publisher: SpectatorPublisher

    private lateinit var controller: ScreenRoutineController

    private val authUser = authenticatedUser()

    @BeforeEach
    fun setUp() {
        controller = ScreenRoutineController(service, publisher)
    }

    private fun screenRoutine(id: Int = 1) =
        ScreenRoutine(
            id = id,
            tournamentId = 7,
            routineId = 5,
            displayOrder = 1,
            isVisible = true,
            label = null,
            createdAt = 123L,
            updatedAt = 123L,
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
    inner class Listen {
        @Test
        fun `should open a listener for the tournament`() {
            val emitter = controller.listen(7)

            assertNotNull(emitter)
            verify(publisher).addEmitter(eq(7), any<SpectatorEmitter>())
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `should get all screen routines`() {
            whenever(service.getByTournamentId(7)).thenReturn(success(listOf(screenRoutine())))

            val response = controller.getAll(7)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("/api/tournaments/7/screen-routines", response.headers.getFirst("Location"))
            assertEquals(listOf(screenRoutine()), response.body)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(service.getByTournamentId(99)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.getAll(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }
    }

    @Nested
    inner class Create {
        private val input = CreateScreenRoutineInput(routineId = 5, displayOrder = 1, label = null)

        @Test
        fun `should create screen routine successfully`() {
            val created = screenRoutine()
            whenever(service.create(authUser.user, 7, 5, 1, null)).thenReturn(success(created))

            val response = controller.create(authUser, 7, input)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals(created, response.body)
            verify(service).create(authUser.user, 7, 5, 1, null)
        }

        @Test
        fun `should return not found when tournament does not exist`() {
            whenever(service.create(authUser.user, 99, 5, 1, null)).thenReturn(failure(ApiError.TOURNAMENT_NOT_FOUND))

            val response = controller.create(authUser, 99, input)

            assertProblem(response, HttpStatus.NOT_FOUND, "tournament-not-found")
        }

        @Test
        fun `should return not found when routine does not exist`() {
            whenever(service.create(authUser.user, 7, 99, 1, null)).thenReturn(failure(ApiError.ROUTINE_NOT_FOUND))

            val response = controller.create(authUser, 7, input.copy(routineId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "routine-not-found")
        }
    }

    @Nested
    inner class UpdateVisibility {
        @Test
        fun `should update visibility successfully`() {
            val updated = screenRoutine().copy(isVisible = false)
            whenever(service.update(authUser.user, 7, 1, false, null, null)).thenReturn(success(updated))

            val response = controller.updateVisibility(authUser, 7, 1, UpdateVisibilityInput(isVisible = false))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(updated, response.body)
            verify(service).update(authUser.user, 7, 1, false, null, null)
        }

        @Test
        fun `should return not found when screen routine does not exist`() {
            whenever(service.update(authUser.user, 7, 99, true, null, null)).thenReturn(failure(ApiError.SCREEN_ROUTINE_NOT_FOUND))

            val response = controller.updateVisibility(authUser, 7, 99, UpdateVisibilityInput(isVisible = true))

            assertProblem(response, HttpStatus.NOT_FOUND, "screen-routine-not-found")
        }

        @Test
        fun `should return conflict on tournament mismatch`() {
            whenever(service.update(authUser.user, 7, 1, true, null, null)).thenReturn(failure(ApiError.TOURNAMENT_MISMATCH))

            val response = controller.updateVisibility(authUser, 7, 1, UpdateVisibilityInput(isVisible = true))

            assertProblem(response, HttpStatus.CONFLICT, "tournament-mismatch")
        }

        @Test
        fun `should return not found when routine does not exist`() {
            whenever(service.update(authUser.user, 7, 1, true, null, null)).thenReturn(failure(ApiError.ROUTINE_NOT_FOUND))

            val response = controller.updateVisibility(authUser, 7, 1, UpdateVisibilityInput(isVisible = true))

            assertProblem(response, HttpStatus.NOT_FOUND, "routine-not-found")
        }
    }

    @Nested
    inner class UpdateDisplayOrder {
        @Test
        fun `should update display order successfully`() {
            whenever(service.update(authUser.user, 7, 1, null, 3, null)).thenReturn(success(screenRoutine()))

            val response = controller.updateDisplayOrder(authUser, 7, 1, UpdateDisplayOrderInput(displayOrder = 3))

            assertEquals(HttpStatus.OK, response.statusCode)
            verify(service).update(authUser.user, 7, 1, null, 3, null)
        }

        @Test
        fun `should return not found when screen routine does not exist`() {
            whenever(service.update(authUser.user, 7, 99, null, 3, null)).thenReturn(failure(ApiError.SCREEN_ROUTINE_NOT_FOUND))

            val response = controller.updateDisplayOrder(authUser, 7, 99, UpdateDisplayOrderInput(displayOrder = 3))

            assertProblem(response, HttpStatus.NOT_FOUND, "screen-routine-not-found")
        }

        @Test
        fun `should return conflict on tournament mismatch`() {
            whenever(service.update(authUser.user, 7, 1, null, 3, null)).thenReturn(failure(ApiError.TOURNAMENT_MISMATCH))

            val response = controller.updateDisplayOrder(authUser, 7, 1, UpdateDisplayOrderInput(displayOrder = 3))

            assertProblem(response, HttpStatus.CONFLICT, "tournament-mismatch")
        }

        @Test
        fun `should return not found when routine does not exist`() {
            whenever(service.update(authUser.user, 7, 1, null, 3, null)).thenReturn(failure(ApiError.ROUTINE_NOT_FOUND))

            val response = controller.updateDisplayOrder(authUser, 7, 1, UpdateDisplayOrderInput(displayOrder = 3))

            assertProblem(response, HttpStatus.NOT_FOUND, "routine-not-found")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `should delete screen routine successfully`() {
            whenever(service.delete(authUser.user, 7, 1)).thenReturn(success(Unit))

            val response = controller.delete(authUser, 7, 1)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            assertNull(response.body)
            verify(service).delete(authUser.user, 7, 1)
        }

        @Test
        fun `should return not found when screen routine does not exist`() {
            whenever(service.delete(authUser.user, 7, 99)).thenReturn(failure(ApiError.SCREEN_ROUTINE_NOT_FOUND))

            val response = controller.delete(authUser, 7, 99)

            assertProblem(response, HttpStatus.NOT_FOUND, "screen-routine-not-found")
        }

        @Test
        fun `should return conflict on tournament mismatch`() {
            whenever(service.delete(authUser.user, 7, 1)).thenReturn(failure(ApiError.TOURNAMENT_MISMATCH))

            val response = controller.delete(authUser, 7, 1)

            assertProblem(response, HttpStatus.CONFLICT, "tournament-mismatch")
        }
    }
}
