package com.caliarena.http

import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.routine.CreateExerciseInput
import com.caliarena.http.model.routine.CreateRoutineInput
import com.caliarena.http.model.routine.UpdateExerciseInput
import com.caliarena.service.ApiError
import com.caliarena.service.RoutineService
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
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class RoutineControllerTest {
    @Mock
    private lateinit var routineService: RoutineService

    private lateinit var controller: RoutineController

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = RoutineController(routineService)
    }

    private fun routine(
        id: Int = 1,
        name: String = "Endurance",
        timeCapSeconds: Int? = 180,
    ) = EnduranceRoutine(id, name, timeCapSeconds, now)

    private fun exercise(
        id: Int = 11,
        routineId: Int = 1,
        exerciseOrder: Int = 1,
    ) = Exercise(
        id = id,
        routineId = routineId,
        name = "Squats",
        targetReps = 10,
        addedWeight = BigDecimal.TEN,
        exerciseOrder = exerciseOrder,
        supersetOrder = null,
        type = ExerciseType.NORMAL,
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
    inner class CreateRoutine {
        @Test
        fun `should create routine successfully`() {
            val created = routine(id = 1)
            whenever(routineService.createRoutine("Endurance", 180)).thenReturn(success(created))

            val response = controller.createRoutine(CreateRoutineInput(name = "Endurance", timeCapSeconds = 180))

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/routines/1", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(routineService).createRoutine("Endurance", 180)
        }

        @Test
        fun `should return conflict when routine already exists`() {
            whenever(routineService.createRoutine("Endurance", 180)).thenReturn(failure(ApiError.ROUTINE_ALREADY_EXISTS))

            val response = controller.createRoutine(CreateRoutineInput(name = "Endurance", timeCapSeconds = 180))

            assertProblem(response, HttpStatus.CONFLICT, "routine-already-exists")
        }
    }

    @Nested
    inner class CreateExercise {
        private val input =
            CreateExerciseInput(
                routineId = 1,
                name = "Squats",
                targetReps = 10,
                addedWeight = BigDecimal.TEN,
                exerciseOrder = 1,
                supersetOrder = null,
                type = "NORMAL",
            )

        @Test
        fun `should create exercise successfully`() {
            val created = exercise(id = 11, routineId = 1)
            whenever(routineService.createExercise(1, "Squats", 10, BigDecimal.TEN, 1, null, "NORMAL")).thenReturn(success(created))

            val response = controller.createExercise(input)

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/routines/1/exercises/11", response.headers.getFirst("Location"))
            assertEquals(created, response.body)
            verify(routineService).createExercise(1, "Squats", 10, BigDecimal.TEN, 1, null, "NORMAL")
        }

        @Test
        fun `should return not found when routine does not exist`() {
            whenever(
                routineService.createExercise(99, "Squats", 10, BigDecimal.TEN, 1, null, "NORMAL"),
            ).thenReturn(failure(ApiError.ROUTINE_NOT_FOUND))

            val response = controller.createExercise(input.copy(routineId = 99))

            assertProblem(response, HttpStatus.NOT_FOUND, "routine-not-found")
        }

        @Test
        fun `should return not found when exercise type is invalid`() {
            whenever(
                routineService.createExercise(1, "Squats", 10, BigDecimal.TEN, 1, null, "INVALID"),
            ).thenReturn(failure(ApiError.EXERCISE_TYPE_NOT_FOUND))

            val response = controller.createExercise(input.copy(type = "INVALID"))

            assertProblem(response, HttpStatus.NOT_FOUND, "exercise-type-not-found")
        }
    }

    @Nested
    inner class UpdateExercise {
        private val input =
            UpdateExerciseInput(
                name = "Squats",
                targetReps = 10,
                addedWeight = BigDecimal.TEN,
                exerciseOrder = 2,
                supersetOrder = null,
                type = "NORMAL",
            )

        @Test
        fun `should update exercise successfully`() {
            val updated = exercise(id = 11, exerciseOrder = 2)
            whenever(routineService.updateExercise(11, "Squats", 10, BigDecimal.TEN, 2, null, "NORMAL")).thenReturn(success(updated))

            val response = controller.updateExercise(11, input)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(updated, response.body)
            verify(routineService).updateExercise(11, "Squats", 10, BigDecimal.TEN, 2, null, "NORMAL")
        }

        @Test
        fun `should return not found when exercise type is invalid`() {
            whenever(
                routineService.updateExercise(11, "Squats", 10, BigDecimal.TEN, 2, null, "INVALID"),
            ).thenReturn(failure(ApiError.EXERCISE_TYPE_NOT_FOUND))

            val response = controller.updateExercise(11, input.copy(type = "INVALID"))

            assertProblem(response, HttpStatus.NOT_FOUND, "exercise-type-not-found")
        }

        @Test
        fun `should return not found when exercise does not exist`() {
            whenever(
                routineService.updateExercise(99, "Squats", 10, BigDecimal.TEN, 2, null, "NORMAL"),
            ).thenReturn(failure(ApiError.EXERCISE_NOT_FOUND))

            val response = controller.updateExercise(99, input)

            assertProblem(response, HttpStatus.NOT_FOUND, "exercise-not-found")
        }
    }

    @Nested
    inner class DeleteExercise {
        @Test
        fun `should delete exercise successfully`() {
            whenever(routineService.deleteExercise(11)).thenReturn(success(Unit))

            val response = controller.deleteExercise(11)

            assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
            assertNull(response.body)
            verify(routineService).deleteExercise(11)
        }

        @Test
        fun `should return not found when exercise does not exist`() {
            whenever(routineService.deleteExercise(99)).thenReturn(failure(ApiError.EXERCISE_NOT_FOUND))

            val response = controller.deleteExercise(99)

            assertProblem(response, HttpStatus.NOT_FOUND, "exercise-not-found")
        }
    }

    @Nested
    inner class GetRoutineOverview {
        @Test
        fun `should get routine overview`() {
            val overview = RoutineOverview(name = "Endurance", timeCapSeconds = 180, createdAt = now, exercises = listOf(exercise()))
            whenever(routineService.getRoutineOverview("Endurance")).thenReturn(success(overview))

            val response = controller.getRoutineOverview("Endurance")

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(overview, response.body)
        }

        @Test
        fun `should return not found when routine does not exist`() {
            whenever(routineService.getRoutineOverview("Missing")).thenReturn(failure(ApiError.ROUTINE_NOT_FOUND))

            val response = controller.getRoutineOverview("Missing")

            assertProblem(response, HttpStatus.NOT_FOUND, "routine-not-found")
        }
    }

    @Nested
    inner class GetRoutines {
        @Test
        fun `should get all routines`() {
            whenever(routineService.getRoutines()).thenReturn(listOf(routine()))

            val response = controller.getRoutines()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(listOf(routine()), response.body)
        }
    }
}
