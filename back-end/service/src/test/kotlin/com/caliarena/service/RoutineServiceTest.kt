package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.routine.RoutineOverview
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class RoutineServiceTest : ServiceTest() {
    private lateinit var service: RoutineService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoEnduranceRoutine).thenReturn(repoEnduranceRoutine)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = RoutineService(trxManager, clock)
    }

    @Nested
    inner class CreateRoutine {
        private val now = clock.instant()

        private val routine =
            EnduranceRoutine(
                id = 1,
                name = "Murph",
                timeCapSeconds = 3600,
                createdAt = now,
            )

        @Test
        fun `should create routine successfully`() {
            whenever(repoEnduranceRoutine.findByName("Murph")).thenReturn(null)
            whenever(repoEnduranceRoutine.createRoutine("Murph", 3600, now)).thenReturn(routine)

            val result = service.createRoutine("Murph", 3600)

            assertEquals(success(routine), result)
        }

        @Test
        fun `should fail when routine already exists`() {
            whenever(repoEnduranceRoutine.findByName("Murph")).thenReturn(routine)

            val result = service.createRoutine("Murph", 3600)

            assertEquals(failure(RoutineError.RoutineAlreadyExists), result)
        }
    }

    @Nested
    inner class CreateExercise {
        private val now = clock.instant()

        private val routine =
            EnduranceRoutine(
                id = 1,
                name = "Murph",
                timeCapSeconds = 3600,
                createdAt = now,
            )

        private val exercise =
            Exercise(
                id = 10,
                routineId = 1,
                name = "Push-Ups",
                targetReps = 20,
                addedWeight = null,
                exerciseOrder = 1,
                supersetOrder = null,
                type = ExerciseType.NORMAL,
            )

        @Test
        fun `should create exercise successfully`() {
            whenever(repoEnduranceRoutine.findById(1)).thenReturn(routine)
            whenever(
                repoEnduranceRoutine.createExercise(
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = ExerciseType.NORMAL,
                ),
            ).thenReturn(exercise)

            val result =
                service.createExercise(
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = "NORMAL",
                )

            assertEquals(success(exercise), result)
        }

        @Test
        fun `should fail when routine does not exist`() {
            whenever(repoEnduranceRoutine.findById(1)).thenReturn(null)

            val result =
                service.createExercise(
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = "NORMAL",
                )

            assertEquals(failure(RoutineError.RoutineNotFound), result)
        }

        @Test
        fun `should fail when exercise type is invalid`() {
            whenever(repoEnduranceRoutine.findById(1)).thenReturn(routine)

            val result =
                service.createExercise(
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = "INVALID",
                )

            assertEquals(failure(RoutineError.ExerciseTypeNotFound), result)
        }

        @Test
        fun `should fail when repository fails to create exercise`() {
            whenever(repoEnduranceRoutine.findById(1)).thenReturn(routine)
            whenever(
                repoEnduranceRoutine.createExercise(
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = ExerciseType.NORMAL,
                ),
            ).thenReturn(null)

            val result =
                service.createExercise(
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = "NORMAL",
                )

            assertEquals(failure(RoutineError.RoutineNotFound), result)
        }
    }

    @Nested
    inner class GetRoutineOverview {
        private val now = clock.instant()

        private val routine =
            EnduranceRoutine(
                id = 1,
                name = "Murph",
                timeCapSeconds = 3600,
                createdAt = now,
            )

        private val exercises =
            listOf(
                Exercise(
                    id = 10,
                    routineId = 1,
                    name = "Pull-Ups",
                    targetReps = 100,
                    addedWeight = null,
                    exerciseOrder = 2,
                    supersetOrder = null,
                    type = ExerciseType.NORMAL,
                ),
                Exercise(
                    id = 11,
                    routineId = 1,
                    name = "Push-Ups",
                    targetReps = 200,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = ExerciseType.NORMAL,
                ),
            )

        @Test
        fun `should return routine overview successfully`() {
            whenever(repoEnduranceRoutine.findByName("Murph")).thenReturn(routine)
            whenever(repoEnduranceRoutine.findExercisesByRoutineId(1)).thenReturn(exercises)

            val result = service.getRoutineOverview("Murph")

            val expected =
                RoutineOverview(
                    name = "Murph",
                    timeCapSeconds = 3600,
                    createdAt = now,
                    exercises = exercises.sortedBy { it.exerciseOrder },
                )

            assertEquals(success(expected), result)
        }

        @Test
        fun `should fail when routine does not exist`() {
            whenever(repoEnduranceRoutine.findByName("Murph")).thenReturn(null)

            val result = service.getRoutineOverview("Murph")

            assertEquals(failure(RoutineError.RoutineNotFound), result)
        }
    }
}
