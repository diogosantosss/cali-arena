package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchEvent
import com.caliarena.domain.match.MatchEventType
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.MatchStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

class MatchServiceTest : ServiceTest() {
    private lateinit var service: MatchService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoMatch).thenReturn(repoMatch)
        lenient().whenever(transaction.repoTournament).thenReturn(repoTournament)
        lenient().whenever(transaction.repoEnduranceRoutine).thenReturn(repoEnduranceRoutine)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = MatchService(trxManager, clock)
    }

    @Nested
    inner class CreateMatch {
        private val now = clock.instant()

        private val match =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = null,
                athleteBlueId = null,
                status = MatchStatus.PENDING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        @Test
        fun `should create match successfully`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(mock())
            whenever(repoEnduranceRoutine.findById(5)).thenReturn(mock())
            whenever(
                repoMatch.createMatch(
                    bracketId = 10,
                    routineId = 5,
                    redFromMatchId = null,
                    blueFromMatchId = null,
                    createdAt = now,
                ),
            ).thenReturn(match)

            val result = service.createMatch(10, 5, null, null)

            assertEquals(success(match), result)
        }

        @Test
        fun `should create match with fromMatch references`() {
            val matchWithRefs = match.copy(redFromMatchId = 2, blueFromMatchId = 3)

            whenever(repoTournament.findByBracketId(10)).thenReturn(mock())
            whenever(repoEnduranceRoutine.findById(5)).thenReturn(mock())
            whenever(
                repoMatch.createMatch(
                    bracketId = 10,
                    routineId = 5,
                    redFromMatchId = 2,
                    blueFromMatchId = 3,
                    createdAt = now,
                ),
            ).thenReturn(matchWithRefs)

            val result = service.createMatch(10, 5, 2, 3)

            assertEquals(success(matchWithRefs), result)
        }

        @Test
        fun `should fail when bracket does not exist`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(null)

            val result = service.createMatch(10, 5, null, null)

            assertEquals(failure(MatchError.BracketNotFound), result)
        }

        @Test
        fun `should fail when routine does not exist`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(mock())
            whenever(repoEnduranceRoutine.findById(5)).thenReturn(null)

            val result = service.createMatch(10, 5, null, null)

            assertEquals(failure(MatchError.RoutineNotFound), result)
        }

        @Test
        fun `should fail when repository fails to create match`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(mock())
            whenever(repoEnduranceRoutine.findById(5)).thenReturn(mock())
            whenever(repoMatch.createMatch(any(), any(), anyOrNull(), anyOrNull(), any()))
                .thenReturn(null)

            val result = service.createMatch(10, 5, null, null)

            assertEquals(failure(MatchError.BracketNotFound), result)
        }
    }

    @Nested
    inner class GetMatchById {
        private val now = clock.instant()

        private val match =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = null,
                athleteBlueId = null,
                status = MatchStatus.PENDING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        @Test
        fun `should return match when found`() {
            whenever(repoMatch.findById(1)).thenReturn(match)

            val result = service.getMatchById(1)

            assertEquals(success(match), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.getMatchById(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }
    }

    @Nested
    inner class GetMatchesByBracket {
        private val now = clock.instant()

        private val matches =
            listOf(
                Match(
                    id = 1,
                    bracketId = 10,
                    routineId = 5,
                    redFromMatchId = null,
                    blueFromMatchId = null,
                    athleteRedId = null,
                    athleteBlueId = null,
                    status = MatchStatus.PENDING,
                    winnerAthleteId = null,
                    startedAt = null,
                    finishedAt = null,
                    createdAt = now,
                ),
                Match(
                    id = 2,
                    bracketId = 10,
                    routineId = 5,
                    redFromMatchId = null,
                    blueFromMatchId = null,
                    athleteRedId = null,
                    athleteBlueId = null,
                    status = MatchStatus.RUNNING,
                    winnerAthleteId = null,
                    startedAt = null,
                    finishedAt = null,
                    createdAt = now,
                ),
            )

        @Test
        fun `should return matches for existing bracket`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(mock())
            whenever(repoMatch.findByBracketId(10)).thenReturn(matches)

            val result = service.getMatchesByBracket(10)

            assertEquals(success(matches), result)
        }

        @Test
        fun `should fail when bracket does not exist`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(null)

            val result = service.getMatchesByBracket(10)

            assertEquals(failure(MatchError.BracketNotFound), result)
        }

        @Test
        fun `should return empty list when bracket has no matches`() {
            whenever(repoTournament.findByBracketId(10)).thenReturn(mock())
            whenever(repoMatch.findByBracketId(10)).thenReturn(emptyList())

            val result = service.getMatchesByBracket(10)

            assertEquals(success(emptyList<Match>()), result)
        }
    }

    @Nested
    inner class UpdateMatchStatus {
        private val now = clock.instant()

        private val pendingMatch =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = null,
                athleteBlueId = null,
                status = MatchStatus.PENDING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        @Test
        fun `should update status from PENDING to READY`() {
            val updated = pendingMatch.copy(status = MatchStatus.READY)

            whenever(repoMatch.findById(1)).thenReturn(pendingMatch)
            whenever(repoMatch.updateStatus(1, MatchStatus.READY)).thenReturn(updated)

            val result = service.updateMatchStatus(1, MatchStatus.READY)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should update status from READY to RUNNING`() {
            val readyMatch = pendingMatch.copy(status = MatchStatus.READY)
            val updated = pendingMatch.copy(status = MatchStatus.RUNNING)

            whenever(repoMatch.findById(1)).thenReturn(readyMatch)
            whenever(repoMatch.updateStatus(1, MatchStatus.RUNNING)).thenReturn(updated)

            val result = service.updateMatchStatus(1, MatchStatus.RUNNING)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should update status from RUNNING to PAUSED`() {
            val runningMatch = pendingMatch.copy(status = MatchStatus.RUNNING)
            val updated = pendingMatch.copy(status = MatchStatus.PAUSED)

            whenever(repoMatch.findById(1)).thenReturn(runningMatch)
            whenever(repoMatch.updateStatus(1, MatchStatus.PAUSED)).thenReturn(updated)

            val result = service.updateMatchStatus(1, MatchStatus.PAUSED)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should update status from RUNNING to FINISHED`() {
            val runningMatch = pendingMatch.copy(status = MatchStatus.RUNNING)
            val updated = pendingMatch.copy(status = MatchStatus.FINISHED)

            whenever(repoMatch.findById(1)).thenReturn(runningMatch)
            whenever(repoMatch.updateStatus(1, MatchStatus.FINISHED)).thenReturn(updated)

            val result = service.updateMatchStatus(1, MatchStatus.FINISHED)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should update status from PAUSED to RUNNING`() {
            val pausedMatch = pendingMatch.copy(status = MatchStatus.PAUSED)
            val updated = pendingMatch.copy(status = MatchStatus.RUNNING)

            whenever(repoMatch.findById(1)).thenReturn(pausedMatch)
            whenever(repoMatch.updateStatus(1, MatchStatus.RUNNING)).thenReturn(updated)

            val result = service.updateMatchStatus(1, MatchStatus.RUNNING)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.updateMatchStatus(1, MatchStatus.READY)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail on invalid status transition from PENDING to RUNNING`() {
            whenever(repoMatch.findById(1)).thenReturn(pendingMatch)

            val result = service.updateMatchStatus(1, MatchStatus.RUNNING)

            assertEquals(failure(MatchError.InvalidStatusTransition), result)
        }

        @Test
        fun `should fail on invalid status transition from FINISHED`() {
            val finishedMatch = pendingMatch.copy(status = MatchStatus.FINISHED)

            whenever(repoMatch.findById(1)).thenReturn(finishedMatch)

            val result = service.updateMatchStatus(1, MatchStatus.RUNNING)

            assertEquals(failure(MatchError.InvalidStatusTransition), result)
        }

        @Test
        fun `should fail when repository fails to update status`() {
            whenever(repoMatch.findById(1)).thenReturn(pendingMatch)
            whenever(repoMatch.updateStatus(1, MatchStatus.READY)).thenReturn(null)

            val result = service.updateMatchStatus(1, MatchStatus.READY)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }
    }

    @Nested
    inner class SetMatchWinner {
        private val now = clock.instant()

        private val match =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = 100,
                athleteBlueId = 200,
                status = MatchStatus.FINISHED,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        @Test
        fun `should set red athlete as winner`() {
            val updated = match.copy(winnerAthleteId = 100)

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.updateWinner(1, 100)).thenReturn(updated)

            val result = service.setMatchWinner(1, 100)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should set blue athlete as winner`() {
            val updated = match.copy(winnerAthleteId = 200)

            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.updateWinner(1, 200)).thenReturn(updated)

            val result = service.setMatchWinner(1, 200)

            assertEquals(success(updated), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.setMatchWinner(1, 100)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when athlete is not in the match`() {
            whenever(repoMatch.findById(1)).thenReturn(match)

            val result = service.setMatchWinner(1, 999)

            assertEquals(failure(MatchError.AthleteNotInMatch), result)
        }

        @Test
        fun `should fail when repository fails to update winner`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.updateWinner(1, 100)).thenReturn(null)

            val result = service.setMatchWinner(1, 100)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }
    }

    @Nested
    inner class InitMatchProgress {
        private val now = clock.instant()

        private val match =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = 100,
                athleteBlueId = 200,
                status = MatchStatus.PENDING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        private val progress =
            MatchProgress(
                id = 1,
                matchId = 1,
                redCurrentExerciseId = null,
                blueCurrentExerciseId = null,
                redCurrentReps = 0,
                blueCurrentReps = 0,
                redFinishedAt = null,
                blueFinishedAt = null,
                timerStartedAt = null,
                timerRemainingSeconds = null,
                updatedAt = now,
            )

        @Test
        fun `should init match progress successfully`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            whenever(repoMatch.createMatchProgress(matchId = 1, updatedAt = now)).thenReturn(progress)

            val result = service.initMatchProgress(1)

            assertEquals(success(progress), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.initMatchProgress(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when progress already exists`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progress)

            val result = service.initMatchProgress(1)

            assertEquals(failure(MatchError.ProgressAlreadyExists), result)
        }

        @Test
        fun `should fail when repository fails to create progress`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)
            whenever(repoMatch.createMatchProgress(any(), any())).thenReturn(null)

            val result = service.initMatchProgress(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }
    }

    @Nested
    inner class GetMatchProgress {
        private val now = clock.instant()

        private val match =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = 100,
                athleteBlueId = 200,
                status = MatchStatus.RUNNING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        private val progress =
            MatchProgress(
                id = 1,
                matchId = 1,
                redCurrentExerciseId = null,
                blueCurrentExerciseId = null,
                redCurrentReps = 5,
                blueCurrentReps = 3,
                redFinishedAt = null,
                blueFinishedAt = null,
                timerStartedAt = null,
                timerRemainingSeconds = null,
                updatedAt = now,
            )

        @Test
        fun `should return progress when found`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(progress)

            val result = service.getMatchProgress(1)

            assertEquals(success(progress), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.getMatchProgress(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when progress does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findProgressByMatchId(1)).thenReturn(null)

            val result = service.getMatchProgress(1)

            assertEquals(failure(MatchError.ProgressNotFound), result)
        }
    }

    @Nested
    inner class UpdateTimer {
        private val now = clock.instant()

        private val runningMatch =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = 100,
                athleteBlueId = 200,
                status = MatchStatus.RUNNING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        private val updatedProgress =
            MatchProgress(
                id = 1,
                matchId = 1,
                redCurrentExerciseId = null,
                blueCurrentExerciseId = null,
                redCurrentReps = 0,
                blueCurrentReps = 0,
                redFinishedAt = null,
                blueFinishedAt = null,
                timerStartedAt = now,
                timerRemainingSeconds = 120,
                updatedAt = now,
            )

        @Test
        fun `should update timer successfully`() {
            whenever(repoMatch.findById(1)).thenReturn(runningMatch)
            whenever(
                repoMatch.updateTimer(
                    matchId = 1,
                    timerStartedAt = now,
                    timerRemainingSeconds = 120,
                    updatedAt = now,
                ),
            ).thenReturn(updatedProgress)

            val result = service.updateTimer(1, now, 120)

            assertEquals(success(updatedProgress), result)
        }

        @Test
        fun `should update timer with null values`() {
            val progressNullTimer =
                updatedProgress.copy(timerStartedAt = null, timerRemainingSeconds = null)

            whenever(repoMatch.findById(1)).thenReturn(runningMatch)
            whenever(
                repoMatch.updateTimer(
                    matchId = 1,
                    timerStartedAt = null,
                    timerRemainingSeconds = null,
                    updatedAt = now,
                ),
            ).thenReturn(progressNullTimer)

            val result = service.updateTimer(1, null, null)

            assertEquals(success(progressNullTimer), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.updateTimer(1, now, 120)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should fail when match is not running`() {
            val finishedMatch = runningMatch.copy(status = MatchStatus.FINISHED)

            whenever(repoMatch.findById(1)).thenReturn(finishedMatch)

            val result = service.updateTimer(1, now, 120)

            assertEquals(failure(MatchError.MatchNotRunning), result)
        }

        @Test
        fun `should fail when repository fails to update timer`() {
            whenever(repoMatch.findById(1)).thenReturn(runningMatch)
            whenever(repoMatch.updateTimer(any(), anyOrNull(), anyOrNull(), any())).thenReturn(null)

            val result = service.updateTimer(1, now, 120)

            assertEquals(failure(MatchError.ProgressNotFound), result)
        }
    }

    @Nested
    inner class GetMatchEvents {
        private val now = clock.instant()

        private val match =
            Match(
                id = 1,
                bracketId = 10,
                routineId = 5,
                redFromMatchId = null,
                blueFromMatchId = null,
                athleteRedId = 100,
                athleteBlueId = 200,
                status = MatchStatus.RUNNING,
                winnerAthleteId = null,
                startedAt = null,
                finishedAt = null,
                createdAt = now,
            )

        private val events =
            listOf(
                MatchEvent(
                    id = 1,
                    matchId = 1,
                    judgeId = 10,
                    eventType = MatchEventType.MATCH_STARTED,
                    payload = """{"athlete":"red","points":1}""",
                    createdAt = now,
                ),
                MatchEvent(
                    id = 2,
                    matchId = 1,
                    judgeId = 11,
                    eventType = MatchEventType.MATCH_FINISHED,
                    payload = """{"athlete":"blue","points":1}""",
                    createdAt = now,
                ),
            )

        @Test
        fun `should return events for existing match`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findEventsByMatchId(1)).thenReturn(events)

            val result = service.getMatchEvents(1)

            assertEquals(success(events), result)
        }

        @Test
        fun `should fail when match does not exist`() {
            whenever(repoMatch.findById(1)).thenReturn(null)

            val result = service.getMatchEvents(1)

            assertEquals(failure(MatchError.MatchNotFound), result)
        }

        @Test
        fun `should return empty list when match has no events`() {
            whenever(repoMatch.findById(1)).thenReturn(match)
            whenever(repoMatch.findEventsByMatchId(1)).thenReturn(emptyList())

            val result = service.getMatchEvents(1)

            assertEquals(success(emptyList<MatchEvent>()), result)
        }
    }
}
