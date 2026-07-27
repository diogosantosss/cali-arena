package com.caliarena.repo

import com.caliarena.RepositoryEnduranceRoutine
import com.caliarena.RepositoryMatch
import com.caliarena.RepositoryTournament
import com.caliarena.RepositoryUser
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.UserRole
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
class MatchRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            repoMatch.clear()
            repoTournament.clear()
            repoEnduranceRoutine.clear()
            repoUser.clear()
        }
    }

    @Nested
    inner class CreateMatch {
        @Test
        fun `should create a match for an existing bracket`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)

                val match = newMatch(repoMatch, bracketId, routineId, judgeId)

                assertNotEquals(0, match.id)
                assertEquals(bracketId, match.bracketId)
                assertEquals(routineId, match.routineId)
                assertEquals(MatchStatus.PENDING, match.status)
            }

        @Test
        fun `should return null when bracket does not exist`() =
            trx.run {
                val routineId = newRoutine(repoEnduranceRoutine)

                val match =
                    repoMatch.createMatch(
                        bracketId = -1,
                        routineId = routineId,
                        judgeId = -1,
                        redFromMatchId = null,
                        blueFromMatchId = null,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNull(match)
            }

        @Test
        fun `should store redFromMatchId and blueFromMatchId`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val prev = newMatch(repoMatch, bracketId, routineId, judgeId)

                val next =
                    repoMatch.createMatch(
                        bracketId = bracketId,
                        routineId = routineId,
                        judgeId = judgeId,
                        redFromMatchId = prev.id,
                        blueFromMatchId = prev.id,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )!!

                assertEquals(prev.id, next.redFromMatchId)
                assertEquals(prev.id, next.blueFromMatchId)
            }
    }

    @Nested
    inner class FindByBracketId {
        @Test
        fun `should return all matches for a bracket`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                newMatch(repoMatch, bracketId, routineId, judgeId)
                newMatch(repoMatch, bracketId, routineId, judgeId)

                val matches = repoMatch.findByBracketId(bracketId)

                assertEquals(2, matches.size)
            }

        @Test
        fun `should return empty list when bracket has no matches`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)

                val matches = repoMatch.findByBracketId(bracketId)

                assertTrue(matches.isEmpty())
            }
    }

    @Nested
    inner class FindByStatus {
        @Test
        fun `should return matches with given status`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)

                newMatch(repoMatch, bracketId, routineId, judgeId)
                newMatch(repoMatch, bracketId, routineId, judgeId)

                val pendingMatches = repoMatch.findByStatus(MatchStatus.PENDING)

                assertEquals(2, pendingMatches.size)
                assertEquals(MatchStatus.PENDING, pendingMatches.first().status)
            }

        @Test
        fun `should return empty list when no matches with status`() =
            trx.run {
                val result = repoMatch.findByStatus(MatchStatus.FINISHED)
                assertTrue(result.isEmpty())
            }
    }

    @Nested
    inner class CreateMatchProgress {
        @Test
        fun `should create progress for an existing match`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val match = newMatch(repoMatch, bracketId, routineId, judgeId)

                val progress =
                    repoMatch.createMatchProgress(
                        matchId = match.id,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(progress)
                assertEquals(match.id, progress?.matchId)
                assertEquals(0, progress?.redCurrentReps)
                assertEquals(0, progress?.blueCurrentReps)
            }

        @Test
        fun `should return null when match does not exist`() =
            trx.run {
                assertNull(
                    repoMatch.createMatchProgress(
                        matchId = -1,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }
    }

    @Nested
    inner class FindProgressByMatchId {
        @Test
        fun `should find progress for an existing match`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val match = newMatch(repoMatch, bracketId, routineId, judgeId)
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val progress = repoMatch.findProgressByMatchId(match.id)

                assertNotNull(progress)
                assertEquals(match.id, progress?.matchId)
            }

        @Test
        fun `should return null when match has no progress`() =
            trx.run {
                assertNull(repoMatch.findProgressByMatchId(-1))
            }
    }

    @Nested
    inner class UpdateMatchProgress {
        @Test
        fun `should update the match progress correctly`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val match = newMatch(repoMatch, bracketId, routineId, judgeId)

                val progress =
                    repoMatch.createMatchProgress(
                        matchId = match.id,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(progress)
                assertEquals(progress?.redCurrentReps, 0)
                assertEquals(progress?.blueCurrentReps, 0)

                val toUpdate =
                    repoMatch.updateMatchProgress(
                        progress!!.copy(
                            redCurrentReps = 1,
                            blueCurrentReps = 1,
                        ),
                        null,
                        null,
                    )

                assertNotNull(toUpdate)
                assertEquals(toUpdate?.redCurrentReps, 1)
                assertEquals(toUpdate?.blueCurrentReps, 1)
            }

        @Test
        fun `should return null when theres no match or progress`() =
            trx.run {
                val progress = MatchProgress(-1, -1, null, null, -1, -1, null, null, null, null, Instant.now())
                val result = repoMatch.updateMatchProgress(progress, null, null)
                assertNull(result)
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing match by id`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val created = newMatch(repoMatch, bracketId, routineId, judgeId)

                val found = repoMatch.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoMatch.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created matches`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                newMatch(repoMatch, bracketId, routineId, judgeId)
                newMatch(repoMatch, bracketId, routineId, judgeId)

                val matches = repoMatch.findAll()

                assertEquals(2, matches.size)
            }

        @Test
        fun `should return empty list when there are no matches`() =
            trx.run {
                assertTrue(repoMatch.findAll().isEmpty())
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the match`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val created = newMatch(repoMatch, bracketId, routineId, judgeId)

                repoMatch.deleteById(created.id)

                assertNull(repoMatch.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all matches, progress and events`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val judgeId = newJudge(repoUser)
                val match = newMatch(repoMatch, bracketId, routineId, judgeId)
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                repoMatch.clear()

                assertTrue(repoMatch.findAll().isEmpty())
                assertNull(repoMatch.findProgressByMatchId(match.id))
            }
    }

    private fun newTournamentWithBracket(
        repoTournament: RepositoryTournament,
        tournamentName: String = "t-${System.nanoTime()}",
    ): Int {
        val tournament =
            repoTournament.createTournament(
                name = tournamentName,
                location = null,
                startDate = null,
                endDate = null,
                createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            )
        val bracket =
            repoTournament.createBracket(
                tournamentId = tournament.id,
                gender = GenderType.MALE,
                stage = BracketStage.QUALIFIERS,
                createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            )
        return bracket!!.id
    }

    private fun newRoutine(repoEnduranceRoutine: RepositoryEnduranceRoutine): Int =
        repoEnduranceRoutine
            .createRoutine(
                name = "routine-${System.nanoTime()}",
                timeCapSeconds = 600,
                createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            ).id

    private fun newMatch(
        repoMatch: RepositoryMatch,
        bracketId: Int,
        routineId: Int,
        judgeId: Int,
    ) = repoMatch.createMatch(
        bracketId = bracketId,
        routineId = routineId,
        judgeId,
        redFromMatchId = null,
        blueFromMatchId = null,
        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
    )!!

    private fun newJudge(repoUser: RepositoryUser): Int =
        repoUser
            .createUser(
                username = "judge-${System.nanoTime()}",
                passwordValidationInfo = PasswordValidationInfo("hashed"),
                role = UserRole.JUDGE,
                createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            ).id
}
