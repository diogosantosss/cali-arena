package com.caliarena.repo

import com.caliarena.domain.routine.ExerciseType
import com.caliarena.repo.jpa.TransactionManagerJpa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfiguration::class])
class EnduranceRoutineRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            repoEnduranceRoutine.clear()
        }
    }

    @Nested
    inner class CreateRoutine {
        @Test
        fun `should create a routine with the given fields`() =
            trx.run {
                val routine =
                    repoEnduranceRoutine.createRoutine(
                        name = "routine-a",
                        timeCapSeconds = 1200,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotEquals(0, routine.id)
                assertEquals("routine-a", routine.name)
                assertEquals(1200, routine.timeCapSeconds)
            }
    }

    @Nested
    inner class FindByName {
        @Test
        fun `should find an existing routine by name`() =
            trx.run {
                repoEnduranceRoutine.createRoutine("routine-b", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val found = repoEnduranceRoutine.findByName("routine-b")

                assertNotNull(found)
                assertEquals("routine-b", found?.name)
            }

        @Test
        fun `should return null when name does not exist`() =
            trx.run {
                assertNull(repoEnduranceRoutine.findByName("missing"))
            }
    }

    @Nested
    inner class CreateExercise {
        @Test
        fun `should create an exercise for an existing routine`() =
            trx.run {
                val routine = repoEnduranceRoutine.createRoutine("routine-c", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val exercise =
                    repoEnduranceRoutine.createExercise(
                        routineId = routine.id,
                        name = "push-up",
                        targetReps = 15,
                        addedWeight = BigDecimal("10.50"),
                        exerciseOrder = 1,
                        supersetOrder = null,
                        type = ExerciseType.NORMAL,
                    )

                assertNotNull(exercise)
                assertNotEquals(0, exercise?.id)
                assertEquals("push-up", exercise?.name)
                assertEquals(routine.id, exercise?.routineId)
            }

        @Test
        fun `should return null when routine does not exist`() =
            trx.run {
                val exercise =
                    repoEnduranceRoutine.createExercise(
                        routineId = -1,
                        name = "ghost",
                        targetReps = 10,
                        addedWeight = null,
                        exerciseOrder = 1,
                        supersetOrder = null,
                        type = ExerciseType.UNBROKEN,
                    )

                assertNull(exercise)
            }
    }

    @Nested
    inner class FindExercisesByRoutineId {
        @Test
        fun `should return exercises for a routine`() =
            trx.run {
                val routine = repoEnduranceRoutine.createRoutine("routine-d", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoEnduranceRoutine.createExercise(routine.id, "e1", 10, null, 1, null, ExerciseType.NORMAL)
                repoEnduranceRoutine.createExercise(routine.id, "e2", 12, null, 2, 1, ExerciseType.SUPERSET)

                val exercises = repoEnduranceRoutine.findExercisesByRoutineId(routine.id)

                assertEquals(2, exercises.size)
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing routine by id`() =
            trx.run {
                val created = repoEnduranceRoutine.createRoutine("routine-e", 900, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val found = repoEnduranceRoutine.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoEnduranceRoutine.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created routines`() =
            trx.run {
                repoEnduranceRoutine.createRoutine("routine-f1", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoEnduranceRoutine.createRoutine("routine-f2", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val routines = repoEnduranceRoutine.findAll()

                assertEquals(2, routines.size)
            }

        @Test
        fun `should return empty list when there are no routines`() =
            trx.run {
                assertTrue(repoEnduranceRoutine.findAll().isEmpty())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing routine`() =
            trx.run {
                val created = repoEnduranceRoutine.createRoutine("routine-g", 600, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                val updated = created.copy(name = "routine-g-updated")

                val saved = repoEnduranceRoutine.save(updated)

                assertNotNull(saved)
                assertEquals("routine-g-updated", saved?.name)
                assertEquals(created.id, saved?.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the routine`() =
            trx.run {
                val created = repoEnduranceRoutine.createRoutine("routine-h", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                repoEnduranceRoutine.deleteById(created.id)

                assertNull(repoEnduranceRoutine.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all routines and exercises`() =
            trx.run {
                val routine = repoEnduranceRoutine.createRoutine("routine-i", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoEnduranceRoutine.createExercise(routine.id, "e1", 10, null, 1, null, ExerciseType.NORMAL)

                repoEnduranceRoutine.clear()

                assertTrue(repoEnduranceRoutine.findAll().isEmpty())
            }
    }
}
