package com.caliarena.http

import com.caliarena.http.model.Problem
import com.caliarena.http.model.screen.CreateScreenRoutineInput
import com.caliarena.http.model.screen.UpdateDisplayOrderInput
import com.caliarena.http.model.screen.UpdateVisibilityInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ScreenRoutineError
import com.caliarena.service.ScreenRoutineService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/screen-routines")
class ScreenRoutineController(
    private val service: ScreenRoutineService,
) {
    @GetMapping
    fun getAll(
        @PathVariable tournamentId: Int,
    ): ResponseEntity<Any> =
        service
            .getByTournamentId(tournamentId)
            .toResponse(
                onSuccess = {
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.LOCATION, "/api/tournaments/$tournamentId/screen-routines")
                        .body(it)
                },
                onError = { it.toResponseEntity() },
            )

    @PostMapping
    fun create(
        @PathVariable tournamentId: Int,
        @RequestBody input: CreateScreenRoutineInput,
    ): ResponseEntity<Any> =
        service
            .create(tournamentId, input.routineId, input.displayOrder, input.label)
            .toResponse(
                onSuccess = {
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(it)
                },
                onError = { it.toResponseEntity() },
            )

    @PatchMapping("/{id}/visibility")
    fun updateVisibility(
        @PathVariable tournamentId: Int,
        @PathVariable id: Int,
        @RequestBody input: UpdateVisibilityInput,
    ): ResponseEntity<Any> =
        service
            .update(tournamentId, id, input.isVisible, null, null)
            .toResponse(
                onSuccess = { ResponseEntity.ok(it) },
                onError = { it.toResponseEntity() },
            )

    @PatchMapping("/{id}/order")
    fun updateDisplayOrder(
        @PathVariable tournamentId: Int,
        @PathVariable id: Int,
        @RequestBody input: UpdateDisplayOrderInput,
    ): ResponseEntity<Any> =
        service
            .update(tournamentId, id, null, input.displayOrder, null)
            .toResponse(
                onSuccess = { ResponseEntity.ok(it) },
                onError = { it.toResponseEntity() },
            )

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable tournamentId: Int,
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        service
            .delete(tournamentId, id)
            .toResponse(
                onSuccess = { ResponseEntity.noContent().build() },
                onError = { it.toResponseEntity() },
            )

    private fun ScreenRoutineError.toResponseEntity() =
        when (this) {
            ScreenRoutineError.TournamentNotFound ->
                Problem.TournamentNotFound.response(HttpStatus.NOT_FOUND)
            ScreenRoutineError.RoutineNotFound ->
                Problem.RoutineNotFound.response(HttpStatus.NOT_FOUND)
            ScreenRoutineError.ScreenRoutineNotFound ->
                Problem.ScreenRoutineNotFound.response(HttpStatus.NOT_FOUND)
            ScreenRoutineError.TournamentMismatch ->
                Problem.TournamentMismatch.response(HttpStatus.CONFLICT)
            ScreenRoutineError.ErrorUpdatingScreenRoutine ->
                Problem.ErrorUpdatingScreenRoutine.response(HttpStatus.INTERNAL_SERVER_ERROR)
        }
}
