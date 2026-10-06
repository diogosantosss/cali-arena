package com.caliarena.service

import com.caliarena.domain.tournament.MyTournaments
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentStaff
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.tournament.TournamentJudgeEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * Manages the staff of a tournament: the single host (`tournaments.host_id`) and
 * the judges (`tournament_judges`).
 *
 * Admins may assign the host; hosts and admins may change the judges. Assigned
 * judges may read the roster.
 */
@Service
class TournamentStaffService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun getStaff(
        user: User,
        tournamentId: Int,
    ): Either<ApiError, TournamentStaff> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (!canJudgeTournament(user, tournamentId)) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            success(readStaff(tournamentId))
        }

    fun assignHost(
        user: User,
        tournamentId: Int,
        hostId: Int,
    ): Either<ApiError, TournamentStaff> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(tournamentId)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (user.role != UserRole.ADMIN) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            val host =
                users.findByIdOrNull(hostId)
                    ?: return@run failure(ApiError.USER_NOT_FOUND)

            // The global HOST role is what unlocks the host-only endpoints, so refuse to
            // delegate a tournament to somebody who would not be able to use them.
            if (host.role != UserRole.HOST) {
                return@run failure(ApiError.INVALID_ROLE)
            }

            tournament.hostId = host.id

            success(readStaff(tournaments.save(tournament).id))
        }

    fun addJudge(
        user: User,
        tournamentId: Int,
        judgeId: Int,
    ): Either<ApiError, TournamentStaff> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (!canManageTournament(user, tournamentId)) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            users.findByIdOrNull(judgeId)
                ?: return@run failure(ApiError.USER_NOT_FOUND)

            if (tournamentJudges.existsByTournamentIdAndUserId(tournamentId, judgeId)) {
                return@run failure(ApiError.JUDGE_ALREADY_ASSIGNED)
            }

            tournamentJudges.save(
                TournamentJudgeEntity(
                    tournamentId = tournamentId,
                    userId = judgeId,
                    createdAt = clock.instant().epochSecond,
                ),
            )

            success(readStaff(tournamentId))
        }

    fun removeJudge(
        user: User,
        tournamentId: Int,
        judgeId: Int,
    ): Either<ApiError, Unit> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (!canManageTournament(user, tournamentId)) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            if (!tournamentJudges.existsByTournamentIdAndUserId(tournamentId, judgeId)) {
                return@run failure(ApiError.JUDGE_NOT_ASSIGNED)
            }

            tournamentJudges.deleteByTournamentIdAndUserId(tournamentId, judgeId)

            success(Unit)
        }

    fun getMyTournaments(user: User): Either<ApiError, MyTournaments> =
        trx.run {
            val hostedIds = hostedTournamentIds(user.id)
            val judgedIds = judgedTournamentIds(user.id) - hostedIds

            success(
                MyTournaments(
                    hosting = toTournaments(hostedIds),
                    judging = toTournaments(judgedIds),
                ),
            )
        }

    private fun Transaction.readStaff(tournamentId: Int): TournamentStaff {
        val host =
            tournaments.findByIdOrNull(tournamentId)?.hostId?.let { users.findByIdOrNull(it) }

        val judges =
            tournamentJudges
                .findByTournamentIdOrderByCreatedAt(tournamentId)
                .mapNotNull { assignment -> users.findByIdOrNull(assignment.userId) }
                .map { it.toStaffMember() }

        return TournamentStaff(
            tournamentId = tournamentId,
            host = host?.toStaffMember(),
            judges = judges,
        )
    }

    private fun toTournaments(ids: Set<Int>): List<Tournament> =
        trx.run {
            if (ids.isEmpty()) {
                emptyList()
            } else {
                tournaments.findAllById(ids).map { it.toDomain() }
            }
        }
}
