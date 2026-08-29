package com.caliarena.http

import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.match.CreateMatchInput
import com.caliarena.http.model.match.UpdateRepsInput
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.utils.hasAnyRole
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ApiError
import com.caliarena.service.MatchService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
) {
    @PostMapping
    fun createMatch(
        user: AuthenticatedUser,
        @RequestBody input: CreateMatchInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return matchService
            .createMatch(input.bracketId, input.routineId, input.judgeId, input.athleteRedId, input.athleteBlueId)
            .toResponse(
                onSuccess = { match: Match ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/matches/${match.id}")
                        .body(match)
                },
                onError = { it.toResponseEntity() },
            )
    }

    @PutMapping("/{id}/start")
    fun startMatch(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN, UserRole.JUDGE)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return matchService
            .startMatch(id)
            .toResponse(
                onSuccess = { prog: MatchProgress ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.LOCATION, "/api/matches/$id")
                        .body(prog)
                },
                onError = { it.toResponseEntity() },
            )
    }

    @PutMapping("/{matchId}/reps")
    fun updateMatchReps(
        user: AuthenticatedUser,
        @PathVariable matchId: Int,
        @RequestBody input: UpdateRepsInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN, UserRole.JUDGE)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return matchService
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
    }

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
