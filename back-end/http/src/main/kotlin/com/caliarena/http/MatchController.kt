package com.caliarena.http

import com.caliarena.service.MatchError
import com.caliarena.service.MatchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val matchService: MatchService,
) {
    private fun MatchError.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            MatchError.AthleteNotFound -> TODO()
            MatchError.AthleteNotInMatch -> TODO()
            MatchError.AthletesNotAssigned -> TODO()
            MatchError.BracketNotFound -> TODO()
            MatchError.ErrorCreatingMatchProg -> TODO()
            MatchError.ExerciseNotFound -> TODO()
            MatchError.InvalidStatusTransition -> TODO()
            MatchError.JudgeNotFound -> TODO()
            MatchError.MatchNotFound -> TODO()
            MatchError.MatchNotRunning -> TODO()
            MatchError.ProgressAlreadyExists -> TODO()
            MatchError.ProgressNotFound -> TODO()
            MatchError.RoutineNotFound -> TODO()
            MatchError.SameAthleteOnBothSides -> TODO()
        }
}
