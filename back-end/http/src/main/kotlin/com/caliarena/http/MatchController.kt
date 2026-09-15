package com.caliarena.http

import com.caliarena.domain.RequiresRole
import com.caliarena.domain.match.JudgeStartedEvent
import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.match.StartedMatch
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.match.CreateMatchInput
import com.caliarena.http.model.match.UpdateRepsInput
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.utils.toResponse
import com.caliarena.service.MatchService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val matchService: MatchService,
    private val messaging: SimpMessagingTemplate,
) {
    @PostMapping
    @RequiresRole([UserRole.ADMIN, UserRole.JUDGE])
    fun createMatch(
        @RequestBody input: CreateMatchInput,
    ): ResponseEntity<Any> =
        matchService
            .createMatch(input.bracketId, input.routineId, input.athleteRedId, input.athleteBlueId)
            .toResponse(
                onSuccess = { match: Match ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/matches/${match.id}")
                        .body(match)
                },
                onError = { it.toResponseEntity() },
            )

    @PutMapping("/{id}/start")
    @RequiresRole([UserRole.ADMIN, UserRole.JUDGE])
    fun startMatch(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        matchService
            .startMatch(id)
            .toResponse(
                onSuccess = { started: StartedMatch ->
                    messaging.convertAndSend(
                        "${JudgeWsController.BROADCAST_TOPIC_PREFIX}$id",
                        JudgeStartedEvent(match = started.match, progress = started.progress),
                    )
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.LOCATION, "/api/matches/$id")
                        .body(started.progress)
                },
                onError = { it.toResponseEntity() },
            )

    @PutMapping("/{matchId}/reps")
    @RequiresRole([UserRole.ADMIN, UserRole.JUDGE])
    fun updateMatchReps(
        @PathVariable matchId: Int,
        @RequestBody input: UpdateRepsInput,
    ): ResponseEntity<Any> =
        matchService
            .updateAthletesReps(matchId, input.redReps, input.blueReps)
            .toResponse(
                onSuccess = { prog: MatchProgress ->
                    ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .header(HttpHeaders.LOCATION, "/api/matches/$matchId")
                        .body(prog)
                },
                onError = { it.toResponseEntity() },
            )

    @DeleteMapping("/{id}")
    @RequiresRole([UserRole.ADMIN])
    fun deleteMatch(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        matchService
            .deleteMatch(id)
            .toResponse(
                onSuccess = {
                    ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping
    @RequiresRole([UserRole.ADMIN, UserRole.JUDGE])
    fun getAllMatches(): ResponseEntity<Any> =
        matchService
            .getAllMatches()
            .toResponse(
                onSuccess = { matches: List<Match> ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(matches)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{id}")
    fun getMatchById(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        matchService
            .getMatchById(id)
            .toResponse(
                onSuccess = { match: Match ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(match)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{id}/progress")
    fun getMatchProgressById(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        matchService
            .getMatchProgress(id)
            .toResponse(
                onSuccess = { prog: MatchProgress ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.LOCATION, "/api/matches/$id/progress")
                        .body(prog)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/bracket/{id}")
    fun getMatchesByBracketId(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        matchService
            .getMatchesByBracket(id)
            .toResponse(
                onSuccess = { matches: List<Match> ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(matches)
                },
                onError = { it.toResponseEntity() },
            )
}
