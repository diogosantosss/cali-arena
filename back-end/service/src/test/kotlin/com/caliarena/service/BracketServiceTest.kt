package com.caliarena.service

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.bracket.LeaderboardEntry
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class BracketServiceTest : ServiceTest() {
    private lateinit var service: BracketService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.tournaments).thenReturn(tournaments)
        lenient().whenever(transaction.brackets).thenReturn(brackets)
        lenient().whenever(transaction.matches).thenReturn(matches)
        lenient().whenever(transaction.matchProgresses).thenReturn(matchProgresses)
        lenient().whenever(transaction.athletes).thenReturn(athletes)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = BracketService(trxManager, clock)
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

    private fun bracketEntity(
        id: Int = 1,
        tournament: TournamentEntity = tournamentEntity(),
        stage: BracketStage = BracketStage.QUALIFIERS,
    ) = BracketEntity(
        id = id,
        tournament = tournament,
        gender = GenderType.MALE,
        stage = stage,
        createdAt = now.epochSecond,
    )

    private fun matchEntity(
        id: Int = 1,
        bracket: BracketEntity = bracketEntity(),
        status: MatchStatus = MatchStatus.FINISHED,
    ) = MatchEntity(
        id = id,
        bracket = bracket,
        routineId = 1,
        judge = UserEntity(1, "judge", "hash", UserRole.JUDGE, now.epochSecond),
        athleteRed = AthleteEntity(1, "Red", GenderType.MALE, ClubEntity(1, "Club", null, now.epochSecond), now.epochSecond),
        athleteBlue = AthleteEntity(2, "Blue", GenderType.MALE, ClubEntity(1, "Club", null, now.epochSecond), now.epochSecond),
        winnerAthlete = AthleteEntity(1, "Red", GenderType.MALE, ClubEntity(1, "Club", null, now.epochSecond), now.epochSecond),
        status = status,
        startedAt = now.toEpochMilli(),
        finishedAt = now.plusSeconds(60).toEpochMilli(),
        createdAt = now.epochSecond,
    )

    private fun matchProgressEntity(
        match: MatchEntity,
        redFinishedAtEpoch: Long? = null,
        blueFinishedAtEpoch: Long? = null,
    ) = MatchProgressEntity(
        match = match,
        redFinishedAt = redFinishedAtEpoch,
        blueFinishedAt = blueFinishedAtEpoch,
        updatedAt = now.epochSecond,
    ).also { it.id = 1 }

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

            assertEquals(failure(ApiError.TOURNAMENT_NOT_FOUND), result)
        }

        @Test
        fun `should fail when gender is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.createBracket(1, "INVALID", "QUALIFIERS")

            assertEquals(failure(ApiError.INVALID_GENDER), result)
        }

        @Test
        fun `should fail when stage is invalid`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.createBracket(1, "MALE", "INVALID")

            assertEquals(failure(ApiError.INVALID_BRACKET_STAGE), result)
        }

        @Test
        fun `should fail when bracket already exists for stage`() {
            val existing =
                bracketEntity(stage = BracketStage.QUALIFIERS)
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(listOf(existing))

            val result = service.createBracket(1, "MALE", "QUALIFIERS")

            assertEquals(failure(ApiError.BRACKET_ALREADY_EXISTS), result)

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

            assertEquals(failure(ApiError.TOURNAMENT_NOT_FOUND), result)
        }
    }

    @Nested
    inner class GetBracketsByTournamentAndGender {
        @Test
        fun `should return brackets of the tournament for gender`() {
            val list = listOf(bracketEntity())
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(list)

            val result = service.getBracketsByTournamentAndGender(1, "MALE")

            assertEquals(success(list.map { it.toDomain() }), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.empty())

            val result = service.getBracketsByTournamentAndGender(1, "MALE")

            assertEquals(failure(ApiError.TOURNAMENT_NOT_FOUND), result)
        }

        @Test
        fun `should fail when gender is invalid`() {
            lenient().whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.getBracketsByTournamentAndGender(1, "INVALID")

            assertEquals(failure(ApiError.INVALID_GENDER), result)
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

            assertEquals(failure(ApiError.INVALID_GENDER), result)
        }
    }

    @Nested
    inner class GetBracketLeaderboard {
        @Test
        fun `should return empty leaderboard when no matches`() {
            val tournament = tournamentEntity()
            val bracket = bracketEntity(tournament = tournament)
            whenever(brackets.findById(1)).thenReturn(Optional.of(bracket))
            whenever(matches.findByBracketId(1)).thenReturn(emptyList())

            val result = service.getBracketLeaderboard(1)

            val leaderboard: BracketLeaderboard? = if (result is Either.Right<*>) result.value as BracketLeaderboard else null
            assertEquals(emptyList<LeaderboardEntry>(), leaderboard?.entries)
        }

        @Test
        fun `should include finished sides of running match`() {
            val tournament = tournamentEntity()
            val bracket = bracketEntity(tournament = tournament)
            val runningMatch = matchEntity(id = 1, status = MatchStatus.RUNNING)
            val progress =
                matchProgressEntity(
                    match = runningMatch,
                    redFinishedAtEpoch = now.plusSeconds(10).toEpochMilli(),
                )
            whenever(brackets.findById(1)).thenReturn(Optional.of(bracket))
            whenever(matches.findByBracketId(1)).thenReturn(listOf(runningMatch))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(progress)

            val result = service.getBracketLeaderboard(1)

            val leaderboard: BracketLeaderboard? = if (result is Either.Right<*>) result.value as BracketLeaderboard else null
            assertEquals(listOf("Red"), leaderboard?.entries?.map { it.athleteName })
            assertEquals("0:10.000", leaderboard?.entries?.first()?.duration)
            assertTrue(result is Either.Right<*>)
        }

        @Test
        fun `should keep best time per athlete`() {
            val tournament = tournamentEntity()
            val bracket = bracketEntity(tournament = tournament)
            val slowerMatch = matchEntity(id = 1, status = MatchStatus.FINISHED)
            val fasterMatch = matchEntity(id = 2, status = MatchStatus.FINISHED)
            val slowerMatchProgress =
                matchProgressEntity(
                    match = slowerMatch,
                    redFinishedAtEpoch = now.plusSeconds(60).toEpochMilli(),
                )
            val fasterMatchProgress =
                matchProgressEntity(
                    match = fasterMatch,
                    redFinishedAtEpoch = now.plusSeconds(30).toEpochMilli(),
                )
            whenever(brackets.findById(1)).thenReturn(Optional.of(bracket))
            whenever(matches.findByBracketId(1)).thenReturn(listOf(slowerMatch, fasterMatch))
            whenever(matchProgresses.findByMatchId(1)).thenReturn(slowerMatchProgress)
            whenever(matchProgresses.findByMatchId(2)).thenReturn(fasterMatchProgress)

            val result = service.getBracketLeaderboard(1)

            val leaderboard: BracketLeaderboard? = if (result is Either.Right<*>) result.value as BracketLeaderboard else null
            assertEquals(listOf("Red"), leaderboard?.entries?.map { it.athleteName })
            assertEquals("0:30.000", leaderboard?.entries?.first()?.duration)
        }
    }

    @Nested
    inner class GetTournamentBracketsSummary {
        @Test
        fun `should return summary with finished matches`() {
            val tournament = tournamentEntity()
            val bracket = bracketEntity(tournament = tournament)
            val finishedMatch = matchEntity()
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournament))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(listOf(bracket))
            whenever(matches.findByBracketId(bracket.id)).thenReturn(listOf(finishedMatch))

            val result = service.getTournamentBracketsSummary(1, "MALE")

            assertTrue(result is Either.Right<*>)
            val summary = (result as Either.Right<*>).value as TournamentBracketsResponse
            assertEquals(
                1,
                summary.brackets
                    .first()
                    .matches.size,
            )
        }

        @Test
        fun `should return empty when no brackets`() {
            whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))
            whenever(brackets.findByTournamentIdAndGender(1, GenderType.MALE)).thenReturn(emptyList())

            val result = service.getTournamentBracketsSummary(1, "MALE")

            assertTrue(result is Either.Right<*>)
            val summary = (result as Either.Right<*>).value as TournamentBracketsResponse
            assertTrue(summary.brackets.isEmpty())
        }

        @Test
        fun `should fail when gender is invalid`() {
            lenient().whenever(tournaments.findById(1)).thenReturn(Optional.of(tournamentEntity()))

            val result = service.getTournamentBracketsSummary(1, "INVALID")

            assertEquals(failure(ApiError.INVALID_GENDER), result)
        }
    }
}
