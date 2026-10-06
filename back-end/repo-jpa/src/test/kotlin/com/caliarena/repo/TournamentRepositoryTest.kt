package com.caliarena.repo

import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentStateEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

private data class TournamentSubtree(
    val tournamentId: Int,
    val bracketId: Int,
    val matchId: Int,
    val routineId: Int,
    val redId: Int,
    val blueId: Int,
)

class TournamentRepositoryTest : AbstractRepositoryTest() {
    @Nested
    inner class Tournaments {
        @Test
        fun `should create a tournament with the given fields`() =
            trx.run {
                val created =
                    tournaments.save(
                        TournamentEntity(
                            name = "Nationals",
                            location = "Lisboa",
                            status = TournamentStatus.LIVE,
                            createdAt = now().epochSecond,
                        ),
                    )

                assertNotEquals(0, created.id)
                assertEquals("Nationals", created.name)
                assertEquals("Lisboa", created.location)
                assertEquals(TournamentStatus.LIVE, created.status)
            }

        @Test
        fun `should find an existing tournament by id`() =
            trx.run {
                val created = newTournament()

                assertNotNull(tournaments.findByIdOrNull(created.id))
            }

        @Test
        fun `should return null when tournament does not exist`() =
            trx.run {
                assertNull(tournaments.findByIdOrNull(-1))
            }

        @Test
        fun `should find tournaments by status`() =
            trx.run {
                val live = newTournament(status = TournamentStatus.LIVE)
                newTournament(status = TournamentStatus.DRAFT)

                val found = tournaments.findByStatus(TournamentStatus.LIVE)

                assertTrue(found.any { it.id == live.id })
                assertTrue(found.all { it.status == TournamentStatus.LIVE })
            }

        @Test
        fun `should list all tournaments`() =
            trx.run {
                newTournament()
                newTournament()

                assertTrue(tournaments.findAll().count() >= 2)
            }

        @Test
        fun `should update tournament status`() =
            trx.run {
                val created = newTournament(status = TournamentStatus.DRAFT)

                created.status = TournamentStatus.FINISHED
                tournaments.save(created)

                assertEquals(TournamentStatus.FINISHED, tournaments.findByIdOrNull(created.id)?.status)
            }
    }

    @Nested
    inner class Brackets {
        @Test
        fun `should create and find a bracket by id`() =
            trx.run {
                val tournament = newTournament()
                val created = newBracket(tournament)

                assertNotEquals(0, created.id)
                assertEquals(tournament.id, brackets.findByIdOrNull(created.id)?.tournament?.id)
            }

        @Test
        fun `should return brackets of a tournament`() =
            trx.run {
                val tournamentA = newTournament()
                val tournamentB = newTournament()
                val bracketA = newBracket(tournamentA)
                newBracket(tournamentB)

                val found = brackets.findByTournamentId(tournamentA.id)

                assertEquals(1, found.size)
                assertEquals(bracketA.id, found.first().id)
            }

        @Test
        fun `should filter brackets by division`() =
            trx.run {
                val tournament = newTournament()
                val male = newBracket(tournament, division = "ELITE MALE")
                newBracket(tournament, division = "FEMALE")

                val found = brackets.findByTournamentIdAndDivision(tournament.id, "ELITE MALE")

                assertEquals(1, found.size)
                assertEquals(male.id, found.first().id)
            }
    }

    @Nested
    inner class TournamentState {
        @Test
        fun `should create and read the state of a tournament`() =
            trx.run {
                val tournament = newTournament()
                val state =
                    tournamentStates.save(
                        TournamentStateEntity(tournament = tournament, currentScreen = ScreenState.BATTLE, updatedAt = now().epochSecond),
                    )

                val found = tournamentStates.findByTournamentId(tournament.id)

                assertNotNull(found)
                assertEquals(state.id, found?.id)
                assertEquals(ScreenState.BATTLE, found?.currentScreen)
                assertNull(found?.currentMatch?.id)
            }

        @Test
        fun `should point current match when set`() =
            trx.run {
                val tournament = newTournament()
                val match = newRunningMatch(tournament)

                tournamentStates.save(
                    TournamentStateEntity(
                        tournament = tournament,
                        currentScreen = ScreenState.BATTLE,
                        currentMatch = match,
                        updatedAt = now().epochSecond,
                    ),
                )

                val found = tournamentStates.findByTournamentId(tournament.id)

                assertNotNull(found)
                assertEquals(match.id, found?.currentMatch?.id)
            }

        @Test
        fun `should update the current screen`() =
            trx.run {
                val tournament = newTournament()
                val state =
                    tournamentStates.save(
                        TournamentStateEntity(tournament = tournament, currentScreen = ScreenState.WAITING, updatedAt = now().epochSecond),
                    )

                state.currentScreen = ScreenState.BATTLE
                tournamentStates.save(state)

                assertEquals(ScreenState.BATTLE, tournamentStates.findByTournamentId(tournament.id)?.currentScreen)
            }

        @Test
        fun `should not leave two states for the same tournament when replaced`() =
            trx.run {
                val tournament = newTournament()
                tournamentStates.save(
                    TournamentStateEntity(tournament = tournament, currentScreen = ScreenState.WAITING, updatedAt = now().epochSecond),
                )

                // simula o comportamento do service: atualizar o estado existente
                val existing = tournamentStates.findByTournamentId(tournament.id)!!
                existing.currentScreen = ScreenState.ROUTINES
                tournamentStates.save(existing)

                assertEquals(ScreenState.ROUTINES, tournamentStates.findByTournamentId(tournament.id)?.currentScreen)
                assertFalse(tournamentStates.findAll().count { it.tournament.id == tournament.id } > 1)
            }
    }

    @Nested
    inner class DeleteTournament {
        @Test
        fun `deleting a tournament removes its whole subtree but nothing global`() {
            val subtree =
                trx.run {
                    val tournament = newTournament()
                    val bracket = newBracket(tournament)
                    val red = newAthlete("red-${System.nanoTime()}")
                    val blue = newAthlete("blue-${System.nanoTime()}")
                    val routine = newRoutine("global-${System.nanoTime()}")
                    val match =
                        matches.save(
                            MatchEntity(
                                bracket = bracket,
                                routineId = routine.id,
                                athleteRed = red,
                                athleteBlue = blue,
                                status = MatchStatus.RUNNING,
                                createdAt = now().epochSecond,
                            ),
                        )
                    newProgress(match)
                    val judge = newUser("judge-${System.nanoTime()}")
                    assignJudge(tournament, judge)
                    newScreenRoutine(tournament)
                    tournamentStates.save(
                        TournamentStateEntity(
                            tournament = tournament,
                            currentScreen = ScreenState.BATTLE,
                            currentMatch = match,
                            currentBracket = bracket,
                            updatedAt = now().epochSecond,
                        ),
                    )

                    TournamentSubtree(tournament.id, bracket.id, match.id, routine.id, red.id, blue.id)
                }

            trx.run {
                val tournament = tournaments.findByIdOrNull(subtree.tournamentId) ?: error("tournament missing")
                tournaments.delete(tournament)
            }

            trx.run {
                assertNull(tournaments.findByIdOrNull(subtree.tournamentId))
                assertNull(tournamentStates.findByTournamentId(subtree.tournamentId))
                assertTrue(brackets.findByTournamentId(subtree.tournamentId).isEmpty())
                assertTrue(matches.findByBracketIdIn(listOf(subtree.bracketId)).isEmpty())
                assertNull(matchProgresses.findByMatchId(subtree.matchId))
                assertTrue(screenRoutines.findByTournamentIdOrderByDisplayOrder(subtree.tournamentId).isEmpty())
                assertTrue(tournamentJudges.findByTournamentId(subtree.tournamentId).isEmpty())
                assertTrue(routines.existsById(subtree.routineId))
                assertTrue(athletes.existsById(subtree.redId))
                assertTrue(athletes.existsById(subtree.blueId))
            }
        }
    }
}
