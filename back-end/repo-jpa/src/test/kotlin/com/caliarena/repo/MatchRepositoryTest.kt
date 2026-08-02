package com.caliarena.repo

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
            repoAthlete.clear()
            repoClub.clear()
            repoTournament.clear()
            repoEnduranceRoutine.clear()
            repoUser.clear()
        }
    }

    @Nested
    inner class CreateMatch {
        @Test
        fun `should create a match for an existing bracket`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val athleteRed = newAthlete()
            val athleteBlue = newAthlete()

            val match =
                trx.run {
                    repoMatch.createMatch(
                        bracketId = bracketId,
                        routineId = routineId,
                        judgeId = judgeId,
                        athleteRed = athleteRed,
                        athleteBlue = athleteBlue,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }!!

            assertNotEquals(0, match.id)
            assertEquals(bracketId, match.bracketId)
            assertEquals(routineId, match.routineId)
            assertEquals(athleteRed, match.athleteRedId)
            assertEquals(athleteBlue, match.athleteBlueId)
            assertEquals(MatchStatus.PENDING, match.status)
        }

        @Test
        fun `should return null when bracket does not exist`() {
            val routineId = newRoutine()
            val judgeId = newJudge()
            val athleteRed = newAthlete()
            val athleteBlue = newAthlete()

            val match =
                trx.run {
                    repoMatch.createMatch(
                        bracketId = -1,
                        routineId = routineId,
                        judgeId = judgeId,
                        athleteRed = athleteRed,
                        athleteBlue = athleteBlue,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }

            assertNull(match)
        }
    }

    @Nested
    inner class FindByBracketId {
        @Test
        fun `should return all matches for a bracket`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            newMatch(bracketId, routineId, judgeId)
            newMatch(bracketId, routineId, judgeId)

            val matches =
                trx.run {
                    repoMatch.findByBracketId(bracketId)
                }

            assertEquals(2, matches.size)
        }

        @Test
        fun `should return empty list when bracket has no matches`() {
            val bracketId = newTournamentWithBracket()

            val matches =
                trx.run {
                    repoMatch.findByBracketId(bracketId)
                }

            assertTrue(matches.isEmpty())
        }
    }

    @Nested
    inner class FindByStatus {
        @Test
        fun `should return matches with given status`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            newMatch(bracketId, routineId, judgeId)
            newMatch(bracketId, routineId, judgeId)

            val pendingMatches =
                trx.run {
                    repoMatch.findByStatus(MatchStatus.PENDING)
                }

            assertEquals(2, pendingMatches.size)
            assertEquals(MatchStatus.PENDING, pendingMatches.first().status)
        }

        @Test
        fun `should return empty list when no matches with status`() {
            val result =
                trx.run {
                    repoMatch.findByStatus(MatchStatus.FINISHED)
                }

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class CreateMatchProgress {
        @Test
        fun `should create progress for an existing match`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val match = newMatch(bracketId, routineId, judgeId)

            val progress =
                trx.run {
                    repoMatch.createMatchProgress(
                        matchId = match.id,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }

            assertNotNull(progress)
            assertEquals(match.id, progress?.matchId)
            assertEquals(0, progress?.redCurrentReps)
            assertEquals(0, progress?.blueCurrentReps)
        }

        @Test
        fun `should return null when match does not exist`() {
            val progress =
                trx.run {
                    repoMatch.createMatchProgress(
                        matchId = -1,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }

            assertNull(progress)
        }
    }

    @Nested
    inner class FindProgressByMatchId {
        @Test
        fun `should find progress for an existing match`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val match = newMatch(bracketId, routineId, judgeId)

            trx.run {
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))
            }

            val progress =
                trx.run {
                    repoMatch.findProgressByMatchId(match.id)
                }

            assertNotNull(progress)
            assertEquals(match.id, progress?.matchId)
        }

        @Test
        fun `should return null when match has no progress`() {
            val progress =
                trx.run {
                    repoMatch.findProgressByMatchId(-1)
                }

            assertNull(progress)
        }
    }

    @Nested
    inner class UpdateMatchProgress {
        @Test
        fun `should update the match progress correctly`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val match = newMatch(bracketId, routineId, judgeId)

            val progress =
                trx.run {
                    repoMatch.createMatchProgress(
                        matchId = match.id,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }

            assertNotNull(progress)
            assertEquals(0, progress?.redCurrentReps)
            assertEquals(0, progress?.blueCurrentReps)

            val toUpdate =
                trx.run {
                    repoMatch.updateMatchProgress(
                        progress!!.copy(
                            redCurrentReps = 1,
                            blueCurrentReps = 1,
                        ),
                        null,
                        null,
                    )
                }

            assertNotNull(toUpdate)
            assertEquals(1, toUpdate?.redCurrentReps)
            assertEquals(1, toUpdate?.blueCurrentReps)
        }

        @Test
        fun `should return null when theres no match or progress`() {
            val progress = MatchProgress(-1, -1, null, null, -1, -1, null, null, null, null, Instant.now())

            val result =
                trx.run {
                    repoMatch.updateMatchProgress(progress, null, null)
                }

            assertNull(result)
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing match by id`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val created = newMatch(bracketId, routineId, judgeId)

            val found =
                trx.run {
                    repoMatch.findById(created.id)
                }

            assertNotNull(found)
            assertEquals(created.id, found?.id)
        }

        @Test
        fun `should return null when id does not exist`() {
            val found =
                trx.run {
                    repoMatch.findById(-1)
                }

            assertNull(found)
        }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created matches`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            newMatch(bracketId, routineId, judgeId)
            newMatch(bracketId, routineId, judgeId)

            val matches =
                trx.run {
                    repoMatch.findAll()
                }

            assertEquals(2, matches.size)
        }

        @Test
        fun `should return empty list when there are no matches`() {
            val matches =
                trx.run {
                    repoMatch.findAll()
                }

            assertTrue(matches.isEmpty())
        }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the match`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val created = newMatch(bracketId, routineId, judgeId)

            trx.run {
                repoMatch.deleteById(created.id)
            }

            val found =
                trx.run {
                    repoMatch.findById(created.id)
                }

            assertNull(found)
        }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all matches, progress and events`() {
            val bracketId = newTournamentWithBracket()
            val routineId = newRoutine()
            val judgeId = newJudge()
            val match = newMatch(bracketId, routineId, judgeId)

            trx.run {
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoMatch.clear()
            }

            val matches =
                trx.run {
                    repoMatch.findAll()
                }

            val progress =
                trx.run {
                    repoMatch.findProgressByMatchId(match.id)
                }

            assertTrue(matches.isEmpty())
            assertNull(progress)
        }
    }

    private fun newTournamentWithBracket(tournamentName: String = "t-${System.nanoTime()}"): Int =
        trx.run {
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

            bracket!!.id
        }

    private fun newRoutine(): Int =
        trx.run {
            repoEnduranceRoutine
                .createRoutine(
                    name = "routine-${System.nanoTime()}",
                    timeCapSeconds = 600,
                    createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                ).id
        }

    private fun newMatch(
        bracketId: Int,
        routineId: Int,
        judgeId: Int,
    ) = trx.run {
        repoMatch.createMatch(
            bracketId = bracketId,
            routineId = routineId,
            judgeId = judgeId,
            athleteRed = newAthlete(),
            athleteBlue = newAthlete(),
            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
        )
    }!!

    private fun newJudge(): Int =
        trx.run {
            repoUser
                .createUser(
                    username = "judge-${System.nanoTime()}",
                    passwordValidationInfo = PasswordValidationInfo("hashed"),
                    role = UserRole.JUDGE,
                    createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                ).id
        }

    private fun newClub(): Int =
        trx.run {
            repoClub
                .createClub(
                    name = "club-${System.nanoTime()}",
                    shortName = null,
                    createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                ).id
        }

    private fun newAthlete(): Int {
        val clubId = newClub()

        return trx.run {
            repoAthlete
                .createAthlete(
                    name = "athlete-${System.nanoTime()}",
                    gender = GenderType.MALE,
                    clubId = clubId,
                    createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )!!
                .id
        }
    }
}
