package com.caliarena.http

import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.model.tournament.CreateTournamentInput
import com.caliarena.http.model.tournament.UpdateScreenInput
import com.caliarena.http.utils.hasAnyRole
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ApiError
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
        user: AuthenticatedUser,
        @RequestBody input: CreateTournamentInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return tournamentService
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
    }

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
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @RequestBody input: UpdateScreenInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return tournamentService
            .updateScreen(
                tournamentId = tournamentId,
                screen = input.screen,
                currentMatchId = input.currentMatchId,
                currentBracketId = input.currentBracketId,
                currentDivision = input.currentDivision,
            ).toResponse(
                onSuccess = { state ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(state)
                },
                onError = { it.toResponseEntity() },
            )
    }

    private fun String.convertDate(): Instant = LocalDate.parse(this).atStartOfDay().toInstant(ZoneOffset.UTC)
}
