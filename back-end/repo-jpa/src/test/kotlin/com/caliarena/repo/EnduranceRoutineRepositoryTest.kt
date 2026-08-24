package com.caliarena.repo

import com.caliarena.domain.routine.ExerciseType
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManagerJpa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class EnduranceRoutineRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            matchProgresses.deleteAll()
            matches.deleteAll()
            screenRoutines.deleteAll()
            exercises.deleteAll()
            routines.deleteAll()
        }
    }

    private fun now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newRoutine(name: String = "routine-${System.nanoTime()}"): EnduranceRoutineEntity =
        routines.save(EnduranceRoutineEntity(name = name, timeCapSeconds = 600, createdAt = now().epochSecond))

    private fun Transaction.newExercise(
        routine: EnduranceRoutineEntity,
        order: Int = 1,
    ): ExerciseEntity =
        exercises.save(
            ExerciseEntity(
                routine = routine,
                name = "ex-$order",
                targetReps = 10,
                exerciseOrder = order,
                type = ExerciseType.NORMAL,
            ),
        )

    @Nested
    inner class CreateRoutine {
        @Test
        fun `should create a routine with the given fields`() =
            trx.run {
                val created = newRoutine(name = "push-day")

                assertNotEqualsZero(created.id)
                assertEquals("push-day", created.name)
                assertEquals(600, created.timeCapSeconds)
            }

        @Test
        fun `should find a routine by name`() =
            trx.run {
                newRoutine(name = "unique-name")

                val found = routines.findByName("unique-name")

                assertNotNull(found)
                assertEquals("unique-name", found?.name)
            }

        @Test
        fun `should return null when name does not exist`() =
            trx.run {
                assertNull(routines.findByName("does-not-exist"))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing routine by id`() =
            trx.run {
                val created = newRoutine()

                assertNotNull(routines.findByIdOrNull(created.id))
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(routines.findByIdOrNull(-1))
            }
    }

    @Nested
    inner class Exercises {
        @Test
        fun `should create an exercise for an existing routine`() =
            trx.run {
                val routine = newRoutine()

                val exercise = newExercise(routine, order = 2)

                assertNotEqualsZero(exercise.id)
                assertEquals(routine.id, exercise.routine.id)
                assertEquals(2, exercise.exerciseOrder)
            }

        @Test
        fun `should list the exercises of a routine ordered`() =
            trx.run {
                val routine = newRoutine()
                newExercise(routine, order = 2)
                newExercise(routine, order = 1)

                val list = exercises.findExercisesByRoutineId(routine.id)

                assertEquals(2, list.size)
            }

        @Test
        fun `should return empty list when routine has no exercises`() =
            trx.run {
                val routine = newRoutine()

                assertTrue(exercises.findExercisesByRoutineId(routine.id).isEmpty())
            }

        @Test
        fun `should report whether an order exists in the routine`() =
            trx.run {
                val routine = newRoutine()
                newExercise(routine, order = 3)

                assertTrue(exercises.existsByRoutineIdAndExerciseOrder(routine.id, 3))
                assertFalse(exercises.existsByRoutineIdAndExerciseOrder(routine.id, 4))
            }

        @Test
        fun `should shift exercise orders from a given position`() =
            trx.run {
                val routine = newRoutine()
                val first = newExercise(routine, order = 1)
                val second = newExercise(routine, order = 2)

                exercises.shiftExerciseOrders(routine.id, 2)

                assertEquals(1, exercises.findByIdOrNull(first.id)?.exerciseOrder)
                assertEquals(3, exercises.findByIdOrNull(second.id)?.exerciseOrder)
            }
    }

    @Nested
    inner class Delete {
        @Test
        fun `should remove the routine`() =
            trx.run {
                val created = newRoutine()

                routines.deleteById(created.id)

                assertNull(routines.findByIdOrNull(created.id))
            }

        @Test
        fun `should delete the routine after its exercises are removed`() =
            trx.run {
                val routine = newRoutine()
                newExercise(routine)

                exercises.findExercisesByRoutineId(routine.id).forEach { exercises.deleteById(it.id) }
                routines.deleteById(routine.id)

                assertNull(routines.findByIdOrNull(routine.id))
                assertTrue(exercises.findExercisesByRoutineId(routine.id).isEmpty())
            }
    }

    private fun assertNotEqualsZero(id: Int) {
        assertNotEquals(0, id)
    }
}
