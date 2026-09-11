package com.caliarena.http

import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.screen.CreateScreenRoutineInput
import com.caliarena.http.model.screen.UpdateDisplayOrderInput
import com.caliarena.http.model.screen.UpdateVisibilityInput
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.utils.hasAnyRole
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ApiError
import com.caliarena.service.ScreenRoutineService
import com.caliarena.service.sse.SpectatorPublisher
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/screen-routines")
class ScreenRoutineController(
    private val service: ScreenRoutineService,
    private val publisher: SpectatorPublisher,
) {
    @GetMapping("/listen")
    fun listen(
        @PathVariable tournamentId: Int,
    ): SseEmitter {
        val sseEmitter = SseEmitter(TimeUnit.HOURS.toMillis(1))
        publisher.addEmitter(
            tournamentId,
            SseSpectatorEmitterAdapter(
                sseEmitter,
            ),
        )
        return sseEmitter
    }

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
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @RequestBody input: CreateScreenRoutineInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return service
            .create(tournamentId, input.routineId, input.displayOrder, input.label)
            .toResponse(
                onSuccess = { screenRoutine: ScreenRoutine ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(screenRoutine)
                },
                onError = { it.toResponseEntity() },
            )
    }

    @PatchMapping("/{id}/visibility")
    fun updateVisibility(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @PathVariable id: Int,
        @RequestBody input: UpdateVisibilityInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return service
            .update(tournamentId, id, input.isVisible, null, null)
            .toResponse(
                onSuccess = { ResponseEntity.ok(it) },
                onError = { it.toResponseEntity() },
            )
    }

    @PatchMapping("/{id}/order")
    fun updateDisplayOrder(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @PathVariable id: Int,
        @RequestBody input: UpdateDisplayOrderInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return service
            .update(tournamentId, id, null, input.displayOrder, null)
            .toResponse(
                onSuccess = { ResponseEntity.ok(it) },
                onError = { it.toResponseEntity() },
            )
    }

    @DeleteMapping("/{id}")
    fun delete(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @PathVariable id: Int,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return service
            .delete(tournamentId, id)
            .toResponse(
                onSuccess = { ResponseEntity.noContent().build() },
                onError = { it.toResponseEntity() },
            )
    }
}
