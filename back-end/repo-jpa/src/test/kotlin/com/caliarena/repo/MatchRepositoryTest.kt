package com.caliarena.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchStatus
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.match.MatchProgressEntity
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManagerJpa
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
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class MatchRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            matchProgresses.deleteAll()
            matches.deleteAll()
            screenRoutines.deleteAll()
            exercises.deleteAll()
            routines.deleteAll()
            tournamentStates.deleteAll()
            brackets.deleteAll()
            tournaments.deleteAll()
            tokens.deleteAll()
            users.deleteAll()
            athletes.deleteAll()
            clubs.deleteAll()
        }
    }

    private fun now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newMatch(status: MatchStatus = MatchStatus.RUNNING): MatchEntity {
        val club = clubs.save(ClubEntity(name = "club-${System.nanoTime()}", createdAt = now().epochSecond))
        val red =
            athletes.save(
                AthleteEntity(name = "red-${System.nanoTime()}", gender = GenderType.MALE, club = club, createdAt = now().epochSecond),
            )
        val blue =
            athletes.save(
                AthleteEntity(name = "blue-${System.nanoTime()}", gender = GenderType.MALE, club = club, createdAt = now().epochSecond),
            )
        val judge =
            users.save(
                UserEntity(
                    username = "judge-${System.nanoTime()}",
                    password = "hash",
                    role = UserRole.JUDGE,
                    createdAt = now().epochSecond,
                ),
            )
        val tournament = tournaments.save(TournamentEntity(name = "t-${System.nanoTime()}", createdAt = now().epochSecond))
        val bracket =
            brackets.save(
                BracketEntity(
                    tournament = tournament,
                    division = "ELITE MALE",
                    stage = BracketStage.QUALIFIERS,
                    createdAt = now().epochSecond,
                ),
            )
        val routine =
            routines.save(
                EnduranceRoutineEntity(name = "mr-${System.nanoTime()}", timeCapSeconds = 600, createdAt = now().epochSecond),
            )
        return matches.save(
            MatchEntity(
                bracket = bracket,
                routineId = routine.id,
                judge = judge,
                athleteRed = red,
                athleteBlue = blue,
                status = status,
                createdAt = now().epochSecond,
            ),
        )
    }

    private fun Transaction.newExercise(): ExerciseEntity {
        val routine =
            routines.save(
                EnduranceRoutineEntity(name = "r-${System.nanoTime()}", timeCapSeconds = 600, createdAt = now().epochSecond),
            )
        return exercises.save(
            ExerciseEntity(
                routine = routine,
                name = "ex-${System.nanoTime()}",
                targetReps = 10,
                exerciseOrder = 1,
                type = ExerciseType.NORMAL,
            ),
        )
    }

    private fun Transaction.newProgress(match: MatchEntity): MatchProgressEntity {
        val exercise = newExercise()
        return matchProgresses.save(
            MatchProgressEntity(
                match = match,
                redCurrentExercise = exercise,
                blueCurrentExercise = exercise,
                timerStartedAt = now().epochSecond,
                updatedAt = now().epochSecond,
            ),
        )
    }

    @Nested
    inner class Matches {
        @Test
        fun `should create and find a match by id`() =
            trx.run {
                val created = newMatch()

                val found = matches.findByIdOrNull(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
                assertEquals(MatchStatus.RUNNING, found?.status)
                assertEquals(created.athleteRed?.id, found?.athleteRed?.id)
            }

        @Test
        fun `should find matches by bracket`() =
            trx.run {
                val matchA = newMatch()
                newMatch()

                val found = matches.findByBracketId(matchA.bracket.id)

                assertEquals(1, found.size)
                assertEquals(matchA.id, found.first().id)
            }

        @Test
        fun `should find matches by status`() =
            trx.run {
                val running = newMatch(MatchStatus.RUNNING)
                newMatch(MatchStatus.PENDING)

                val found = matches.findByStatus(MatchStatus.RUNNING)

                assertTrue(found.any { it.id == running.id })
                assertTrue(found.all { it.status == MatchStatus.RUNNING })
            }

        @Test
        fun `should update a match`() =
            trx.run {
                val created = newMatch()

                created.status = MatchStatus.FINISHED
                matches.save(created)

                assertEquals(MatchStatus.FINISHED, matches.findByIdOrNull(created.id)?.status)
            }

        @Test
        fun `should delete a match`() =
            trx.run {
                val created = newMatch()

                matches.deleteById(created.id)

                assertNull(matches.findByIdOrNull(created.id))
            }
    }

    @Nested
    inner class MatchProgress {
        @Test
        fun `should create progress for a match`() =
            trx.run {
                val match = newMatch()

                val progress = newProgress(match)

                assertNotEqualsZero(progress.id)
                assertEquals(match.id, progress.match.id)
            }

        @Test
        fun `should find progress by match id`() =
            trx.run {
                val match = newMatch()
                val progress = newProgress(match)

                val found = matchProgresses.findByMatchId(match.id)

                assertNotNull(found)
                assertEquals(progress.id, found?.id)
            }

        @Test
        fun `should return null when there is no progress for the match`() =
            trx.run {
                val match = newMatch()

                assertNull(matchProgresses.findByMatchId(match.id))
            }

        @Test
        fun `should update reps in place`() =
            trx.run {
                val match = newMatch()
                val progress = newProgress(match)

                progress.redCurrentReps = 7
                matchProgresses.save(progress)

                assertEquals(7, matchProgresses.findByMatchId(match.id)?.redCurrentReps)
            }

        @Test
        fun `should delete all progress rows`() =
            trx.run {
                newProgress(newMatch())
                newProgress(newMatch())

                matchProgresses.deleteAll()

                assertEquals(0, matchProgresses.count())
            }
    }

    private fun assertNotEqualsZero(id: Int) {
        assertNotEquals(0, id)
    }
}
