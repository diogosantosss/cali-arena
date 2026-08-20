package com.caliarena.repo

import com.caliarena.Transaction
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentStatus
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
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class TournamentRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            repoMatch.clear()
            repoTournament.clear()
        }
    }

    @Nested
    inner class CreateTournament {
        @Test
        fun `should create a tournament with the given fields`() =
            trx.run {
                val tournament =
                    repoTournament.createTournament(
                        name = "open",
                        location = "porto",
                        startDate = now(),
                        endDate = null,
                        createdAt = now(),
                    )

                assertNotEquals(0, tournament.id)
                assertEquals("open", tournament.name)
                assertEquals("porto", tournament.location)
                assertEquals(TournamentStatus.DRAFT, tournament.status)
            }
    }

    @Nested
    inner class FindByStatus {
        @Test
        fun `should return tournaments with the given status`() =
            trx.run {
                newTournament()
                newTournament()

                val tournaments = repoTournament.findByStatus(TournamentStatus.DRAFT)

                assertEquals(2, tournaments.size)
            }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `should update the tournament status`() =
            trx.run {
                val created = newTournament()

                val updated = repoTournament.updateStatus(created.id, TournamentStatus.LIVE)

                assertNotNull(updated)
                assertEquals(TournamentStatus.LIVE, updated?.status)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            trx.run {
                assertNull(repoTournament.updateStatus(-1, TournamentStatus.LIVE))
            }
    }

    @Nested
    inner class FindByName {
        @Test
        fun `should find a tournament by name`() =
            trx.run {
                val created = newTournament(name = "unique-open")

                val found = repoTournament.findByName("unique-open")

                assertNotNull(found)
                assertEquals(created.name, found?.name)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            trx.run {
                assertNull(repoTournament.findByName("nonexistent"))
            }
    }

    @Nested
    inner class CreateTournamentState {
        @Test
        fun `should create a tournament state for an existing tournament`() =
            trx.run {
                val tournament = newTournament()

                val state = repoTournament.createTournamentState(tournament.id, now())

                assertNotNull(state)
                assertEquals(tournament.id, state?.tournamentId)
                assertEquals(ScreenState.WAITING, state?.currentScreen)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            trx.run {
                assertNull(repoTournament.createTournamentState(-1, now()))
            }
    }

    @Nested
    inner class FindStateByTournamentId {
        @Test
        fun `should find an existing tournament state by tournament id`() =
            trx.run {
                val tournament = newTournament()
                repoTournament.createTournamentState(tournament.id, now())

                val state = repoTournament.findStateByTournamentId(tournament.id)

                assertNotNull(state)
                assertEquals(tournament.id, state?.tournamentId)
            }

        @Test
        fun `should return null when state does not exist`() =
            trx.run {
                assertNull(repoTournament.findStateByTournamentId(-1))
            }
    }

    @Nested
    inner class UpdateScreen {
        @Test
        fun `should update the tournament screen`() =
            trx.run {
                val tournament = newTournament()
                repoTournament.createTournamentState(tournament.id, now())

                val updated =
                    repoTournament.updateScreen(
                        tournamentId = tournament.id,
                        screen = ScreenState.ROUTINES,
                        currentMatchId = null,
                        updatedAt = now().plusSeconds(10),
                    )

                assertNotNull(updated)
                assertEquals(ScreenState.ROUTINES, updated?.currentScreen)
            }

        @Test
        fun `should return null when tournament state does not exist`() =
            trx.run {
                assertNull(
                    repoTournament.updateScreen(
                        tournamentId = -1,
                        screen = ScreenState.WAITING,
                        currentMatchId = null,
                        updatedAt = now(),
                    ),
                )
            }
    }

    @Nested
    inner class CreateBracket {
        @Test
        fun `should create a bracket for an existing tournament`() =
            trx.run {
                val tournament = newTournament()

                val bracket =
                    repoTournament.createBracket(
                        tournamentId = tournament.id,
                        gender = GenderType.MALE,
                        stage = BracketStage.QUARTERFINALS,
                        createdAt = now(),
                    )

                assertNotNull(bracket)
                assertEquals(tournament.id, bracket?.tournamentId)
                assertEquals(BracketStage.QUARTERFINALS, bracket?.stage)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            trx.run {
                assertNull(
                    repoTournament.createBracket(
                        tournamentId = -1,
                        gender = GenderType.MALE,
                        stage = BracketStage.FINALS,
                        createdAt = now(),
                    ),
                )
            }
    }

    @Nested
    inner class FindBracketsByTournamentId {
        @Test
        fun `should return brackets for a tournament`() =
            trx.run {
                val tournament = newTournament()
                repoTournament.createBracket(tournament.id, GenderType.MALE, BracketStage.QUALIFIERS, now())
                repoTournament.createBracket(tournament.id, GenderType.FEMALE, BracketStage.SEMIFINALS, now())

                val brackets = repoTournament.findBracketsByTournamentId(tournament.id)

                assertEquals(2, brackets.size)
            }
    }

    @Nested
    inner class FindBracketsByTournamentIdAndGender {
        @Test
        fun `should return only brackets matching the given gender`() =
            trx.run {
                val tournament = newTournament()
                repoTournament.createBracket(tournament.id, GenderType.MALE, BracketStage.QUALIFIERS, now())
                repoTournament.createBracket(tournament.id, GenderType.FEMALE, BracketStage.SEMIFINALS, now())

                val brackets = repoTournament.findBracketsByTournamentIdAndGender(tournament.id, GenderType.FEMALE)

                assertEquals(1, brackets.size)
                assertEquals(GenderType.FEMALE, brackets.first().gender)
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing tournament by id`() =
            trx.run {
                val created = newTournament()

                val found = repoTournament.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoTournament.findById(-1))
            }
    }

    @Nested
    inner class FindByBracketId {
        @Test
        fun `should find the bracket by its id`() =
            trx.run {
                val tournament = newTournament()
                val bracket = repoTournament.createBracket(tournament.id, GenderType.MALE, BracketStage.QUALIFIERS, now())!!

                val found = repoTournament.findByBracketId(bracket.id)

                assertNotNull(found)
                assertEquals(bracket.id, found?.id)
                assertEquals(tournament.id, found?.tournamentId)
            }

        @Test
        fun `should return null when bracket id does not exist`() =
            trx.run {
                assertNull(repoTournament.findByBracketId(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created tournaments`() =
            trx.run {
                newTournament()
                newTournament()

                assertEquals(2, repoTournament.findAll().size)
            }

        @Test
        fun `should return empty list when there are no tournaments`() =
            trx.run {
                assertTrue(repoTournament.findAll().isEmpty())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing tournament`() =
            trx.run {
                val created = newTournament()
                val saved = repoTournament.save(created.copy(name = "updated-name"))

                assertNotNull(saved)
                assertEquals("updated-name", saved?.name)
                assertEquals(created.id, saved?.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the tournament`() =
            trx.run {
                val created = newTournament()

                repoTournament.deleteById(created.id)

                assertNull(repoTournament.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove tournaments, brackets and states`() =
            trx.run {
                val tournament = newTournament()
                repoTournament.createTournamentState(tournament.id, now())
                repoTournament.createBracket(tournament.id, GenderType.MALE, BracketStage.QUALIFIERS, now())

                repoTournament.clear()

                assertTrue(repoTournament.findAll().isEmpty())
            }
    }

    private fun Transaction.now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newTournament(name: String = "tournament-${System.nanoTime()}"): Tournament =
        repoTournament.createTournament(
            name = name,
            location = null,
            startDate = null,
            endDate = null,
            createdAt = now(),
        )
}
