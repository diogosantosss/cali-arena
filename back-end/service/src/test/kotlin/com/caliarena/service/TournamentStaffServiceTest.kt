package com.caliarena.service

import com.caliarena.domain.tournament.MyTournaments
import com.caliarena.domain.tournament.StaffMember
import com.caliarena.domain.tournament.TournamentStaff
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentJudgeEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class TournamentStaffServiceTest : ServiceTest() {
    private lateinit var service: TournamentStaffService

    @BeforeEach
    fun setup() {
        stubTransactionRepositories()

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = TournamentStaffService(trxManager, clock)
    }

    private val now = clock.instant()

    private fun tournamentEntity(
        id: Int = 1,
        hostId: Int? = null,
    ) = TournamentEntity(id = id, name = "Tournament $id", hostId = hostId, createdAt = now.epochSecond)

    private fun userEntity(
        id: Int,
        username: String,
        role: UserRole = UserRole.JUDGE,
    ) = UserEntity(id = id, username = username, password = "hash", role = role, createdAt = now.epochSecond)

    private fun staffMember(
        id: Int,
        username: String,
        role: UserRole = UserRole.JUDGE,
    ) = StaffMember(id = id, username = username, role = role)

    private fun judgeAssignment(
        tournamentId: Int,
        userId: Int,
    ) = TournamentJudgeEntity(tournamentId = tournamentId, userId = userId, createdAt = now.epochSecond)

    private fun stubTournament(tournament: TournamentEntity) {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        lenient().whenever(tournaments.save(any<TournamentEntity>())).thenReturn(tournament)
    }

    @Nested
    inner class GetStaff {
        @Test
        fun `should return host and judges for an admin`() {
            stubTournament(tournamentEntity(hostId = 2))
            whenever(tournamentJudges.findByTournamentIdOrderByCreatedAt(1))
                .thenReturn(listOf(judgeAssignment(1, 3)))
            whenever(users.findById(2)).thenReturn(Optional.of(userEntity(2, "host", UserRole.HOST)))
            whenever(users.findById(3)).thenReturn(Optional.of(userEntity(3, "judge")))

            val result = service.getStaff(adminUser, 1)

            assertEquals(
                success(
                    TournamentStaff(
                        tournamentId = 1,
                        host = staffMember(2, "host", UserRole.HOST),
                        judges = listOf(staffMember(3, "judge")),
                    ),
                ),
                result,
            )
        }

        @Test
        fun `should allow an assigned judge to read the roster`() {
            stubTournament(tournamentEntity(hostId = 2))
            whenever(tournamentJudges.existsByTournamentIdAndUserId(1, judgeUser.id)).thenReturn(true)
            whenever(tournamentJudges.findByTournamentIdOrderByCreatedAt(1)).thenReturn(emptyList())
            whenever(users.findById(2)).thenReturn(Optional.of(userEntity(2, "host", UserRole.HOST)))

            val result = service.getStaff(judgeUser, 1)

            assertEquals(
                success(TournamentStaff(tournamentId = 1, host = staffMember(2, "host", UserRole.HOST), judges = emptyList())),
                result,
            )
        }

        @Test
        fun `should reject a user outside the tournament`() {
            stubTournament(tournamentEntity(hostId = 2))
            whenever(tournamentJudges.existsByTournamentIdAndUserId(1, judgeUser.id)).thenReturn(false)

            val result = service.getStaff(judgeUser, 1)

            assertEquals(failure(ApiError.NOT_AUTHORIZED), result)
        }

        @Test
        fun `should fail when tournament does not exist`() {
            whenever(tournaments.findById(99)).thenReturn(Optional.empty())

            val result = service.getStaff(adminUser, 99)

            assertEquals(failure(ApiError.TOURNAMENT_NOT_FOUND), result)
        }
    }

    @Nested
    inner class AssignHost {
        @Test
        fun `should let the admin assign the host`() {
            val tournament = tournamentEntity()
            stubTournament(tournament)
            whenever(users.findById(7)).thenReturn(Optional.of(userEntity(7, "new-host", UserRole.HOST)))
            whenever(tournamentJudges.findByTournamentIdOrderByCreatedAt(1)).thenReturn(emptyList())

            val result = service.assignHost(adminUser, 1, 7)

            assertEquals(
                success(TournamentStaff(tournamentId = 1, host = staffMember(7, "new-host", UserRole.HOST), judges = emptyList())),
                result,
            )
            assertEquals(7, tournament.hostId)
        }

        @Test
        fun `should reject the current host handing over to another user`() {
            val tournament = tournamentEntity(hostId = hostUser.id)
            stubTournament(tournament)

            val result = service.assignHost(hostUser, 1, 7)

            assertEquals(failure(ApiError.NOT_AUTHORIZED), result)
            verify(tournaments, never()).save(any())
        }

        @Test
        fun `should reject a user that is not admin nor host`() {
            val tournament = tournamentEntity(hostId = 2)
            stubTournament(tournament)

            val result = service.assignHost(judgeUser, 1, 7)

            assertEquals(failure(ApiError.NOT_AUTHORIZED), result)
            verify(tournaments, never()).save(any())
        }

        @Test
        fun `should fail when the new host does not exist`() {
            val tournament = tournamentEntity(hostId = 2)
            stubTournament(tournament)
            whenever(users.findById(7)).thenReturn(Optional.empty())

            val result = service.assignHost(adminUser, 1, 7)

            assertEquals(failure(ApiError.USER_NOT_FOUND), result)
        }

        @Test
        fun `should refuse a user without the host role`() {
            val tournament = tournamentEntity(hostId = 2)
            stubTournament(tournament)
            whenever(users.findById(7)).thenReturn(Optional.of(userEntity(7, "judge")))

            val result = service.assignHost(adminUser, 1, 7)

            assertEquals(failure(ApiError.INVALID_ROLE), result)
            verify(tournaments, never()).save(any<TournamentEntity>())
        }
    }

    @Nested
    inner class Judges {
        @Test
        fun `should let the host assign a judge`() {
            stubTournament(tournamentEntity(hostId = hostUser.id))
            whenever(users.findById(3)).thenReturn(Optional.of(userEntity(3, "judge")))
            whenever(tournamentJudges.findByTournamentIdOrderByCreatedAt(1))
                .thenReturn(listOf(judgeAssignment(1, 3)))

            val result = service.addJudge(hostUser, 1, 3)

            assertEquals(
                success(TournamentStaff(tournamentId = 1, host = null, judges = listOf(staffMember(3, "judge")))),
                result,
            )
            verify(tournamentJudges).save(any())
        }

        @Test
        fun `should reject an already assigned judge`() {
            stubTournament(tournamentEntity(hostId = hostUser.id))
            whenever(users.findById(3)).thenReturn(Optional.of(userEntity(3, "judge")))
            whenever(tournamentJudges.existsByTournamentIdAndUserId(1, 3)).thenReturn(true)

            val result = service.addJudge(adminUser, 1, 3)

            assertEquals(failure(ApiError.JUDGE_ALREADY_ASSIGNED), result)
            verify(tournamentJudges, never()).save(any())
        }

        @Test
        fun `should reject an assigned judge trying to add other judges`() {
            stubTournament(tournamentEntity(hostId = 2))

            val result = service.addJudge(judgeUser, 1, 3)

            assertEquals(failure(ApiError.NOT_AUTHORIZED), result)
            verify(tournamentJudges, never()).save(any())
        }

        @Test
        fun `should let the host remove a judge`() {
            stubTournament(tournamentEntity(hostId = hostUser.id))
            whenever(tournamentJudges.existsByTournamentIdAndUserId(1, 3)).thenReturn(true)

            val result = service.removeJudge(hostUser, 1, 3)

            assertEquals(success(Unit), result)
            verify(tournamentJudges).deleteByTournamentIdAndUserId(1, 3)
        }

        @Test
        fun `should fail removing a judge that is not assigned`() {
            stubTournament(tournamentEntity(hostId = hostUser.id))
            whenever(tournamentJudges.existsByTournamentIdAndUserId(1, 3)).thenReturn(false)

            val result = service.removeJudge(hostUser, 1, 3)

            assertEquals(failure(ApiError.JUDGE_NOT_ASSIGNED), result)
            verify(tournamentJudges, never()).deleteByTournamentIdAndUserId(any(), any())
        }
    }

    @Nested
    inner class GetMyTournaments {
        @Test
        fun `should split hosting and judging tournaments`() {
            val hosted = tournamentEntity(1, hostId = hostUser.id)
            val judged = tournamentEntity(2)
            whenever(tournaments.findByHostId(hostUser.id)).thenReturn(listOf(hosted))
            whenever(tournamentJudges.findByUserId(hostUser.id)).thenReturn(listOf(judgeAssignment(2, hostUser.id)))
            whenever(tournaments.findAllById(setOf(1))).thenReturn(listOf(hosted))
            whenever(tournaments.findAllById(setOf(2))).thenReturn(listOf(judged))

            val result = service.getMyTournaments(hostUser)

            assertEquals(
                success(MyTournaments(hosting = listOf(hosted.toDomain()), judging = listOf(judged.toDomain()))),
                result,
            )
        }

        @Test
        fun `should not repeat a hosted tournament as judged`() {
            val hosted = tournamentEntity(1, hostId = hostUser.id)
            whenever(tournaments.findByHostId(hostUser.id)).thenReturn(listOf(hosted))
            whenever(tournamentJudges.findByUserId(hostUser.id)).thenReturn(listOf(judgeAssignment(1, hostUser.id)))
            whenever(tournaments.findAllById(setOf(1))).thenReturn(listOf(hosted))

            val result = service.getMyTournaments(hostUser)

            assertEquals(
                success(MyTournaments(hosting = listOf(hosted.toDomain()), judging = emptyList())),
                result,
            )
        }

        @Test
        fun `should return empty lists for a user without assignments`() {
            whenever(tournaments.findByHostId(judgeUser.id)).thenReturn(emptyList())
            whenever(tournamentJudges.findByUserId(judgeUser.id)).thenReturn(emptyList())

            val result = service.getMyTournaments(judgeUser)

            assertEquals(success(MyTournaments(hosting = emptyList(), judging = emptyList())), result)
        }
    }
}
