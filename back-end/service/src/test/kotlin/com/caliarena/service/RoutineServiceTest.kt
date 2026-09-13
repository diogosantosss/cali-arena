package com.caliarena.service

import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class RoutineServiceTest : ServiceTest() {
    private lateinit var service: RoutineService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.routines).thenReturn(routines)
        lenient().whenever(transaction.exercises).thenReturn(exercises)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = RoutineService(trxManager, clock)
    }

    private val now = clock.instant()

    private fun routineEntity(
        id: Int = 1,
        name: String = "Murph",
    ) = EnduranceRoutineEntity(id, name, 3600, now.epochSecond)

    private fun exerciseEntity(
        id: Int = 10,
        name: String = "Push-Ups",
        order: Int = 1,
    ) = ExerciseEntity(id, routineEntity(), name, 20, null, order, null, ExerciseType.NORMAL)

    @Nested
    inner class CreateRoutine {
        @Test
        fun `should create routine successfully`() {
            whenever(routines.findByName("Murph")).thenReturn(null)
            whenever(routines.save(any())).thenReturn(routineEntity())

            val result = service.createRoutine("Murph", 3600)

            assertEquals(success(EnduranceRoutine(1, "Murph", 3600, now)), result)
        }

        @Test
        fun `should fail when routine already exists`() {
            whenever(routines.findByName("Murph")).thenReturn(routineEntity())

            val result = service.createRoutine("Murph", 3600)

            assertEquals(failure(ApiError.ROUTINE_ALREADY_EXISTS), result)

            verify(routines, never()).save(any())
        }
    }

    @Nested
    inner class CreateExercise {
        private val routine = routineEntity()

        private val exercise = exerciseEntity()

        @Test
        fun `should create exercise successfully`() {
            whenever(routines.findById(1)).thenReturn(Optional.of(routine))
            whenever(exercises.existsByRoutineIdAndExerciseOrder(1, 1)).thenReturn(false)
            whenever(exercises.save(any())).thenReturn(exercise)

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

            assertEquals(success(exercise.toDomain()), result)
        }

        @Test
        fun `should fail when routine does not exist`() {
            whenever(routines.findById(1)).thenReturn(Optional.empty())

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

            assertEquals(failure(ApiError.ROUTINE_NOT_FOUND), result)
        }

        @Test
        fun `should fail when exercise type is invalid`() {
            whenever(routines.findById(1)).thenReturn(Optional.of(routine))

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

            assertEquals(failure(ApiError.EXERCISE_TYPE_NOT_FOUND), result)

            verify(exercises, never()).save(any())
        }

        @Test
        fun `should shift existing orders when position is taken by non-superset`() {
            val saved = exerciseEntity(id = 11)
            whenever(routines.findById(1)).thenReturn(Optional.of(routine))
            whenever(exercises.existsByRoutineIdAndExerciseOrder(1, 1)).thenReturn(true)
            whenever(exercises.save(any())).thenReturn(saved)

            service.createExercise(
                routineId = 1,
                name = "Push-Ups",
                targetReps = 20,
                addedWeight = null,
                exerciseOrder = 1,
                supersetOrder = null,
                type = "NORMAL",
            )

            verify(exercises).shiftExerciseOrders(1, 1)
        }
    }

    @Nested
    inner class UpdateExercise {
        @Test
        fun `should update exercise successfully`() {
            val existing = exerciseEntity()
            val updated = exerciseEntity(name = "Dips")

            whenever(exercises.findById(10)).thenReturn(Optional.of(existing))
            whenever(exercises.save(any())).thenReturn(updated)

            val result =
                service.updateExercise(
                    id = 10,
                    name = "Dips",
                    targetReps = 15,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = null,
                    type = "UNBROKEN",
                )

            assertEquals(success(updated.toDomain()), result)

            verify(exercises).save(existing)
            verify(exercises, never()).shiftExerciseOrdersUp(any(), any(), any())
            verify(exercises, never()).shiftExerciseOrdersDown(any(), any(), any())
        }

        @Test
        fun `should shift orders down when moving exercise earlier`() {
            val existing = exerciseEntity(order = 2)

            whenever(exercises.findById(10)).thenReturn(Optional.of(existing))
            whenever(exercises.save(any())).thenReturn(existing)

            service.updateExercise(
                id = 10,
                name = "Push-Ups",
                targetReps = 20,
                addedWeight = null,
                exerciseOrder = 1,
                supersetOrder = null,
                type = "NORMAL",
            )

            verify(exercises).shiftExerciseOrdersUp(1, 1, 2)
            verify(exercises, never()).shiftExerciseOrdersDown(any(), any(), any())
        }

        @Test
        fun `should shift orders up when moving exercise later`() {
            val existing = exerciseEntity(order = 1)

            whenever(exercises.findById(10)).thenReturn(Optional.of(existing))
            whenever(exercises.save(any())).thenReturn(existing)

            service.updateExercise(
                id = 10,
                name = "Push-Ups",
                targetReps = 20,
                addedWeight = null,
                exerciseOrder = 9,
                supersetOrder = null,
                type = "NORMAL",
            )

            verify(exercises).shiftExerciseOrdersDown(1, 1, 9)
            verify(exercises, never()).shiftExerciseOrdersUp(any(), any(), any())
        }

        @Test
        fun `should fail when exercise does not exist`() {
            whenever(exercises.findById(10)).thenReturn(Optional.empty())

            val result =
                service.updateExercise(
                    id = 10,
                    name = "Dips",
                    targetReps = 15,
                    addedWeight = null,
                    exerciseOrder = 2,
                    supersetOrder = null,
                    type = "NORMAL",
                )

            assertEquals(failure(ApiError.EXERCISE_NOT_FOUND), result)

            verify(exercises, never()).save(any())
        }

        @Test
        fun `should fail when exercise type is invalid`() {
            val result =
                service.updateExercise(
                    id = 10,
                    name = "Dips",
                    targetReps = 15,
                    addedWeight = null,
                    exerciseOrder = 2,
                    supersetOrder = null,
                    type = "INVALID",
                )

            assertEquals(failure(ApiError.EXERCISE_TYPE_NOT_FOUND), result)

            verify(exercises, never()).save(any())
        }
    }

    @Nested
    inner class DeleteExercise {
        @Test
        fun `should delete exercise successfully`() {
            val existing = exerciseEntity()

            whenever(exercises.findById(10)).thenReturn(Optional.of(existing))

            val result = service.deleteExercise(10)

            assertEquals(success(Unit), result)

            verify(exercises).delete(existing)
        }

        @Test
        fun `should fail when exercise does not exist`() {
            whenever(exercises.findById(10)).thenReturn(Optional.empty())

            val result = service.deleteExercise(10)

            assertEquals(failure(ApiError.EXERCISE_NOT_FOUND), result)

            verify(exercises, never()).delete(any())
        }
    }

    @Nested
    inner class GetRoutineOverview {
        private val pullUps = exerciseEntity(id = 10, name = "Pull-Ups", order = 2)
        private val pushUps = exerciseEntity(id = 11, name = "Push-Ups", order = 1)

        @Test
        fun `should return routine overview successfully`() {
            whenever(routines.findByName("Murph")).thenReturn(routineEntity())
            whenever(exercises.findExercisesByRoutineId(1)).thenReturn(listOf(pullUps, pushUps))

            val result = service.getRoutineOverview("Murph")

            val expected =
                RoutineOverview(
                    name = "Murph",
                    timeCapSeconds = 3600,
                    createdAt = now,
                    exercises =
                        listOf(
                            pushUps.toDomain(),
                            pullUps.toDomain(),
                        ),
                )

            assertEquals(success(expected), result)
        }

        @Test
        fun `should fail when routine does not exist`() {
            whenever(routines.findByName("Murph")).thenReturn(null)

            val result = service.getRoutineOverview("Murph")

            assertEquals(failure(ApiError.ROUTINE_NOT_FOUND), result)
        }
    }
}
