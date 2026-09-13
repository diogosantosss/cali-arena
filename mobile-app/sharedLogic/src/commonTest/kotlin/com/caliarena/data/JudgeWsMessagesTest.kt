package com.caliarena.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class JudgeWsMessagesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun parsesRepsEvent() {
        val body = """{"type":"REPS","side":"RED","reps":7,"exerciseId":3}"""

        val event = parseJudgeEvent(json, body)

        assertEquals(JudgeRepsEvent(side = RepSide.RED, reps = 7, exerciseId = 3), event)
    }

    @Test
    fun parsesRepsEventWithoutExerciseId() {
        val body = """{"type":"REPS","side":"BLUE","reps":0}"""

        val event = parseJudgeEvent(json, body)

        assertEquals(JudgeRepsEvent(side = RepSide.BLUE, reps = 0, exerciseId = null), event)
    }

    @Test
    fun parsesFinishedEvent() {
        val body = """{"type":"FINISHED","side":"RED","finishedAt":"2026-08-09T10:11:12Z"}"""

        val event = parseJudgeEvent(json, body)

        assertEquals(JudgeFinishedEvent(side = RepSide.RED, finishedAt = "2026-08-09T10:11:12Z"), event)
    }

    @Test
    fun parsesErrorEvent() {
        val body = """{"type":"ERROR","message":"match-not-running"}"""

        val event = parseJudgeEvent(json, body)

        assertEquals(JudgeErrorEvent(message = "match-not-running"), event)
    }

    @Test
    fun parsesStartedEvent() {
        val body =
            """{"type":"STARTED","match":{"id":1,"bracketId":2,"routineId":3,"athleteRedId":5,"athleteBlueId":6,"winnerAthleteId":null,"status":"RUNNING","startedAt":"2026-08-09T10:00:00Z","finishedAt":null,"createdAt":"2026-08-09T09:00:00Z"},"progress":{"id":1,"matchId":1,"redCurrentExerciseId":3,"blueCurrentExerciseId":3,"redCurrentReps":0,"blueCurrentReps":0,"timerStartedAt":"2026-08-09T10:00:00Z","updatedAt":"2026-08-09T10:00:00Z"}}"""

        val event = parseJudgeEvent(json, body)

        assertEquals(
            JudgeStartedEvent(
                match =
                    MatchOutput(
                        id = 1,
                        bracketId = 2,
                        routineId = 3,
                        athleteRedId = 5,
                        athleteBlueId = 6,
                        winnerAthleteId = null,
                        status = MatchStatus.RUNNING,
                        startedAt = "2026-08-09T10:00:00Z",
                        finishedAt = null,
                        createdAt = "2026-08-09T09:00:00Z",
                    ),
                progress =
                    MatchProgressOutput(
                        id = 1,
                        matchId = 1,
                        redCurrentExerciseId = 3,
                        blueCurrentExerciseId = 3,
                        redCurrentReps = 0,
                        blueCurrentReps = 0,
                        timerStartedAt = "2026-08-09T10:00:00Z",
                        updatedAt = "2026-08-09T10:00:00Z",
                    ),
            ),
            event,
        )
    }

    @Test
    fun unknownTypeIsUnhandled() {
        val body = """{"type":"UNKNOWN","message":"nope"}"""

        val event = parseJudgeEvent(json, body)

        assertEquals(UnhandledJudgeEvent, event)
    }

    @Test
    fun malformedBodyIsUnhandled() {
        val event = parseJudgeEvent(json, "not json at all")

        assertEquals(UnhandledJudgeEvent, event)
    }

    @Test
    fun errorEventMapsMessageToErrorCode() {
        assertEquals(ErrorCode.MATCH_NOT_RUNNING, JudgeErrorEvent(message = "match-not-running").toErrorCode())
        assertEquals(ErrorCode.UNKNOWN_ERROR, JudgeErrorEvent(message = "whatever").toErrorCode())
    }
}
