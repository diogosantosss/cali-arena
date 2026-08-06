package com.caliarena.repo

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

    private data class FullMatchContext(
        val bracketId: Int,
        val routineId: Int,
        val judgeId: Int,
        val athleteRed: Int,
        val athleteBlue: Int,
        val matchId: Int,
    )

    private class TestDataFactory(
        private val trx: TransactionManagerJpa,
    ) {
        fun createFullMatch(): FullMatchContext =
            trx.run {
                // 1. Tournament + bracket
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

                // 2. Routine
                val routine =
                    repoEnduranceRoutine.createRoutine(
                        name = "routine-${System.nanoTime()}",
                        timeCapSeconds = 600,
                        createdAt = now(),
                    )

                // 3. Judge
                val judge =
                    repoUser.createUser(
                        username = "judge-${System.nanoTime()}",
                        passwordValidationInfo = PasswordValidationInfo("hashed"),
                        role = UserRole.JUDGE,
                        createdAt = now(),
                    )

                // 4. Two athletes (each with a fresh club)
                val athleteRed = createAthlete()
                val athleteBlue = createAthlete()

                // 5. Match
                val match =
                    repoMatch.createMatch(
                        bracketId = bracket.id,
                        routineId = routine.id,
                        judgeId = judge.id,
                        athleteRed = athleteRed,
                        athleteBlue = athleteBlue,
                        createdAt = now(),
                    )!!

                FullMatchContext(
                    bracketId = bracket.id,
                    routineId = routine.id,
                    judgeId = judge.id,
                    athleteRed = athleteRed,
                    athleteBlue = athleteBlue,
                    matchId = match.id,
                )
            }

        // Helper to create a single athlete (with club)
        fun createAthlete(): Int =
            trx.run {
                val club =
                    repoClub.createClub(
                        name = "club-${System.nanoTime()}",
                        shortName = null,
                        createdAt = now(),
                    )
                repoAthlete
                    .createAthlete(
                        name = "athlete-${System.nanoTime()}",
                        gender = GenderType.MALE,
                        clubId = club.id,
                        createdAt = now(),
                    )!!
                    .id
            }

        // Helper to create an exercise inside a routine
        fun createExercise(routineId: Int): Int =
            trx.run {
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
            }

        private fun now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    }

    private lateinit var factory: TestDataFactory

    @BeforeEach
    fun initFactory() {
        factory = TestDataFactory(trx)
    }

    @Nested
    inner class CreateMatch {
        @Test
        fun `should create a match for an existing bracket`() {
            val context = factory.createFullMatch()

            val match =
                trx.run {
                    repoMatch.createMatch(
                        bracketId = context.bracketId,
                        routineId = context.routineId,
                        judgeId = context.judgeId,
                        athleteRed = context.athleteRed,
                        athleteBlue = context.athleteBlue,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }!!

            assertAll(
                { assertNotEquals(0, match.id) },
                { assertEquals(context.bracketId, match.bracketId) },
                { assertEquals(context.routineId, match.routineId) },
                { assertEquals(context.athleteRed, match.athleteRedId) },
                { assertEquals(context.athleteBlue, match.athleteBlueId) },
                { assertEquals(MatchStatus.PENDING, match.status) },
            )
        }

        @Test
        fun `should return null when bracket does not exist`() {
            val context = factory.createFullMatch()
            val match =
                trx.run {
                    repoMatch.createMatch(
                        bracketId = -1,
                        routineId = context.routineId,
                        judgeId = context.judgeId,
                        athleteRed = context.athleteRed,
                        athleteBlue = context.athleteBlue,
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
            val context = factory.createFullMatch()

            // Add two extra matches under the same bracket
            repeat(2) {
                trx.run {
                    repoMatch.createMatch(
                        bracketId = context.bracketId,
                        routineId = context.routineId,
                        judgeId = context.judgeId,
                        athleteRed = factory.createAthlete(),
                        athleteBlue = factory.createAthlete(),
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }
            }

            val matches = trx.run { repoMatch.findByBracketId(context.bracketId) }
            assertEquals(3, matches.size) // 1 from factory + 2 added
        }

        @Test
        fun `should return empty list when bracket has no matches`() {
            // Create a bracket without any match
            val bracketId =
                trx.run {
                    val tournament =
                        repoTournament.createTournament(
                            name = "t-${System.nanoTime()}",
                            location = null,
                            startDate = null,
                            endDate = null,
                            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                        )
                    repoTournament
                        .createBracket(
                            tournamentId = tournament.id,
                            gender = GenderType.MALE,
                            stage = BracketStage.QUALIFIERS,
                            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                        )!!
                        .id
                }

            val matches = trx.run { repoMatch.findByBracketId(bracketId) }
            assertTrue(matches.isEmpty())
        }
    }

    @Nested
    inner class FindByStatus {
        @Test
        fun `should return matches with given status`() {
            val context = factory.createFullMatch()

            // Add two more PENDING matches
            repeat(2) {
                trx.run {
                    repoMatch.createMatch(
                        bracketId = context.bracketId,
                        routineId = context.routineId,
                        judgeId = context.judgeId,
                        athleteRed = factory.createAthlete(),
                        athleteBlue = factory.createAthlete(),
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }
            }

            val pending = trx.run { repoMatch.findByStatus(MatchStatus.PENDING) }
            assertEquals(3, pending.size)
            assertTrue(pending.all { it.status == MatchStatus.PENDING })
        }

        @Test
        fun `should return empty list when no matches with status`() {
            val result = trx.run { repoMatch.findByStatus(MatchStatus.FINISHED) }
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class CreateMatchProgress {
        @Test
        fun `should create progress for an existing match`() {
            val context = factory.createFullMatch()
            val exerciseId = factory.createExercise(context.routineId)

            val progress =
                trx.run {
                    repoMatch.createMatchProgress(
                        matchId = context.matchId,
                        firstExerciseId = exerciseId,
                        now = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }

            assertNotNull(progress)
            assertEquals(context.matchId, progress?.matchId)
            assertEquals(0, progress?.redCurrentReps)
            assertEquals(0, progress?.blueCurrentReps)
        }

        @Test
        fun `should return null when match does not exist`() {
            val progress =
                trx.run {
                    repoMatch.createMatchProgress(
                        matchId = -1,
                        firstExerciseId = 1,
                        now = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }
            assertNull(progress)
        }
    }

    @Nested
    inner class FindProgressByMatchId {
        @Test
        fun `should find progress for an existing match`() {
            val context = factory.createFullMatch()
            val exerciseId = factory.createExercise(context.routineId)

            trx.run {
                repoMatch.createMatchProgress(
                    context.matchId,
                    exerciseId,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )
            }

            val progress = trx.run { repoMatch.findProgressByMatchId(context.matchId) }
            assertNotNull(progress)
            assertEquals(context.matchId, progress?.matchId)
        }

        @Test
        fun `should return null when match has no progress`() {
            val progress = trx.run { repoMatch.findProgressByMatchId(-1) }
            assertNull(progress)
        }
    }

    @Nested
    inner class UpdateMatchProgress {
        @Test
        fun `should update the match progress correctly`() {
            val context = factory.createFullMatch()
            val exerciseId = factory.createExercise(context.routineId)

            val initialProgress =
                trx.run {
                    repoMatch.createMatchProgress(
                        matchId = context.matchId,
                        firstExerciseId = exerciseId,
                        now = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )
                }!!

            assertEquals(0, initialProgress.redCurrentReps)
            assertEquals(0, initialProgress.blueCurrentReps)

            val updated =
                trx.run {
                    repoMatch.updateMatchProgress(
                        initialProgress.copy(
                            redCurrentReps = 1,
                            blueCurrentReps = 1,
                        ),
                        null,
                        null,
                    )
                }

            assertNotNull(updated)
            assertEquals(1, updated?.redCurrentReps)
            assertEquals(1, updated?.blueCurrentReps)
        }

        @Test
        fun `should return null when there is no match or progress`() {
            val dummyProgress =
                MatchProgress(
                    id = -1,
                    matchId = -1,
                    redCurrentReps = 0,
                    blueCurrentReps = 0,
                    updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )

            val result =
                trx.run {
                    repoMatch.updateMatchProgress(dummyProgress, null, null)
                }

            assertNull(result)
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing match by id`() {
            val context = factory.createFullMatch()
            val found = trx.run { repoMatch.findById(context.matchId) }
            assertNotNull(found)
            assertEquals(context.matchId, found?.id)
        }

        @Test
        fun `should return null when id does not exist`() {
            val found = trx.run { repoMatch.findById(-1) }
            assertNull(found)
        }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created matches`() {
            val context = factory.createFullMatch()
            // Create one more match
            trx.run {
                repoMatch.createMatch(
                    bracketId = context.bracketId,
                    routineId = context.routineId,
                    judgeId = context.judgeId,
                    athleteRed = factory.createAthlete(),
                    athleteBlue = factory.createAthlete(),
                    createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )
            }

            val all = trx.run { repoMatch.findAll() }
            assertEquals(2, all.size)
        }

        @Test
        fun `should return empty list when there are no matches`() {
            // cleanup already cleared everything, so findAll should be empty
            val all = trx.run { repoMatch.findAll() }
            assertTrue(all.isEmpty())
        }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the match`() {
            val context = factory.createFullMatch()
            trx.run { repoMatch.deleteById(context.matchId) }

            val found = trx.run { repoMatch.findById(context.matchId) }
            assertNull(found)
        }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all matches, progress and events`() {
            val context = factory.createFullMatch()
            val exerciseId = factory.createExercise(context.routineId)

            trx.run {
                repoMatch.createMatchProgress(
                    context.matchId,
                    exerciseId,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS),
                )
                repoMatch.clear()
            }

            val matches = trx.run { repoMatch.findAll() }
            val progress = trx.run { repoMatch.findProgressByMatchId(context.matchId) }

            assertTrue(matches.isEmpty())
            assertNull(progress)
        }
    }
}
