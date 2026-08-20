package com.caliarena.repo

import com.caliarena.Transaction
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.routine.ExerciseType
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
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class MatchRepositoryTest {
    @Autowired
    private lateinit var trx: TransactionManagerJpa

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

    private data class MatchContext(
        val bracketId: Int,
        val routineId: Int,
        val judgeId: Int,
        val athleteRedId: Int,
        val athleteBlueId: Int,
        val matchId: Int,
    )

    private fun Transaction.now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newAthlete(): Int {
        val club =
            repoClub.createClub(
                name = "club-${System.nanoTime()}",
                shortName = null,
                createdAt = now(),
            )
        return repoAthlete
            .createAthlete(
                name = "athlete-${System.nanoTime()}",
                gender = GenderType.MALE,
                clubId = club.id,
                createdAt = now(),
            )!!
            .id
    }

    private fun Transaction.newExercise(routineId: Int): Int =
        repoEnduranceRoutine
            .createExercise(
                routineId = routineId,
                name = "ex-${System.nanoTime()}",
                targetReps = 10,
                exerciseOrder = 1,
                type = ExerciseType.NORMAL,
                addedWeight = null,
                supersetOrder = null,
            )!!
            .id

    private fun Transaction.newMatch(): MatchContext {
        val tournament =
            repoTournament.createTournament(
                name = "t-${System.nanoTime()}",
                location = null,
                startDate = null,
                endDate = null,
                createdAt = now(),
            )
        val bracket =
            repoTournament.createBracket(
                tournamentId = tournament.id,
                gender = GenderType.MALE,
                stage = BracketStage.QUALIFIERS,
                createdAt = now(),
            )!!
        val routine =
            repoEnduranceRoutine.createRoutine(
                name = "routine-${System.nanoTime()}",
                timeCapSeconds = 600,
                createdAt = now(),
            )
        val judge =
            repoUser.createUser(
                username = "judge-${System.nanoTime()}",
                passwordValidationInfo = PasswordValidationInfo("hashed"),
                role = UserRole.JUDGE,
                createdAt = now(),
            )
        val athleteRed = newAthlete()
        val athleteBlue = newAthlete()
        val match =
            repoMatch.createMatch(
                bracketId = bracket.id,
                routineId = routine.id,
                judgeId = judge.id,
                athleteRed = athleteRed,
                athleteBlue = athleteBlue,
                createdAt = now(),
            )!!

        return MatchContext(
            bracketId = bracket.id,
            routineId = routine.id,
            judgeId = judge.id,
            athleteRedId = athleteRed,
            athleteBlueId = athleteBlue,
            matchId = match.id,
        )
    }

    @Nested
    inner class CreateMatch {
        @Test
        fun `should create a match for an existing bracket`() =
            trx.run {
                val ctx = newMatch()

                val match =
                    repoMatch.createMatch(
                        bracketId = ctx.bracketId,
                        routineId = ctx.routineId,
                        judgeId = ctx.judgeId,
                        athleteRed = newAthlete(),
                        athleteBlue = newAthlete(),
                        createdAt = now(),
                    )!!

                assertAll(
                    { assertNotEquals(0, match.id) },
                    { assertEquals(ctx.bracketId, match.bracketId) },
                    { assertEquals(ctx.routineId, match.routineId) },
                    { assertEquals(MatchStatus.PENDING, match.status) },
                )
            }

        @Test
        fun `should return null when bracket does not exist`() =
            trx.run {
                val ctx = newMatch()

                val match =
                    repoMatch.createMatch(
                        bracketId = -1,
                        routineId = ctx.routineId,
                        judgeId = ctx.judgeId,
                        athleteRed = ctx.athleteRedId,
                        athleteBlue = ctx.athleteBlueId,
                        createdAt = now(),
                    )

                assertNull(match)
            }
    }

    @Nested
    inner class FindByBracketId {
        @Test
        fun `should return all matches for a bracket`() =
            trx.run {
                val ctx = newMatch()

                repeat(2) {
                    repoMatch.createMatch(
                        bracketId = ctx.bracketId,
                        routineId = ctx.routineId,
                        judgeId = ctx.judgeId,
                        athleteRed = newAthlete(),
                        athleteBlue = newAthlete(),
                        createdAt = now(),
                    )
                }

                val matches = repoMatch.findByBracketId(ctx.bracketId)
                assertEquals(3, matches.size)
            }

        @Test
        fun `should return empty list when bracket has no matches`() =
            trx.run {
                val tournament = repoTournament.createTournament("t-${System.nanoTime()}", null, null, null, now())
                val bracket = repoTournament.createBracket(tournament.id, GenderType.MALE, BracketStage.QUALIFIERS, now())!!

                assertTrue(repoMatch.findByBracketId(bracket.id).isEmpty())
            }
    }

    @Nested
    inner class FindByStatus {
        @Test
        fun `should return matches with given status`() =
            trx.run {
                val ctx = newMatch()

                repeat(2) {
                    repoMatch.createMatch(
                        bracketId = ctx.bracketId,
                        routineId = ctx.routineId,
                        judgeId = ctx.judgeId,
                        athleteRed = newAthlete(),
                        athleteBlue = newAthlete(),
                        createdAt = now(),
                    )
                }

                val pending = repoMatch.findByStatus(MatchStatus.PENDING)
                assertEquals(3, pending.size)
                assertTrue(pending.all { it.status == MatchStatus.PENDING })
            }

        @Test
        fun `should return empty list when no matches with status`() =
            trx.run {
                assertTrue(repoMatch.findByStatus(MatchStatus.FINISHED).isEmpty())
            }
    }

    @Nested
    inner class CreateMatchProgress {
        @Test
        fun `should create progress for an existing match`() =
            trx.run {
                val ctx = newMatch()
                val exerciseId = newExercise(ctx.routineId)

                val progress =
                    repoMatch.createMatchProgress(
                        matchId = ctx.matchId,
                        firstExerciseId = exerciseId,
                        now = now(),
                    )

                assertNotNull(progress)
                assertEquals(ctx.matchId, progress?.matchId)
                assertEquals(0, progress?.redCurrentReps)
                assertEquals(0, progress?.blueCurrentReps)
            }

        @Test
        fun `should return null when match does not exist`() =
            trx.run {
                assertNull(repoMatch.createMatchProgress(matchId = -1, firstExerciseId = 1, now = now()))
            }
    }

    @Nested
    inner class FindProgressByMatchId {
        @Test
        fun `should find progress for an existing match`() =
            trx.run {
                val ctx = newMatch()
                val exerciseId = newExercise(ctx.routineId)
                repoMatch.createMatchProgress(ctx.matchId, exerciseId, now())

                val progress = repoMatch.findProgressByMatchId(ctx.matchId)

                assertNotNull(progress)
                assertEquals(ctx.matchId, progress?.matchId)
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
        fun `should update reps on an existing progress`() =
            trx.run {
                val ctx = newMatch()
                val exerciseId = newExercise(ctx.routineId)
                val initial = repoMatch.createMatchProgress(ctx.matchId, exerciseId, now())!!

                val updated =
                    repoMatch.updateMatchProgress(
                        initial.copy(redCurrentReps = 5, blueCurrentReps = 3),
                        null,
                        null,
                    )

                assertNotNull(updated)
                assertEquals(5, updated?.redCurrentReps)
                assertEquals(3, updated?.blueCurrentReps)
            }

        @Test
        fun `should return null when match does not exist`() =
            trx.run {
                val dummy =
                    MatchProgress(
                        id = -1,
                        matchId = -1,
                        redCurrentReps = 0,
                        blueCurrentReps = 0,
                        updatedAt = now(),
                    )

                assertNull(repoMatch.updateMatchProgress(dummy, null, null))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing match by id`() =
            trx.run {
                val ctx = newMatch()

                val found = repoMatch.findById(ctx.matchId)

                assertNotNull(found)
                assertEquals(ctx.matchId, found?.id)
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
                val ctx = newMatch()
                repoMatch.createMatch(
                    bracketId = ctx.bracketId,
                    routineId = ctx.routineId,
                    judgeId = ctx.judgeId,
                    athleteRed = newAthlete(),
                    athleteBlue = newAthlete(),
                    createdAt = now(),
                )

                assertEquals(2, repoMatch.findAll().size)
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
                val ctx = newMatch()

                repoMatch.deleteById(ctx.matchId)

                assertNull(repoMatch.findById(ctx.matchId))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all matches and progress`() =
            trx.run {
                val ctx = newMatch()
                val exerciseId = newExercise(ctx.routineId)
                repoMatch.createMatchProgress(ctx.matchId, exerciseId, now())

                repoMatch.clear()

                assertTrue(repoMatch.findAll().isEmpty())
                assertNull(repoMatch.findProgressByMatchId(ctx.matchId))
            }
    }
}
