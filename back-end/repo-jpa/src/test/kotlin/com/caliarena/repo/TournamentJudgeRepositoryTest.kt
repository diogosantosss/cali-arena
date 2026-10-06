package com.caliarena.repo

import com.caliarena.repo.entities.tournament.TournamentJudgeId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class TournamentJudgeRepositoryTest : AbstractRepositoryTest() {
    @Nested
    inner class Assignment {
        @Test
        fun `should assign a judge to a tournament`() =
            trx.run {
                val tournament = newTournament()
                val judge = newUser()

                val assigned = assignJudge(tournament, judge)

                assertEquals(tournament.id, assigned.tournamentId)
                assertEquals(judge.id, assigned.userId)
                assertTrue(tournamentJudges.existsByTournamentIdAndUserId(tournament.id, judge.id))
            }

        @Test
        fun `should reject a duplicate assignment on the same tournament`() =
            trx.run {
                val tournament = newTournament()
                val judge = newUser()
                assignJudge(tournament, judge)
                // forces the pending insert to be flushed before hitting the table directly
                assertEquals(1, tournamentJudges.findByTournamentId(tournament.id).size)

                val second =
                    runCatching {
                        jdbc.update(
                            "INSERT INTO tournament_judges (tournament_id, user_id, created_at) VALUES (?, ?, ?)",
                            tournament.id,
                            judge.id,
                            now().epochSecond,
                        )
                    }

                assertTrue(second.isFailure)
            }

        @Test
        fun `should not duplicate rows when saving an existing assignment`() =
            trx.run {
                // CrudRepository.save() merges once the composite id is set, so the service
                // must check existsByTournamentIdAndUserId before assigning.
                val tournament = newTournament()
                val judge = newUser()
                assignJudge(tournament, judge)
                assertEquals(1, tournamentJudges.findByTournamentId(tournament.id).size)

                assignJudge(tournament, judge)
                assertEquals(1, tournamentJudges.findByTournamentId(tournament.id).size)
            }

        @Test
        fun `should list judges of a tournament`() =
            trx.run {
                val tournament = newTournament()
                val other = newTournament()
                val judgeA = newUser()
                val judgeB = newUser()
                assignJudge(tournament, judgeA)
                assignJudge(tournament, judgeB)
                assignJudge(other, newUser())

                val found = tournamentJudges.findByTournamentId(tournament.id)

                assertEquals(2, found.size)
                assertTrue(found.all { it.tournamentId == tournament.id })
            }

        @Test
        fun `should list tournaments a user judges`() =
            trx.run {
                val user = newUser()
                val tournamentA = newTournament()
                val tournamentB = newTournament()
                assignJudge(tournamentA, user)
                assignJudge(tournamentB, user)

                val found = tournamentJudges.findByUserId(user.id)

                assertEquals(2, found.size)
                assertTrue(found.all { it.userId == user.id })
            }

        @Test
        fun `should list judges across several tournaments`() =
            trx.run {
                val tournamentA = newTournament()
                val tournamentB = newTournament()
                assignJudge(tournamentA, newUser())
                assignJudge(tournamentB, newUser())

                val found = tournamentJudges.findByTournamentIdIn(listOf(tournamentA.id, tournamentB.id))

                assertEquals(2, found.size)
            }

        @Test
        fun `should remove a judge from a tournament`() =
            trx.run {
                val tournament = newTournament()
                val judge = newUser()
                assignJudge(tournament, judge)

                tournamentJudges.deleteByTournamentIdAndUserId(tournament.id, judge.id)

                assertFalse(tournamentJudges.existsByTournamentIdAndUserId(tournament.id, judge.id))
            }

        @Test
        fun `should find the assignment by composite id`() =
            trx.run {
                val tournament = newTournament()
                val judge = newUser()
                val assigned = assignJudge(tournament, judge)

                val found = tournamentJudges.findByIdOrNull(TournamentJudgeId(tournament.id, judge.id))

                assertEquals(assigned.userId, found?.userId)
            }
    }

    @Nested
    inner class Host {
        @Test
        fun `should find tournaments hosted by a user`() =
            trx.run {
                val host = newUser()
                val hosted = newTournament(host = host)
                newTournament(host = newUser())
                newTournament()

                val found = tournaments.findByHostId(host.id)

                assertEquals(1, found.size)
                assertEquals(hosted.id, found.first().id)
            }

        @Test
        fun `should move the host of a tournament`() =
            trx.run {
                val tournament = newTournament()
                val newHost = newUser()

                tournament.hostId = newHost.id
                tournaments.save(tournament)

                assertEquals(newHost.id, tournaments.findByIdOrNull(tournament.id)?.hostId)
            }
    }
}
