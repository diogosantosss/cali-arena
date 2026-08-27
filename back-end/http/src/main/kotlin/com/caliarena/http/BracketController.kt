package com.caliarena.http

import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.model.tournament.CreateBracketInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.BracketService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/brackets")
class BracketController(
    private val bracketService: BracketService,
) {
    @PostMapping
    fun createBracket(
        @RequestBody input: CreateBracketInput,
    ): ResponseEntity<Any> =
        bracketService
            .createBracket(
                tournamentId = input.tournamentId,
                gender = input.gender,
                stage = input.stage,
            ).toResponse(
                onSuccess = { bracket ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/brackets/${bracket.id}")
                        .body(bracket)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/tournament/{tournamentId}")
    fun getBracketsByTournamentId(
        @PathVariable tournamentId: Int,
    ): ResponseEntity<Any> =
        bracketService
            .getBracketsByTournament(tournamentId)
            .toResponse(
                onSuccess = { brackets ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(brackets)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/tournament/{tournamentId}/gender/{gender}")
    fun getBracketsByTournamentAndGender(
        @PathVariable tournamentId: Int,
        @PathVariable gender: String,
    ): ResponseEntity<Any> =
        bracketService
            .getBracketsByTournamentAndGender(tournamentId, gender)
            .toResponse(
                onSuccess = { brackets ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(brackets)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/tournament/{tournamentId}/gender/{gender}/overview")
    fun getBracketOverview(
        @PathVariable tournamentId: Int,
        @PathVariable gender: String,
    ): ResponseEntity<Any> =
        bracketService
            .getBracketOverview(tournamentId, gender)
            .toResponse(
                onSuccess = { overview ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(overview)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{bracketId}/leaderboard")
    fun getBracketLeaderboard(
        @PathVariable bracketId: Int,
    ): ResponseEntity<Any> =
        bracketService
            .getBracketLeaderboard(bracketId)
            .toResponse(
                onSuccess = {
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(it)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/tournament/{tournamentId}/summary")
    fun getBracketsSummary(
        @PathVariable tournamentId: Int,
        @RequestParam gender: String,
    ): ResponseEntity<Any> =
        bracketService
            .getTournamentBracketsSummary(tournamentId, gender)
            .toResponse(
                onSuccess = { summary ->
                    ResponseEntity.ok(summary)
                },
                onError = { it.toResponseEntity() },
            )
}
