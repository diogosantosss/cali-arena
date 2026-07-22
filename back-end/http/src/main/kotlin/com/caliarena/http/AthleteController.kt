package com.caliarena.http

import com.caliarena.domain.athlete.Athlete
import com.caliarena.http.model.Problem
import com.caliarena.http.model.athlete.CreateAthleteInput
import com.caliarena.http.model.athlete.UpdateAthleteInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.AthleteError
import com.caliarena.service.AthleteService
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
        @RequestBody input: CreateAthleteInput,
    ): ResponseEntity<Any> =
        athleteService
            .createAthlete(input.name, input.gender, input.clubId)
            .toResponse<AthleteError, Athlete>(
                onSuccess = { athlete ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("Location", "/api/athletes/${athlete.id}")
                        .body(athlete)
                },
                onError = { it.toResponseEntity() },
            )

    @PutMapping("/{id}")
    fun updateAthlete(
        @PathVariable id: Int,
        @RequestBody input: UpdateAthleteInput,
    ): ResponseEntity<Any> =
        athleteService
            .updateAthlete(id, input.name, input.gender, input.clubId)
            .toResponse<AthleteError, Athlete>(
                onSuccess = { athlete ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(athlete)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{id}")
    fun getAthleteById(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        athleteService
            .getAthleteById(id)
            .toResponse<AthleteError, Athlete>(
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
            .toResponse<AthleteError, List<Athlete>>(
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
            .toResponse<AthleteError, List<Athlete>>(
                onSuccess = { athletes ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(athletes)
                },
                onError = { it.toResponseEntity() },
            )

    private fun AthleteError.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            AthleteError.AthleteNotFound ->
                Problem.AthleteNotFound.response(HttpStatus.NOT_FOUND)

            AthleteError.ClubNotFound ->
                Problem.ClubNotFound.response(HttpStatus.NOT_FOUND)

            AthleteError.CreatingAthlete ->
                Problem.ErrorCreatingAthlete.response(HttpStatus.BAD_REQUEST)

            AthleteError.InvalidGender ->
                Problem.InvalidGender.response(HttpStatus.BAD_REQUEST)

            AthleteError.UpdatingAthlete ->
                Problem.UpdatingAthlete.response(HttpStatus.BAD_REQUEST)
        }
}
