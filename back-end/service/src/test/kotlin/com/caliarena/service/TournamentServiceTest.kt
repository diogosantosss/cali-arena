package com.caliarena.service

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class TournamentServiceTest : ServiceTest() {
    private lateinit var service: TournamentService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.tournaments).thenReturn(tournaments)
        lenient().whenever(transaction.brackets).thenReturn(brackets)
        lenient().whenever(transaction.tournamentStates).thenReturn(tournamentStates)
        lenient().whenever(transaction.matches).thenReturn(matches)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = TournamentService(trxManager, clock, mock())
    }

    private val now = clock.instant()

    private fun tournamentEntity(
        id: Int = 1,
        name: String = "Open Setúbal",
        status: TournamentStatus = TournamentStatus.DRAFT,
    ) = TournamentEntity(
        id = id,
        name = name,
        location = "Setúbal",
        startDate = now.epochSecond,
        endDate = now.plusSeconds(3600).epochSecond,
        status = status,
        createdAt = now.epochSecond,
    )

    private fun bracketEntity(
        id: Int = 1,
        tournament: TournamentEntity = tournamentEntity(),
        gender: GenderType = GenderType.MALE,
        stage: BracketStage = BracketStage.QUALIFIERS,
    ) = BracketEntity(id = id, tournament = tournament, gender = gender, stage = stage, createdAt = now.epochSecond)

    private fun matchEntity(id: Int = 1): MatchEntity {
        val tournament = tournamentEntity()
        val judge = UserEntity(3, "judge", "hash", UserRole.JUDGE, now.epochSecond)
        val club = ClubEntity(1, "club", null, now.epochSecond)
        val red = AthleteEntity(10, "red", GenderType.MALE, club, now.epochSecond)
        val blue = AthleteEntity(20, "blue", GenderType.MALE, club, now.epochSecond)
        return MatchEntity(
            id = id,
            bracket = bracketEntity(tournament = tournament),
            routineId = 2,
            judge = judge,
            athleteRed = red,
            athleteBlue = blue,
            status = MatchStatus.PENDING,
            createdAt = now.epochSecond,
        )
    }

    @Nested
    inner class CreateTournament {
        @Test
        fun `should create tournament successfully`() {
            whenever(tournaments.findByName("Open Setúbal")).thenReturn(null)
            whenever(tournaments.save(any())).thenReturn(tournamentEntity())
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.createTournament("Open Setúbal", "Setúbal", now, now.plusSeconds(3600))

            assertEquals(success(tournamentEntity().toDomain()), result)

            verify(tournamentStates).save(any())
        }

        @Test
        fun `should fail when name already exists`() {
            whenever(tournaments.findByName("Open Setúbal")).thenReturn(tournamentEntity())

            val result = service.createTournament("Open Setúbal", "Setúbal", now, now.plusSeconds(3600))

            assertEquals(failure(TournamentError.TournamentAlreadyExists), result)
        }
    }

    @Nested
    inner class GetTournamentById {
        @Test
        fun `should return tournament when found`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.getTournamentById(1)

            assertEquals(success(tournamentEntity().toDomain()), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.empty())

            val result = service.getTournamentById(1)

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetAllTournaments {
        @Test
        fun `should return all tournaments`() {
            val list = listOf(tournamentEntity(1), tournamentEntity(2, name = "Other"))
            whenever(tournaments.findAll()).thenReturn(list)

            val result = service.getAllTournaments()

            assertEquals(list.map { it.toDomain() }, result)
        }
    }

    @Nested
    inner class GetTournamentsByStatus {
        @Test
        fun `should return tournaments filtered by status`() {
            val list = listOf(tournamentEntity(status = TournamentStatus.LIVE))
            whenever(tournaments.findByStatus(TournamentStatus.LIVE)).thenReturn(list)

            val result = service.getTournamentsByStatus(TournamentStatus.LIVE)

            assertEquals(list.map { it.toDomain() }, result)
        }
    }

    @Nested
    inner class UpdateTournamentStatus {
        @Test
        fun `should update status successfully`() {
            val updated = tournamentEntity(status = TournamentStatus.FINISHED)
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(tournaments.save(any())).thenReturn(updated)

            val result = service.updateTournamentStatus(1, "FINISHED")

            assertEquals(success(updated.toDomain()), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.empty())

            val result = service.updateTournamentStatus(1, "FINISHED")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }

        @Test
        fun `should fail when status is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.updateTournamentStatus(1, "INVALID")

            assertEquals(failure(TournamentError.InvalidTournamentStatus), result)
        }
    }

    @Nested
    inner class CreateBracket {
        @Test
        fun `should create bracket successfully`() {
            val tournament = tournamentEntity()
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(emptyList())
            whenever(brackets.save(any())).thenReturn(bracketEntity())

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(success(bracketEntity().toDomain()), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.empty())

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }

        @Test
        fun `should fail when gender is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.createBracket(1, "INVALID", "QUALIFIERS")

            assertEquals(failure(TournamentError.InvalidGender), result)
        }

        @Test
        fun `should fail when stage is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.createBracket(1, "MALE", "INVALID")

            assertEquals(failure(TournamentError.InvalidBracketStage), result)
        }

        @Test
        fun `should fail when bracket already exists for stage`() {
            val existing =
                bracketEntity(stage = BracketStage.QUALIFIERS)
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(listOf(existing))

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(failure(TournamentError.BracketAlreadyExists), result)

            verify(brackets, never()).save(any())
        }
    }

    @Nested
    inner class GetBracketsByTournament {
        @Test
        fun `should return brackets of the tournament`() {
            val list = listOf(bracketEntity())
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findByTournamentId(1)).thenReturn(list)

            val result = service.getBracketsByTournament(1)

            assertEquals(success(list.map { it.toDomain() }), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.empty())

            val result = service.getBracketsByTournament(1)

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetBracketOverview {
        @Test
        fun `should return overview with matches per bracket`() {
            val tournament = tournamentEntity()
            val bracket = bracketEntity(tournament = tournament)
            val matchList = listOf(matchEntity(1))
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(listOf(bracket))
            whenever(matches.findByBracketId(bracket.id)).thenReturn(matchList)

            val result = service.getBracketOverview(1, "MALE")

            assertTrue(result is Either.Right)

            val overview = (result as Either.Right).value
            assertEquals(1, overview.size)
            assertEquals(1, overview.first().matches.size)
        }

        @Test
        fun `should fail when gender is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.getBracketOverview(1, "INVALID")

            assertEquals(failure(TournamentError.InvalidGender), result)
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

            assertEquals(failure(TournamentError.TournamentStateNotFound), result)
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

            val result = service.updateScreen(1, "BATTLE", null)

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

            val result = service.updateScreen(1, "BATTLE", match.id)

            assertTrue(result is Either.Right)
        }

        @Test
        fun `should fail when screen is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.updateScreen(1, "INVALID", null)

            assertEquals(failure(TournamentError.InvalidScreenState), result)
        }

        @Test
        fun `should fail when state does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(tournamentStates.findByTournamentId(1)).thenReturn(null)

            val result = service.updateScreen(1, "BATTLE", null)

            assertEquals(failure(TournamentError.TournamentStateNotFound), result)
        }
    }
}
