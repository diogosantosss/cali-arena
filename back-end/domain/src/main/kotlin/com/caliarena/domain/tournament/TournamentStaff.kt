package com.caliarena.domain.tournament

import com.caliarena.domain.user.UserRole

/**
 * Minimal, password-free view of a user assigned to a tournament, used by the
 * host roster screens.
 */
data class StaffMember(
    val id: Int,
    val username: String,
    val role: UserRole,
)

/**
 * Staff of a single tournament: at most one host and any number of judges.
 */
data class TournamentStaff(
    val tournamentId: Int,
    val host: StaffMember?,
    val judges: List<StaffMember>,
)

/**
 * Tournaments the acting user is responsible for, either as host or as judge.
 */
data class MyTournaments(
    val hosting: List<Tournament>,
    val judging: List<Tournament>,
)
