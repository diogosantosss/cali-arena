package com.caliarena.service

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentStateEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class TournamentServiceTest : ServiceTest() {
    private lateinit var service: TournamentService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.tournaments).thenReturn(tournaments)
        lenient().whenever(transaction.tournamentStates).thenReturn(tournamentStates)
        lenient().whenever(transaction.matches).thenReturn(matches)
        lenient().whenever(transaction.brackets).thenReturn(brackets)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = TournamentService(trxManager, clock, mock())
    }

    private val now = clock.instant()

    private fun tournamentEntity(id: Int = 1) =
        TournamentEntity(
            id = id,
            name = "Tournament $id",
            location = "Location",
            startDate = now.epochSecond,
            endDate = null,
            status = TournamentStatus.READY,
            createdAt = now.epochSecond,
        )

    private fun stateEntity(
        tournament: TournamentEntity = tournamentEntity(),
        screen: ScreenState = ScreenState.BATTLE,
    ) = TournamentStateEntity(
        id = 1,
        tournament = tournament,
        currentScreen = screen,
        updatedAt = now.epochSecond,
    )

    private fun matchEntity(id: Int = 1) =
        MatchEntity(
            id = id,
            bracket =
                com.caliarena.repo.entities.tournament
                    .BracketEntity(),
            routineId = 1,
            judge = UserEntity(1, "judge", "hash", UserRole.JUDGE, now.epochSecond),
            athleteRed = null,
            athleteBlue = null,
            status = com.caliarena.domain.match.MatchStatus.FINISHED,
            startedAt = now.toEpochMilli(),
            finishedAt = now.plusSeconds(60).toEpochMilli(),
            createdAt = now.epochSecond,
        )

    private fun bracketEntity(
        id: Int = 1,
        tournament: TournamentEntity = tournamentEntity(),
    ) = BracketEntity(
        id = id,
        tournament = tournament,
        division = "FEMALE",
        stage = com.caliarena.domain.bracket.BracketStage.QUALIFIERS,
        createdAt = now.epochSecond,
    )

    @Nested
    inner class CreateTournament {
        @Test
        fun `should create tournament successfully`() {
            val saved = tournamentEntity(2)
            `when`(tournaments.findByName("New")).thenReturn(null as TournamentEntity?)
            `when`(tournaments.save(any())).thenReturn(saved)
            `when`(tournaments.findById(2)).thenReturn(Optional.of(saved))
            `when`(tournamentStates.save(any())).thenReturn(stateEntity(saved))

            val result = service.createTournament("New", "Loc", now, now.plusSeconds(3600))

            assertTrue(result is Either.Right)
        }

        @Test
        fun `should fail when name already exists`() {
            lenient().whenever(tournaments.findByName("Existing")).thenReturn(tournamentEntity())

            val result = service.createTournament("Existing", "Loc", now, now.plusSeconds(3600))

            assertEquals(failure(ApiError.TOURNAMENT_ALREADY_EXISTS), result)
        }
    }

    @Nested
    inner class GetTournamentById {
        @Test
        fun `should return tournament by id`() {
            lenient().whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.getTournamentById(1)

            assertEquals(success(tournamentEntity().toDomain()), result)
        }

        @Test
        fun `should fail when not found`() {
            lenient().whenever(tournaments.findById(99)).thenReturn(Optional.empty())

            val result = service.getTournamentById(99)

            assertEquals(failure(ApiError.TOURNAMENT_NOT_FOUND), result)
        }
    }

    @Nested
    inner class UpdateTournamentStatus {
        @Test
        fun `should update status`() {
            val existing = tournamentEntity()
            lenient().whenever(tournaments.findById(1)).thenReturn(Optional.of(existing))
            lenient().whenever(tournaments.save(existing)).thenReturn(existing)

            val result = service.updateTournamentStatus(1, "LIVE")

            assertEquals(success(existing.toDomain()), result)
            assertEquals(TournamentStatus.LIVE, existing.status)
        }

        @Test
        fun `should fail when not found`() {
            lenient().whenever(tournaments.findById(99)).thenReturn(Optional.empty())

            val result = service.updateTournamentStatus(99, "LIVE")

            assertEquals(failure(ApiError.TOURNAMENT_NOT_FOUND), result)
        }

        @Test
        fun `should fail on invalid status`() {
            lenient().whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.updateTournamentStatus(1, "INVALID")

            assertEquals(failure(ApiError.INVALID_TOURNAMENT_STATUS), result)
        }
    }

    @Nested
    inner class GetTournamentState {
        @Test
        fun `should return state successfully`() {
            val tournament = tournamentEntity()
            val state =
                TournamentStateEntity(
                    tournament = tournament,
                    currentScreen = ScreenState.BATTLE,
                    updatedAt = now.epochSecond,
                )
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(state)

            val result = service.getTournamentState(1)

            assertEquals(success(state.toDomain()), result)
        }

        @Test
        fun `should fail when state does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(null)

            val result = service.getTournamentState(1)

            assertEquals(failure(ApiError.TOURNAMENT_STATE_NOT_FOUND), result)
        }
    }

    @Nested
    inner class UpdateScreen {
        @Test
        fun `should update screen successfully`() {
            val tournament = tournamentEntity()
            val state =
                TournamentStateEntity(
                    tournament = tournament,
                    currentScreen = ScreenState.WAITING,
                    updatedAt = now.epochSecond,
                )
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(state)
            whenever(tournamentStates.save(any())).thenReturn(state)

            val result = service.updateScreen(1, "BATTLE", null, null, null)

            assertTrue(result is Either.Right)
            assertEquals(ScreenState.BATTLE, (result as Either.Right).value.currentScreen)
        }

        @Test
        fun `should set current match when provided`() {
            val tournament = tournamentEntity()
            val state =
                TournamentStateEntity(
                    tournament = tournament,
                    currentScreen = ScreenState.WAITING,
                    updatedAt = now.epochSecond,
                )
            val match = matchEntity()
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(state)
            whenever(matches.findById(match.id)).thenReturn(Optional.of(match))
            whenever(tournamentStates.save(any())).thenReturn(state)

            val result = service.updateScreen(1, "BATTLE", match.id, null, null)

            assertTrue(result is Either.Right)
        }

        @Test
        fun `should set current bracket when provided`() {
            val tournament = tournamentEntity()
            val state =
                TournamentStateEntity(
                    tournament = tournament,
                    currentScreen = ScreenState.WAITING,
                    updatedAt = now.epochSecond,
                )
            val bracket = bracketEntity(tournament = tournament)
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(state)
            whenever(brackets.findById(bracket.id)).thenReturn(Optional.of(bracket))
            whenever(tournamentStates.save(any())).thenReturn(state)

            val result = service.updateScreen(1, "LEADERBOARD", null, bracket.id, null)

            assertTrue(result is Either.Right)
            assertEquals(bracket.id, (result as Either.Right).value.currentBracketId)
        }

        @Test
        fun `should set current division when provided`() {
            val tournament = tournamentEntity()
            val state =
                TournamentStateEntity(
                    tournament = tournament,
                    currentScreen = ScreenState.WAITING,
                    updatedAt = now.epochSecond,
                )
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(state)
            whenever(brackets.findByTournamentIdAndDivision(1, "FEMALE")).thenReturn(listOf(bracketEntity(tournament = tournament)))
            whenever(tournamentStates.save(any())).thenReturn(state)

            val result = service.updateScreen(1, "BRACKETS", null, null, "FEMALE")

            assertTrue(result is Either.Right)
            assertEquals("FEMALE", (result as Either.Right).value.currentDivision)
        }

        @Test
        fun `should fail when division has no brackets`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findByTournamentIdAndDivision(1, "FEMALE")).thenReturn(emptyList())

            val result = service.updateScreen(1, "BRACKETS", null, null, "FEMALE")

            assertEquals(failure(ApiError.INVALID_BRACKET_DIVISION), result)
        }

        @Test
        fun `should fail when division is blank`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.updateScreen(1, "BRACKETS", null, null, "   ")

            assertEquals(failure(ApiError.INVALID_BRACKET_DIVISION), result)
        }

        @Test
        fun `should fail when bracket does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findById(99)).thenReturn(Optional.empty())

            val result = service.updateScreen(1, "LEADERBOARD", null, 99, null)

            assertEquals(failure(ApiError.BRACKET_NOT_FOUND), result)
        }

        @Test
        fun `should fail when bracket belongs to another tournament`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            val otherBracket = bracketEntity(tournament = tournamentEntity(id = 2))
            whenever(brackets.findById(otherBracket.id)).thenReturn(Optional.of(otherBracket))

            val result = service.updateScreen(1, "LEADERBOARD", null, otherBracket.id, null)

            assertEquals(failure(ApiError.BRACKET_NOT_FOUND), result)
        }

        @Test
        fun `should fail when screen is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.updateScreen(1, "INVALID", null, null, null)

            assertEquals(failure(ApiError.INVALID_SCREEN_STATE), result)
        }

        @Test
        fun `should fail when state does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(null)

            val result = service.updateScreen(1, "BATTLE", null, null, null)

            assertEquals(failure(ApiError.TOURNAMENT_STATE_NOT_FOUND), result)
        }
    }
}
