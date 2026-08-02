package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class TournamentServiceTest : ServiceTest() {
    private lateinit var service: TournamentService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoTournament).thenReturn(repoTournament)
        lenient().whenever(transaction.repoMatch).thenReturn(repoMatch)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = TournamentService(trxManager, clock)
    }

    private val now = clock.instant()

    @Nested
    inner class CreateTournament {
        private val tournament =
            Tournament(
                id = 1,
                name = "Open Setúbal",
                location = "Setúbal",
                startDate = now,
                endDate = now.plusSeconds(3600),
                status = TournamentStatus.DRAFT,
                createdAt = now,
            )

        @Test
        fun `should create tournament successfully`() {
            val now = clock.instant()
            val tournamentState = mock(TournamentState::class.java)

            whenever(repoTournament.findByName("Open Setúbal")).thenReturn(null)
            whenever(
                repoTournament.createTournament(
                    name = "Open Setúbal",
                    location = "Setúbal",
                    startDate = now,
                    endDate = now.plusSeconds(3600),
                    createdAt = now,
                ),
            ).thenReturn(tournament)

            whenever(
                repoTournament.createTournamentState(
                    tournamentId = tournament.id,
                    updatedAt = now,
                ),
            ).thenReturn(tournamentState)

            val result =
                service.createTournament("Open Setúbal", "Setúbal", now, now.plusSeconds(3600))

            assertEquals(success(tournament), result)
        }

        @Test
        fun `should fail when tournament already exists`() {
            whenever(repoTournament.findByName("Open Setúbal")).thenReturn(tournament)

            val result =
                service.createTournament("Open Setúbal", "Setúbal", now, now.plusSeconds(3600))

            assertEquals(failure(TournamentError.TournamentAlreadyExists), result)
        }
    }

    @Nested
    inner class GetTournamentById {
        private val tournament =
            Tournament(
                id = 1,
                name = "Open Setúbal",
                location = "Setúbal",
                startDate = now,
                endDate = now.plusSeconds(3600),
                status = TournamentStatus.DRAFT,
                createdAt = now,
            )

        @Test
        fun `should return tournament when found`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)

            val result = service.getTournamentById(1)

            assertEquals(success(tournament), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.getTournamentById(1)

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetAllTournaments {
        private val tournaments =
            listOf(
                Tournament(1, "A", null, null, null, TournamentStatus.DRAFT, now),
                Tournament(2, "B", null, null, null, TournamentStatus.READY, now),
            )

        @Test
        fun `should return all tournaments`() {
            whenever(repoTournament.findAll()).thenReturn(tournaments)

            val result = service.getAllTournaments()

            assertEquals(tournaments, result)
        }
    }

    @Nested
    inner class GetTournamentsByStatus {
        private val tournaments =
            listOf(
                Tournament(1, "A", null, null, null, TournamentStatus.READY, now),
                Tournament(2, "B", null, null, null, TournamentStatus.READY, now),
            )

        @Test
        fun `should return tournaments by status`() {
            whenever(repoTournament.findByStatus(TournamentStatus.READY)).thenReturn(tournaments)

            val result = service.getTournamentsByStatus(TournamentStatus.READY)

            assertEquals(tournaments, result)
        }
    }

    @Nested
    inner class UpdateTournamentStatus {
        private val tournament =
            Tournament(
                id = 1,
                name = "Open",
                location = null,
                startDate = null,
                endDate = null,
                status = TournamentStatus.DRAFT,
                createdAt = now,
            )

        @Test
        fun `should update status successfully`() {
            val updated = tournament.copy(status = TournamentStatus.READY)

            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.updateStatus(1, TournamentStatus.READY)).thenReturn(updated)

            val result = service.updateTournamentStatus(1, "READY")

            assertEquals(success(updated), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.updateTournamentStatus(1, "READY")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }

        @Test
        fun `should fail when repository fails to update`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.updateStatus(1, TournamentStatus.READY)).thenReturn(null)

            val result = service.updateTournamentStatus(1, "READY")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class CreateBracket {
        private val tournament =
            Tournament(1, "Open", null, null, null, TournamentStatus.DRAFT, now)

        private val bracket =
            Bracket(
                id = 10,
                tournamentId = 1,
                gender = GenderType.MALE,
                stage = BracketStage.QUALIFIERS,
                createdAt = now,
            )

        @Test
        fun `should create bracket successfully`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findBracketsByTournamentIdAndGender(1, GenderType.MALE))
                .thenReturn(emptyList())
            whenever(
                repoTournament.createBracket(
                    tournamentId = 1,
                    gender = GenderType.MALE,
                    stage = BracketStage.QUALIFIERS,
                    createdAt = now,
                ),
            ).thenReturn(bracket)

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(success(bracket), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }

        @Test
        fun `should fail when bracket already exists`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findBracketsByTournamentIdAndGender(1, GenderType.MALE))
                .thenReturn(listOf(bracket))

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(failure(TournamentError.BracketAlreadyExists), result)
        }

        @Test
        fun `should fail when repository fails to create bracket`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findBracketsByTournamentIdAndGender(1, GenderType.MALE))
                .thenReturn(emptyList())
            whenever(
                repoTournament.createBracket(
                    tournamentId = 1,
                    gender = GenderType.MALE,
                    stage = BracketStage.QUALIFIERS,
                    createdAt = now,
                ),
            ).thenReturn(null)

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetBracketsByTournament {
        private val tournament =
            Tournament(1, "Open", null, null, null, TournamentStatus.DRAFT, now)

        private val brackets =
            listOf(
                Bracket(10, 1, GenderType.MALE, BracketStage.QUALIFIERS, now),
                Bracket(11, 1, GenderType.FEMALE, BracketStage.FINALS, now),
            )

        @Test
        fun `should return brackets successfully`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findBracketsByTournamentId(1)).thenReturn(brackets)

            val result = service.getBracketsByTournament(1)

            assertEquals(success(brackets), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.getBracketsByTournament(1)

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetBracketsByTournamentAndGender {
        private val tournament =
            Tournament(1, "Open", null, null, null, TournamentStatus.DRAFT, now)

        private val brackets =
            listOf(
                Bracket(10, 1, GenderType.MALE, BracketStage.QUALIFIERS, now),
            )

        @Test
        fun `should return brackets successfully`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findBracketsByTournamentIdAndGender(1, GenderType.MALE))
                .thenReturn(brackets)

            val result = service.getBracketsByTournamentAndGender(1, "MALE")

            assertEquals(success(brackets), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.getBracketsByTournamentAndGender(1, "MALE")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetBracketOverview {
        private val tournament =
            Tournament(1, "Open", null, null, null, TournamentStatus.DRAFT, now)

        private val bracket =
            Bracket(10, 1, GenderType.MALE, BracketStage.QUALIFIERS, now)

        private val matches =
            listOf(
                Match(
                    id = 1,
                    bracketId = 10,
                    routineId = 5,
                    judgeId = 1,
                    athleteRedId = 100,
                    athleteBlueId = 200,
                    status = MatchStatus.PENDING,
                    winnerAthleteId = null,
                    startedAt = null,
                    finishedAt = null,
                    createdAt = now,
                ),
            )

        @Test
        fun `should return bracket overview successfully`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findBracketsByTournamentIdAndGender(1, GenderType.MALE))
                .thenReturn(listOf(bracket))
            whenever(repoMatch.findByBracketId(10)).thenReturn(matches)

            val result = service.getBracketOverview(1, "MALE")

            val expected =
                listOf(
                    BracketOverview(
                        bracket = bracket,
                        matches = matches,
                    ),
                )

            assertEquals(success(expected), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.getBracketOverview(1, "MALE")

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }
    }

    @Nested
    inner class GetTournamentState {
        private val tournament =
            Tournament(1, "Open", null, null, null, TournamentStatus.DRAFT, now)

        private val state =
            TournamentState(
                id = 1,
                tournamentId = 1,
                currentScreen = ScreenState.WAITING,
                currentMatchId = null,
                updatedAt = now,
            )

        @Test
        fun `should return tournament state successfully`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findStateByTournamentId(1)).thenReturn(state)

            val result = service.getTournamentState(1)

            assertEquals(success(state), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.getTournamentState(1)

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }

        @Test
        fun `should fail when state does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(repoTournament.findStateByTournamentId(1)).thenReturn(null)

            val result = service.getTournamentState(1)

            assertEquals(failure(TournamentError.TournamentStateNotFound), result)
        }
    }

    @Nested
    inner class UpdateScreen {
        private val tournament =
            Tournament(1, "Open", null, null, null, TournamentStatus.DRAFT, now)

        private val updatedState =
            TournamentState(
                id = 1,
                tournamentId = 1,
                currentScreen = ScreenState.BATTLE,
                currentMatchId = 99,
                updatedAt = now,
            )

        @Test
        fun `should update screen successfully`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(
                repoTournament.updateScreen(
                    tournamentId = 1,
                    screen = ScreenState.BATTLE,
                    currentMatchId = 99,
                    updatedAt = now,
                ),
            ).thenReturn(updatedState)

            val result = service.updateScreen(1, "BATTLE", 99)

            assertEquals(success(updatedState), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(repoTournament.findById(1)).thenReturn(null)

            val result = service.updateScreen(1, "BATTLE", 99)

            assertEquals(failure(TournamentError.TournamentNotFound), result)
        }

        @Test
        fun `should fail when repository fails to update screen`() {
            whenever(repoTournament.findById(1)).thenReturn(tournament)
            whenever(
                repoTournament.updateScreen(
                    tournamentId = 1,
                    screen = ScreenState.BATTLE,
                    currentMatchId = 99,
                    updatedAt = now,
                ),
            ).thenReturn(null)

            val result = service.updateScreen(1, "BATTLE", 99)

            assertEquals(failure(TournamentError.TournamentStateNotFound), result)
        }
    }
}
