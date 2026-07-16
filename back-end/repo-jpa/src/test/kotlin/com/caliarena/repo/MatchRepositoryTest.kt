package com.caliarena.repo

import com.caliarena.RepositoryEnduranceRoutine
import com.caliarena.RepositoryMatch
import com.caliarena.RepositoryTournament
import com.caliarena.RepositoryUser
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.match.MatchEventType
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

                val match = newMatch(repoMatch, bracketId, routineId)

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
                val prev = newMatch(repoMatch, bracketId, routineId)

                val next =
                    repoMatch.createMatch(
                        bracketId = bracketId,
                        routineId = routineId,
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
                newMatch(repoMatch, bracketId, routineId)
                newMatch(repoMatch, bracketId, routineId)

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
        fun `should return matches with the given status`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val m1 = newMatch(repoMatch, bracketId, routineId)
                newMatch(repoMatch, bracketId, routineId)

                repoMatch.updateStatus(m1.id, MatchStatus.RUNNING)

                val running = repoMatch.findByStatus(MatchStatus.RUNNING)
                val pending = repoMatch.findByStatus(MatchStatus.PENDING)

                assertEquals(1, running.size)
                assertEquals(1, pending.size)
            }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `should update the match status`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)

                val updated = repoMatch.updateStatus(match.id, MatchStatus.FINISHED)

                assertNotNull(updated)
                assertEquals(MatchStatus.FINISHED, updated?.status)
            }

        @Test
        fun `should return null when match does not exist`() =
            trx.run {
                assertNull(repoMatch.updateStatus(-1, MatchStatus.RUNNING))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing match by id`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val created = newMatch(repoMatch, bracketId, routineId)

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
                newMatch(repoMatch, bracketId, routineId)
                newMatch(repoMatch, bracketId, routineId)

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
                val created = newMatch(repoMatch, bracketId, routineId)

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
                val match = newMatch(repoMatch, bracketId, routineId)
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                repoMatch.clear()

                assertTrue(repoMatch.findAll().isEmpty())
                assertNull(repoMatch.findProgressByMatchId(match.id))
            }
    }

    @Nested
    inner class CreateMatchProgress {
        @Test
        fun `should create progress for an existing match`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)

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
                val match = newMatch(repoMatch, bracketId, routineId)
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
    inner class UpdateReps {
        @Test
        fun `should update red and blue reps`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val updated =
                    repoMatch.updateReps(
                        matchId = match.id,
                        redReps = 10,
                        blueReps = 8,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(updated)
                assertEquals(10, updated?.redCurrentReps)
                assertEquals(8, updated?.blueCurrentReps)
            }

        @Test
        fun `should return null when progress does not exist`() =
            trx.run {
                assertNull(
                    repoMatch.updateReps(
                        matchId = -1,
                        redReps = 5,
                        blueReps = 5,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }
    }

    @Nested
    inner class UpdateTimer {
        @Test
        fun `should update the timer fields`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)
                repoMatch.createMatchProgress(match.id, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val timerStart = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                val updated =
                    repoMatch.updateTimer(
                        matchId = match.id,
                        timerStartedAt = timerStart,
                        timerRemainingSeconds = 300,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(updated)
                assertEquals(timerStart, updated?.timerStartedAt)
                assertEquals(300, updated?.timerRemainingSeconds)
            }

        @Test
        fun `should return null when progress does not exist`() =
            trx.run {
                assertNull(
                    repoMatch.updateTimer(
                        matchId = -1,
                        timerStartedAt = null,
                        timerRemainingSeconds = null,
                        updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }
    }

    @Nested
    inner class CreateEvent {
        @Test
        fun `should create an event for an existing match and judge`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)
                val judgeId = newJudge(repoUser)

                val event =
                    repoMatch.createEvent(
                        matchId = match.id,
                        judgeId = judgeId,
                        eventType = MatchEventType.MATCH_STARTED,
                        payload = null,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(event)
                assertEquals(match.id, event?.matchId)
                assertEquals(judgeId, event?.judgeId)
                assertEquals(MatchEventType.MATCH_STARTED, event?.eventType)
            }

        @Test
        fun `should store payload when provided`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)
                val judgeId = newJudge(repoUser)

                val event =
                    repoMatch.createEvent(
                        matchId = match.id,
                        judgeId = judgeId,
                        eventType = MatchEventType.REP_ADDED,
                        payload = """{"athlete":"red","reps":1}""",
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertEquals("""{"athlete":"red","reps":1}""", event?.payload)
            }

        @Test
        fun `should return null when match does not exist`() =
            trx.run {
                val judgeId = newJudge(repoUser)

                assertNull(
                    repoMatch.createEvent(
                        matchId = -1,
                        judgeId = judgeId,
                        eventType = MatchEventType.MATCH_STARTED,
                        payload = null,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }

        @Test
        fun `should return null when judge does not exist`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)

                assertNull(
                    repoMatch.createEvent(
                        matchId = match.id,
                        judgeId = -1,
                        eventType = MatchEventType.MATCH_STARTED,
                        payload = null,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    ),
                )
            }
    }

    @Nested
    inner class FindEventsByMatchId {
        @Test
        fun `should return all events for a match`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)
                val judgeId = newJudge(repoUser)

                repoMatch.createEvent(match.id, judgeId, MatchEventType.MATCH_STARTED, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoMatch.createEvent(match.id, judgeId, MatchEventType.REP_ADDED, null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val events = repoMatch.findEventsByMatchId(match.id)

                assertEquals(2, events.size)
            }

        @Test
        fun `should return empty list when match has no events`() =
            trx.run {
                val bracketId = newTournamentWithBracket(repoTournament)
                val routineId = newRoutine(repoEnduranceRoutine)
                val match = newMatch(repoMatch, bracketId, routineId)

                val events = repoMatch.findEventsByMatchId(match.id)

                assertTrue(events.isEmpty())
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
    ) = repoMatch.createMatch(
        bracketId = bracketId,
        routineId = routineId,
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
