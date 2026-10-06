package com.caliarena.http

import com.caliarena.domain.match.JudgeActionInput
import com.caliarena.domain.match.JudgeActionType
import com.caliarena.domain.match.JudgeErrorEvent
import com.caliarena.domain.match.JudgeEvent
import com.caliarena.domain.match.JudgeFinishedEvent
import com.caliarena.domain.match.JudgeRepsEvent
import com.caliarena.domain.match.JudgeStartedEvent
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.RepSide
import com.caliarena.domain.match.RepSide.BLUE
import com.caliarena.domain.match.RepSide.RED
import com.caliarena.domain.user.User
import com.caliarena.service.ApiError
import com.caliarena.service.Either
import com.caliarena.service.Failure
import com.caliarena.service.MatchService
import com.caliarena.service.Success
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal
import java.time.Instant
import java.util.Optional

@Controller
class JudgeWsController(
    private val matchService: MatchService,
    private val messaging: SimpMessagingTemplate,
) {
    companion object {
        const val BROADCAST_TOPIC_PREFIX = "/topic/matches/"

        const val NOT_AUTHORIZED_MESSAGE = "not-authorized"
    }

    /**
     * Handles a judge action, dispatching to a per-action handler that broadcasts
     * the resulting event to `/topic/matches/{matchId}`.
     *
     * The acting user comes from the handshake principal; the match service then
     * verifies the user is an admin, the tournament host or one of its judges.
     *
     * Input: `{"action":"START","side":"RED"}` or `{"action":"ADJUST","side":"RED","reps":4}`
     * sent to `/app/matches/{matchId}/actions`
     * Success: `STARTED(match, progress)`, `REPS(side, reps, exerciseId)` then
     * `FINISHED(side, finishedAt)` when the side completes
     * Error: `ERROR(message)` e.g. "MatchNotRunning"
     */
    @MessageMapping("/matches/{matchId}/actions")
    fun onJudgeAction(
        @DestinationVariable matchId: Int,
        input: JudgeActionInput,
        principal: Optional<Principal>,
    ) {
        val user =
            (principal.orElse(null) as? WsAuthenticatedPrincipal)?.authenticatedUser?.user
                ?: return send(matchId, JudgeErrorEvent(message = NOT_AUTHORIZED_MESSAGE))

        when (input.action) {
            JudgeActionType.START -> start(user, matchId)
            JudgeActionType.ADJUST -> adjustReps(user, matchId, input)
            JudgeActionType.FINISH -> forceFinish(user, matchId, input)
        }
    }

    private fun start(
        user: User,
        matchId: Int,
    ) {
        matchService.startMatch(user, matchId).broadcast(matchId) { started ->
            send(matchId, JudgeStartedEvent(match = started.match, progress = started.progress))
        }
    }

    private fun adjustReps(
        user: User,
        matchId: Int,
        input: JudgeActionInput,
    ) {
        val result =
            when (input.side) {
                RED -> matchService.updateAthletesReps(user, matchId, redReps = input.reps)
                BLUE -> matchService.updateAthletesReps(user, matchId, blueReps = input.reps)
            }

        result.broadcast(matchId) { progress ->
            send(
                matchId,
                JudgeRepsEvent(
                    side = input.side,
                    reps = progress.repsOf(input.side),
                    exerciseId = progress.exerciseIdOf(input.side),
                ),
            )
            broadcastFinishIfDone(matchId, input.side, progress)
        }
    }

    private fun forceFinish(
        user: User,
        matchId: Int,
        input: JudgeActionInput,
    ) {
        matchService.forceFinishSide(user, matchId, input.side).broadcast(matchId) { progress ->
            broadcastFinishIfDone(matchId, input.side, progress)
        }
    }

    private fun broadcastFinishIfDone(
        matchId: Int,
        side: RepSide,
        progress: MatchProgress,
    ) {
        progress.finishedAtOf(side)?.let { finishedAt ->
            send(matchId, JudgeFinishedEvent(side = side, finishedAt = finishedAt))
        }
    }

    private fun <T> Either<ApiError, T>.broadcast(
        matchId: Int,
        onSuccess: (T) -> Unit,
    ) {
        when (this) {
            is Success -> onSuccess(value)
            is Failure -> send(matchId, JudgeErrorEvent(message = value.problemType))
        }
    }

    private fun MatchProgress.repsOf(side: RepSide): Int =
        when (side) {
            RED -> redCurrentReps
            BLUE -> blueCurrentReps
        }

    private fun MatchProgress.exerciseIdOf(side: RepSide): Int? =
        when (side) {
            RED -> redCurrentExerciseId
            BLUE -> blueCurrentExerciseId
        }

    private fun MatchProgress.finishedAtOf(side: RepSide): Instant? =
        when (side) {
            RED -> redFinishedAt
            BLUE -> blueFinishedAt
        }

    private fun send(
        matchId: Int,
        event: JudgeEvent,
    ) {
        messaging.convertAndSend("$BROADCAST_TOPIC_PREFIX$matchId", event)
    }
}
