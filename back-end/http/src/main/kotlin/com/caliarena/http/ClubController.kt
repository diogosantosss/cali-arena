package com.caliarena.http

import com.caliarena.domain.club.Club
import com.caliarena.http.model.Problem
import com.caliarena.http.model.club.CreateClubInput
import com.caliarena.http.model.club.UpdateClubInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ClubError
import com.caliarena.service.ClubService
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
@RequestMapping("/api/clubs")
class ClubController(
    private val clubService: ClubService,
) {
    @PostMapping
    fun createClub(
        @RequestBody input: CreateClubInput,
    ): ResponseEntity<Any> =
        clubService
            .createClub(input.name, input.shortName)
            .toResponse<ClubError, Club>(
                onSuccess = { club ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/clubs/${club.id}")
                        .body(club)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{id}")
    fun getClubById(
        @PathVariable id: Int,
    ): ResponseEntity<Any> =
        clubService
            .getClubById(id)
            .toResponse<ClubError, Club>(
                onSuccess = { club ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(club)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping
    fun getAllClubs(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(clubService.getAllClubs())

    @PutMapping("/{id}")
    fun updateClub(
        @PathVariable id: Int,
        @RequestBody input: UpdateClubInput,
    ): ResponseEntity<Any> =
        clubService
            .updateClub(id, input.name, input.shortName)
            .toResponse<ClubError, Club>(
                onSuccess = { club ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(club)
                },
                onError = { it.toResponseEntity() },
            )

    private fun ClubError.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            ClubError.ClubAlreadyExists ->
                Problem.ClubAlreadyExists.response(HttpStatus.CONFLICT)

            ClubError.ClubNotFound ->
                Problem.ClubNotFound.response(HttpStatus.NOT_FOUND)

            ClubError.UpdatingClub ->
                Problem.CreatingClub.response(HttpStatus.BAD_REQUEST)
        }
}
