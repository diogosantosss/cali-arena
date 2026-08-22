package com.caliarena.http

import com.caliarena.domain.match.MatchProgress
import com.caliarena.http.model.match.JudgeActionInput
import com.caliarena.http.model.match.JudgeErrorEvent
import com.caliarena.http.model.match.JudgeFinishedEvent
import com.caliarena.http.model.match.JudgeRepsEvent
import com.caliarena.http.model.match.RepSide.BLUE
import com.caliarena.http.model.match.RepSide.RED
import com.caliarena.service.Either
import com.caliarena.service.Failure
import com.caliarena.service.MatchError
import com.caliarena.service.MatchService
import com.caliarena.service.Success
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class JudgeActionsWsController(
    private val matchService: MatchService,
    private val messaging: SimpMessagingTemplate,
) {
    /**
     * Handles a rep adjustment from a judge for one side of an ongoing match.
     *
     * The input carries the new absolute rep count for a single side; the other side is left untouched.
     * On success a "REPS" event (that side's reps and current exercise) is broadcast to `/topic/matches/{matchId}`,
     * followed by a "FINISHED" event if the side has just completed its routine.
     * On failure an "ERROR" event with the error name is broadcast instead.
     *
     * Input: `{"side": "RED", "reps": 4}` sent to `/app/matches/{matchId}/actions`
     * Success: `REPS(side, reps, exerciseId)` then `FINISHED(side, finishedAt)` when done
     * Error: `ERROR(message)` e.g. `"MatchNotRunning"`
     */
    @MessageMapping("/matches/{matchId}/actions")
    fun onJudgeAction(
        @DestinationVariable matchId: Int,
        input: JudgeActionInput,
    ) {
        val result: Either<MatchError, MatchProgress> =
            when (input.side) {
                // Red Athlete reps update
                RED ->
                    matchService.updateAthletesReps(matchId, redReps = input.reps)

                // Blue Athlete reps update
                BLUE ->
                    matchService.updateAthletesReps(matchId, blueReps = input.reps)
            }

        val url = "/topic/matches/$matchId"
        when (result) {
            is Success -> {
                val prog = result.value
                val (reps, exerciseId) =
                    if (input.side == RED) {
                        prog.redCurrentReps to prog.redCurrentExerciseId
                    } else {
                        prog.blueCurrentReps to prog.blueCurrentExerciseId
                    }

                messaging.convertAndSend(url, JudgeRepsEvent(side = input.side, reps = reps, exerciseId = exerciseId))

                // checking if any athlete have finished
                val finishedAt =
                    if (input.side == RED) prog.redFinishedAt else prog.blueFinishedAt

                if (finishedAt != null) {
                    messaging.convertAndSend(url, JudgeFinishedEvent(side = input.side, finishedAt = finishedAt))
                }
            }

            is Failure -> {
                messaging.convertAndSend(url, JudgeErrorEvent(message = result.value::class.simpleName ?: "Error"))
            }
        }
    }
}
