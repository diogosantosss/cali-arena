package com.caliarena.service

import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.trx.Transaction
import org.springframework.data.repository.findByIdOrNull

// Authorization rules for tournament scoped resources.
//
// The global [UserRole] is only a coarse gate applied at the HTTP layer: an ADMIN has
// access everywhere, and a HOST or JUDGE is only meaningful inside the tournaments they
// were assigned to (`tournaments.host_id` and `tournament_judges`). All checks below
// therefore run inside the transaction that is about to read or mutate the resource, so
// callers cannot skip them.

/** Admin, or the single host of the tournament. */
internal fun Transaction.canManageTournament(
    user: User,
    tournamentId: Int,
): Boolean = user.role == UserRole.ADMIN || isTournamentHost(user.id, tournamentId)

/**
 * Admin, host or assigned judge of the tournament: the roles allowed to control
 * matches and view the staff roster.
 */
internal fun Transaction.canJudgeTournament(
    user: User,
    tournamentId: Int,
): Boolean =
    user.role == UserRole.ADMIN ||
        isTournamentHost(user.id, tournamentId) ||
        tournamentJudges.existsByTournamentIdAndUserId(tournamentId, user.id)

internal fun Transaction.isTournamentHost(
    userId: Int,
    tournamentId: Int,
): Boolean = tournaments.findByIdOrNull(tournamentId)?.hostId == userId

/** Admin, host or assigned judge of the tournament owning the bracket. */
internal fun Transaction.canManageBracket(
    user: User,
    bracketId: Int,
): Boolean {
    if (user.role == UserRole.ADMIN) return true

    val tournamentId = brackets.findByIdOrNull(bracketId)?.tournament?.id ?: return false

    return canJudgeTournament(user, tournamentId)
}

/** Admin, host or assigned judge of the tournament owning the match. */
internal fun Transaction.canControlMatch(
    user: User,
    matchId: Int,
): Boolean {
    if (user.role == UserRole.ADMIN) return true

    val match = matches.findByIdOrNull(matchId) ?: return false

    return canManageBracket(user, match.bracket.id)
}

internal fun Transaction.hostedTournamentIds(userId: Int): Set<Int> = tournaments.findByHostId(userId).map { it.id }.toSet()

internal fun Transaction.judgedTournamentIds(userId: Int): Set<Int> = tournamentJudges.findByUserId(userId).map { it.tournamentId }.toSet()

/** Admin sees every tournament; anyone else only the ones they host or judge. */
internal fun Transaction.visibleTournamentIds(user: User): List<Int>? {
    if (user.role == UserRole.ADMIN) return null

    return (hostedTournamentIds(user.id) + judgedTournamentIds(user.id)).sorted()
}
