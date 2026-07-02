package com.caliarena.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
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

@SpringBootTest(classes = [TestConfiguration::class])
class TournamentRepositoryTest {
    @Autowired
    lateinit var transactionManager: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        transactionManager.run {
            repoMatch.clear()
            repoTournament.clear()
        }
    }

    @Nested
    inner class CreateTournament {
        @Test
        fun `should create a tournament with the given fields`() =
            transactionManager.run {
                val tournament =
                    repoTournament.createTournament(
                        name = "open",
                        location = "porto",
                        startDate = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                        endDate = null,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
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
            transactionManager.run {
                repoTournament.createTournament("t1", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createTournament("t2", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val tournaments = repoTournament.findByStatus(TournamentStatus.DRAFT)

                assertEquals(2, tournaments.size)
            }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `should update the tournament status`() =
            transactionManager.run {
                val created = repoTournament.createTournament("t3", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val updated = repoTournament.updateStatus(created.id, TournamentStatus.LIVE)

                assertNotNull(updated)
                assertEquals(TournamentStatus.LIVE, updated?.status)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            transactionManager.run {
                assertNull(repoTournament.updateStatus(-1, TournamentStatus.LIVE))
            }
    }

    @Nested
    inner class CreateTournamentState {
        @Test
        fun `should create a tournament state for an existing tournament`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t4", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val state = repoTournament.createTournamentState(tournament.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                assertNotNull(state)
                assertEquals(tournament.id, state?.tournamentId)
                assertEquals(ScreenState.WAITING, state?.currentScreen)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            transactionManager.run {
                assertNull(repoTournament.createTournamentState(-1, Instant.now().truncatedTo(ChronoUnit.SECONDS)))
            }
    }

    @Nested
    inner class FindStateByTournamentId {
        @Test
        fun `should find an existing tournament state by tournament id`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t5", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createTournamentState(tournament.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val state = repoTournament.findStateByTournamentId(tournament.id)

                assertNotNull(state)
                assertEquals(tournament.id, state?.tournamentId)
            }

        @Test
        fun `should return null when state does not exist`() =
            transactionManager.run {
                assertNull(repoTournament.findStateByTournamentId(-1))
            }
    }

    @Nested
    inner class UpdateScreen {
        @Test
        fun `should update the tournament screen`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t6", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createTournamentState(tournament.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val updated =
                    repoTournament.updateScreen(
                        tournamentId = tournament.id,
                        screen = ScreenState.BRACKET,
                        currentMatchId = null,
                        updatedAt = Instant.now().plusSeconds(10).truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(updated)
                assertEquals(ScreenState.BRACKET, updated?.currentScreen)
            }

        @Test
        fun `should return null when tournament state does not exist`() =
            transactionManager.run {
                assertNull(
                    repoTournament.updateScreen(
                        tournamentId = -1,
                        screen = ScreenState.BRACKET,
                        currentMatchId = null,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }
    }

    @Nested
    inner class CreateBracket {
        @Test
        fun `should create a bracket for an existing tournament`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t7", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val bracket =
                    repoTournament.createBracket(
                        tournamentId = tournament.id,
                        gender = GenderType.MALE,
                        stage = BracketStage.QUARTERFINALS,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(bracket)
                assertEquals(tournament.id, bracket?.tournamentId)
                assertEquals(BracketStage.QUARTERFINALS, bracket?.stage)
            }

        @Test
        fun `should return null when tournament does not exist`() =
            transactionManager.run {
                assertNull(
                    repoTournament.createBracket(
                        tournamentId = -1,
                        gender = GenderType.MALE,
                        stage = BracketStage.FINALS,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }
    }

    @Nested
    inner class FindBracketsByTournamentId {
        @Test
        fun `should return brackets for a tournament`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t8", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createBracket(
                    tournament.id,
                    GenderType.MALE,
                    BracketStage.QUALIFIERS,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )
                repoTournament.createBracket(
                    tournament.id,
                    GenderType.FEMALE,
                    BracketStage.SEMIFINALS,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )

                val brackets = repoTournament.findBracketsByTournamentId(tournament.id)

                assertEquals(2, brackets.size)
            }
    }

    @Nested
    inner class FindBracketsByTournamentIdAndGender {
        @Test
        fun `should return brackets for a tournament and gender`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t9", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createBracket(
                    tournament.id,
                    GenderType.MALE,
                    BracketStage.QUALIFIERS,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )
                repoTournament.createBracket(
                    tournament.id,
                    GenderType.FEMALE,
                    BracketStage.SEMIFINALS,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )

                val brackets = repoTournament.findBracketsByTournamentIdAndGender(tournament.id, GenderType.FEMALE)

                assertEquals(1, brackets.size)
                assertEquals(GenderType.FEMALE, brackets.first().gender)
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing tournament by id`() =
            transactionManager.run {
                val created = repoTournament.createTournament("t10", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val found = repoTournament.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            transactionManager.run {
                assertNull(repoTournament.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created tournaments`() =
            transactionManager.run {
                repoTournament.createTournament("t11", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createTournament("t12", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val tournaments = repoTournament.findAll()

                assertEquals(2, tournaments.size)
            }

        @Test
        fun `should return empty list when there are no tournaments`() =
            transactionManager.run {
                assertTrue(repoTournament.findAll().isEmpty())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing tournament`() =
            transactionManager.run {
                val created = repoTournament.createTournament("t13", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                val updated = created.copy(name = "t13-updated")

                val saved = repoTournament.save(updated)

                assertNotNull(saved)
                assertEquals("t13-updated", saved?.name)
                assertEquals(created.id, saved?.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the tournament`() =
            transactionManager.run {
                val created = repoTournament.createTournament("t14", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                repoTournament.deleteById(created.id)

                assertNull(repoTournament.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove tournaments, brackets and states`() =
            transactionManager.run {
                val tournament = repoTournament.createTournament("t15", null, null, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createTournamentState(tournament.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoTournament.createBracket(
                    tournament.id,
                    GenderType.MALE,
                    BracketStage.QUALIFIERS,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )

                repoTournament.clear()

                assertTrue(repoTournament.findAll().isEmpty())
            }
    }
}
