package com.caliarena.repo

import com.caliarena.Transaction
import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.domain.tournament.Tournament
import com.caliarena.repo.jpa.TransactionManagerJpa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class ScreenRoutineRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            repoScreenRoutine.clear()
            repoTournament.clear()
            repoEnduranceRoutine.clear()
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `should create a screen routine with the given fields`() =
            trx.run {
                val tournament = newTournament()
                val routine = newRoutine()
                val sr = newScreenRoutine(tournament.id, routine.id)

                assertNotEquals(0, sr.id)
                assertEquals(tournament.id, sr.tournamentId)
                assertEquals(routine.id, sr.routineId)
                assertEquals(0, sr.displayOrder)
                assertTrue(sr.isVisible)
                assertNull(sr.label)
            }

        @Test
        fun `should persist label when provided`() =
            trx.run {
                val sr = newScreenRoutine(newTournament().id, newRoutine().id, label = "Quartos de Final")

                assertEquals("Quartos de Final", repoScreenRoutine.findById(sr.id)?.label)
            }
    }

    @Nested
    inner class FindByTournamentId {
        @Test
        fun `should return all screen routines for a tournament ordered by displayOrder`() =
            trx.run {
                val tournament = newTournament()
                val routine = newRoutine()
                newScreenRoutine(tournament.id, routine.id, displayOrder = 2)
                newScreenRoutine(tournament.id, routine.id, displayOrder = 0)
                newScreenRoutine(tournament.id, routine.id, displayOrder = 1)

                val result = repoScreenRoutine.findByTournamentId(tournament.id)

                assertEquals(3, result.size)
                assertEquals(listOf(0, 1, 2), result.map { it.displayOrder })
            }

        @Test
        fun `should return empty list when no screen routines exist for tournament`() =
            trx.run {
                assertTrue(repoScreenRoutine.findByTournamentId(999).isEmpty())
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing screen routine by id`() =
            trx.run {
                val created = newScreenRoutine(newTournament().id, newRoutine().id)

                val found = repoScreenRoutine.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoScreenRoutine.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created screen routines`() =
            trx.run {
                val tournament = newTournament()
                val routine = newRoutine()
                repeat(3) { i -> newScreenRoutine(tournament.id, routine.id, displayOrder = i) }

                assertEquals(3, repoScreenRoutine.findAll().size)
            }

        @Test
        fun `should return empty list when there are no screen routines`() =
            trx.run {
                assertTrue(repoScreenRoutine.findAll().isEmpty())
            }
    }

    @Nested
    inner class Update {
        @Test
        fun `should update visibility`() =
            trx.run {
                val created = newScreenRoutine(newTournament().id, newRoutine().id)

                val updated = repoScreenRoutine.update(created.id, isVisible = false, displayOrder = null, label = null, now = now())

                assertEquals(false, updated?.isVisible)
            }

        @Test
        fun `should update displayOrder`() =
            trx.run {
                val created = newScreenRoutine(newTournament().id, newRoutine().id, displayOrder = 0)

                val updated = repoScreenRoutine.update(created.id, isVisible = null, displayOrder = 5, label = null, now = now())

                assertEquals(5, updated?.displayOrder)
            }

        @Test
        fun `should update label`() =
            trx.run {
                val created = newScreenRoutine(newTournament().id, newRoutine().id)

                val updated = repoScreenRoutine.update(created.id, isVisible = null, displayOrder = null, label = "Semi-Final", now = now())

                assertEquals("Semi-Final", updated?.label)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoScreenRoutine.update(-1, isVisible = false, displayOrder = null, label = null, now = now()))
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the screen routine`() =
            trx.run {
                val created = newScreenRoutine(newTournament().id, newRoutine().id)

                repoScreenRoutine.deleteById(created.id)

                assertNull(repoScreenRoutine.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all screen routines`() =
            trx.run {
                val tournament = newTournament()
                val routine = newRoutine()
                newScreenRoutine(tournament.id, routine.id, displayOrder = 0)
                newScreenRoutine(tournament.id, routine.id, displayOrder = 1)

                repoScreenRoutine.clear()

                assertTrue(repoScreenRoutine.findAll().isEmpty())
            }
    }

    private fun Transaction.now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newScreenRoutine(
        tournamentId: Int,
        routineId: Int,
        displayOrder: Int = 0,
        label: String? = null,
    ): ScreenRoutine =
        repoScreenRoutine.create(
            tournamentId = tournamentId,
            routineId = routineId,
            displayOrder = displayOrder,
            label = label,
            now = now(),
        )

    private fun Transaction.newTournament(): Tournament =
        repoTournament.createTournament(
            name = "tournament-${System.nanoTime()}",
            location = null,
            startDate = null,
            endDate = null,
            createdAt = now(),
        )

    private fun Transaction.newRoutine(): EnduranceRoutine =
        repoEnduranceRoutine.createRoutine(
            name = "routine-${System.nanoTime()}",
            timeCapSeconds = null,
            createdAt = now(),
        )
}
