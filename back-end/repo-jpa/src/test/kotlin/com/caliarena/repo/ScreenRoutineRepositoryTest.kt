package com.caliarena.repo

import com.caliarena.repo.entities.routine.ScreenRoutineEntity
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
class ScreenRoutineRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    private val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newTournament(): com.caliarena.repo.entities.tournament.TournamentEntity =
        tournaments.save(
            com.caliarena.repo.entities.tournament
                .TournamentEntity(name = "t-${System.nanoTime()}", createdAt = now.epochSecond),
        )

    private fun Transaction.newRoutineEntity(): com.caliarena.repo.entities.routine.EnduranceRoutineEntity =
        routines.save(
            com.caliarena.repo.entities.routine.EnduranceRoutineEntity(
                name = "sr-${System.nanoTime()}",
                timeCapSeconds = 600,
                createdAt = now.epochSecond,
            ),
        )

    private fun Transaction.newScreenRoutine(
        tournament: com.caliarena.repo.entities.tournament.TournamentEntity,
        displayOrder: Int = 1,
        label: String? = null,
    ): ScreenRoutineEntity {
        val routine = newRoutineEntity()
        return screenRoutines.save(
            ScreenRoutineEntity(
                tournamentId = tournament.id,
                routineId = routine.id,
                displayOrder = displayOrder,
                label = label,
                createdAt = now.epochSecond,
                updatedAt = now.epochSecond,
            ),
        )
    }

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

    @Nested
    inner class Create {
        @Test
        fun `should create a screen routine with the given fields`() {
            trx.run {
                val tournament = newTournament()
                val created = newScreenRoutine(tournament = tournament, displayOrder = 2, label = "WOD A")

                assertNotEqualsZero(created.id)
                assertEquals(tournament.id, created.tournamentId)
                assertEquals(2, created.displayOrder)
                assertTrue(created.isVisible)
                assertEquals("WOD A", created.label)
            }
        }

        @Test
        fun `should persist label when provided`() {
            trx.run {
                val created = newScreenRoutine(tournament = newTournament(), label = "Final WOD")

                assertEquals("Final WOD", screenRoutines.findByIdOrNull(created.id)?.label)
            }
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing screen routine by id`() =
            trx.run {
                val created = newScreenRoutine(tournament = newTournament())

                assertNotNull(screenRoutines.findByIdOrNull(created.id))
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(screenRoutines.findByIdOrNull(-1))
            }
    }

    @Nested
    inner class FindByTournamentId {
        @Test
        fun `should return all screen routines for a tournament ordered by display order`() =
            trx.run {
                val tournament10 = newTournament()
                val otherTournament = newTournament()
                newScreenRoutine(tournament = tournament10, displayOrder = 3, label = "third")
                newScreenRoutine(tournament = tournament10, displayOrder = 1, label = "first")
                newScreenRoutine(tournament = otherTournament, displayOrder = 1, label = "other")

                val found = screenRoutines.findByTournamentIdOrderByDisplayOrder(tournament10.id)

                assertEquals(2, found.size)
                assertEquals(listOf("first", "third"), found.map { it.label })
            }

        @Test
        fun `should return empty list when no screen routines exist for tournament`() =
            trx.run {
                assertTrue(screenRoutines.findByTournamentIdOrderByDisplayOrder(-1).isEmpty())
            }
    }

    @Nested
    inner class Update {
        @Test
        fun `should update visibility, order and label`() =
            trx.run {
                val created = newScreenRoutine(tournament = newTournament(), displayOrder = 1, label = "before")

                created.isVisible = false
                created.displayOrder = 4
                created.label = "after"
                created.updatedAt = now.plusSeconds(30).epochSecond
                screenRoutines.save(created)

                val updated = screenRoutines.findByIdOrNull(created.id)!!
                assertFalse(updated.isVisible)
                assertEquals(4, updated.displayOrder)
                assertEquals("after", updated.label)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the screen routine`() =
            trx.run {
                val created = newScreenRoutine(tournament = newTournament())

                screenRoutines.deleteById(created.id)

                assertNull(screenRoutines.findByIdOrNull(created.id))
            }
    }

    private fun assertNotEqualsZero(id: Int) {
        assertNotEquals(0, id)
    }
}
