package com.caliarena.http

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.athlete.CreateAthleteInput
import com.caliarena.http.model.athlete.UpdateAthleteInput
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.utils.hasAnyRole
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ApiError
import com.caliarena.service.AthleteService
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
@RequestMapping("/api/athletes")
class AthleteController(
    private val athleteService: AthleteService,
) {
    @PostMapping
    fun createAthlete(
        user: AuthenticatedUser,
        @RequestBody input: CreateAthleteInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return athleteService
            .createAthlete(input.name, input.gender, input.clubId)
            .toResponse(
                onSuccess = { athlete ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/athletes/${athlete.id}")
                        .body(athlete)
                },
                onError = { it.toResponseEntity() },
            )
    }

    @PutMapping("/{id}")
    fun updateAthlete(
        user: AuthenticatedUser,
        @PathVariable id: Int,
        @RequestBody input: UpdateAthleteInput,
    ): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return athleteService
            .updateAthlete(id, input.name, input.gender, input.clubId)
            .toResponse(
                onSuccess = { athlete ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(athlete)
                },
                onError = { it.toResponseEntity() },
            )
    }

    @GetMapping("/{id}")
    fun getAthleteById(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        athleteService
            .getAthleteById(id)
            .toResponse(
                onSuccess = { athlete ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(athlete)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping
    fun getAllAthletes(): ResponseEntity<List<Athlete>> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(athleteService.getAllAthletes())

    @GetMapping("/club/{clubId}")
    fun getAthletesByClub(
        @PathVariable clubId: Int,
    ): ResponseEntity<Any> =
        athleteService
            .getAthletesByClub(clubId)
            .toResponse(
                onSuccess = { athletes ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(athletes)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/gender/{gender}")
    fun getAthletesByGender(
        @PathVariable gender: String,
    ): ResponseEntity<Any> =
        athleteService
            .getAthletesByGender(gender)
            .toResponse(
                onSuccess = { athletes ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(athletes)
                },
                onError = { it.toResponseEntity() },
            )
}
