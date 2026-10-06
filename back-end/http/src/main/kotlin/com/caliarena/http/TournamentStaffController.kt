package com.caliarena.http

import com.caliarena.domain.RequiresRole
import com.caliarena.domain.tournament.TournamentStaff
import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.model.tournament.AssignHostInput
import com.caliarena.http.model.tournament.AssignJudgeInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.TournamentStaffService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Staff of a tournament: the single host and the judges allowed to control its
 * matches. Only admins may assign the host; hosts and admins manage judges;
 * judges may read it.
 */
@RestController
@RequestMapping("/api/tournaments")
class TournamentStaffController(
    private val staffService: TournamentStaffService,
) {
    @GetMapping("/mine")
    fun getMyTournaments(user: AuthenticatedUser): ResponseEntity<Any> =
        staffService
            .getMyTournaments(user.user)
            .toResponse(
                onSuccess = { ResponseEntity.status(HttpStatus.OK).body(it) },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{tournamentId}/staff")
    @RequiresRole([UserRole.ADMIN, UserRole.HOST, UserRole.JUDGE])
    fun getStaff(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
    ): ResponseEntity<Any> =
        staffService
            .getStaff(user.user, tournamentId)
            .toResponse(
                onSuccess = { staff ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .header(HttpHeaders.LOCATION, "/api/tournaments/$tournamentId/staff")
                        .body(staff)
                },
                onError = { it.toResponseEntity() },
            )

    @PutMapping("/{tournamentId}/staff/host")
    @RequiresRole([UserRole.ADMIN])
    fun assignHost(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @RequestBody input: AssignHostInput,
    ): ResponseEntity<Any> =
        staffService
            .assignHost(user.user, tournamentId, input.userId)
            .toResponse(
                onSuccess = { staff -> staffResponse(staff) },
                onError = { it.toResponseEntity() },
            )

    @PostMapping("/{tournamentId}/staff/judges")
    @RequiresRole([UserRole.ADMIN, UserRole.HOST])
    fun addJudge(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @RequestBody input: AssignJudgeInput,
    ): ResponseEntity<Any> =
        staffService
            .addJudge(user.user, tournamentId, input.userId)
            .toResponse(
                onSuccess = { staff -> staffResponse(staff) },
                onError = { it.toResponseEntity() },
            )

    @DeleteMapping("/{tournamentId}/staff/judges/{userId}")
    @RequiresRole([UserRole.ADMIN, UserRole.HOST])
    fun removeJudge(
        user: AuthenticatedUser,
        @PathVariable tournamentId: Int,
        @PathVariable userId: Int,
    ): ResponseEntity<Any> =
        staffService
            .removeJudge(user.user, tournamentId, userId)
            .toResponse(
                onSuccess = { ResponseEntity.status(HttpStatus.NO_CONTENT).build() },
                onError = { it.toResponseEntity() },
            )

    private fun staffResponse(staff: TournamentStaff): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.OK)
            .header(HttpHeaders.LOCATION, "/api/tournaments/${staff.tournamentId}/staff")
            .body(staff)
}
