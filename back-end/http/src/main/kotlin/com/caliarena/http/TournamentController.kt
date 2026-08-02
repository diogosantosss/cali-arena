package com.caliarena.http

import com.caliarena.http.model.Problem
import com.caliarena.http.model.tournament.CreateBracketInput
import com.caliarena.http.model.tournament.CreateTournamentInput
import com.caliarena.http.model.tournament.UpdateScreenInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.TournamentError
import com.caliarena.service.TournamentService
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/tournaments")
class TournamentController(
    private val tournamentService: TournamentService,
) {
    @PostMapping
    fun createTournament(
        @RequestBody input: CreateTournamentInput,
    ): ResponseEntity<Any> =
        tournamentService
            .createTournament(
                name = input.name,
                location = input.location,
                startDate = input.startDate?.convertDate(),
                endDate = input.endDate?.convertDate(),
            ).toResponse(
                onSuccess = { tournament ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("Location", "/api/tournaments/${tournament.id}")
                        .body(tournament)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping
    fun getTournaments(): ResponseEntity<Any> = ResponseEntity.ok(tournamentService.getAllTournaments())

    @GetMapping("/{id}")
    fun getTournamentById(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        tournamentService
            .getTournamentById(id)
            .toResponse(
                onSuccess = { tournament ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.LOCATION, "/api/tournaments/${tournament.id}")
                        .body(tournament)
                },
                onError = { it.toResponseEntity() },
            )

    @PostMapping("/bracket")
    fun createBracket(
        @RequestBody input: CreateBracketInput,
    ): ResponseEntity<Any> =
        tournamentService
            .createBracket(
                tournamentId = input.tournamentId,
                gender = input.gender,
                stage = input.stage,
            ).toResponse(
                onSuccess = { bracket ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/tournaments/bracket/${bracket.id}")
                        .body(bracket)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("{id}/brackets")
    fun getBracketsByTournamentId(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        tournamentService
            .getBracketsByTournament(id)
            .toResponse(
                onSuccess = { brackets ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(brackets)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("{id}/brackets/gender/{gender}")
    fun getBracketsByTournamentAndGender(
        @PathVariable id: Int,
        @PathVariable gender: String,
    ): ResponseEntity<Any> =
        tournamentService
            .getBracketsByTournamentAndGender(id, gender)
            .toResponse(
                onSuccess = { brackets ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(brackets)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{tournamentId}/bracket/{gender}/overview")
    fun getBracketOverview(
        @PathVariable tournamentId: Int,
        @PathVariable gender: String,
    ): ResponseEntity<Any> =
        tournamentService
            .getBracketOverview(tournamentId, gender)
            .toResponse(
                onSuccess = { overview ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(overview)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{tournamentId}/state")
    fun getTournamentState(
        @PathVariable tournamentId: Int,
    ): ResponseEntity<Any> =
        tournamentService
            .getTournamentState(tournamentId)
            .toResponse(
                onSuccess = { state ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(state)
                },
                onError = { it.toResponseEntity() },
            )

    @PutMapping("/{tournamentId}/state/screen")
    fun updateScreen(
        @PathVariable tournamentId: Int,
        @RequestBody input: UpdateScreenInput,
    ): ResponseEntity<Any> =
        tournamentService
            .updateScreen(
                tournamentId = tournamentId,
                screen = input.screen,
                currentMatchId = input.currentMatchId,
            ).toResponse(
                onSuccess = { state ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(state)
                },
                onError = { it.toResponseEntity() },
            )

    private fun String.convertDate(): Instant = LocalDate.parse(this).atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun TournamentError.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            TournamentError.InvalidBracketStage ->
                Problem.InvalidBracketStage.response(HttpStatus.BAD_REQUEST)
            TournamentError.InvalidGender ->
                Problem.InvalidGender.response(HttpStatus.BAD_REQUEST)
            TournamentError.InvalidTournamentStatus ->
                Problem.InvalidTournamentStatus.response(HttpStatus.BAD_REQUEST)
            TournamentError.InvalidScreenState ->
                Problem.InvalidScreenState.response(HttpStatus.BAD_REQUEST)
            TournamentError.TournamentStateNotFound ->
                Problem.TournamentStateNotFound.response(HttpStatus.NOT_FOUND)
            TournamentError.BracketNotFound ->
                Problem.BracketNotFound.response(HttpStatus.NOT_FOUND)
            TournamentError.TournamentNotFound ->
                Problem.TournamentNotFound.response(HttpStatus.NOT_FOUND)
            TournamentError.BracketAlreadyExists ->
                Problem.BracketAlreadyExists.response(HttpStatus.CONFLICT)
            TournamentError.TournamentAlreadyExists ->
                Problem.TournamentAlreadyExists.response(HttpStatus.CONFLICT)
            TournamentError.TournamentStateAlreadyExists ->
                Problem.TournamentStateAlreadyExists.response(HttpStatus.CONFLICT)
        }
}
