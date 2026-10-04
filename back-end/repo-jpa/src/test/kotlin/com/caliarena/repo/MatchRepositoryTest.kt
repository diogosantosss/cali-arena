package com.caliarena.repo

import com.caliarena.domain.match.MatchStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class MatchRepositoryTest : AbstractRepositoryTest() {
    @Nested
    inner class Matches {
        @Test
        fun `should create and find a match by id`() =
            trx.run {
                val created = newMatch()

                val found = matches.findByIdOrNull(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
                assertEquals(MatchStatus.RUNNING, found?.status)
                assertEquals(created.athleteRed?.id, found?.athleteRed?.id)
            }

        @Test
        fun `should find matches by bracket`() =
            trx.run {
                val matchA = newMatch()
                newMatch()

                val found = matches.findByBracketId(matchA.bracket.id)

                assertEquals(1, found.size)
                assertEquals(matchA.id, found.first().id)
            }

        @Test
        fun `should find matches by status`() =
            trx.run {
                val running = newMatch(MatchStatus.RUNNING)
                newMatch(MatchStatus.PENDING)

                val found = matches.findByStatus(MatchStatus.RUNNING)

                assertTrue(found.any { it.id == running.id })
                assertTrue(found.all { it.status == MatchStatus.RUNNING })
            }

        @Test
        fun `should update a match`() =
            trx.run {
                val created = newMatch()

                created.status = MatchStatus.FINISHED
                matches.save(created)

                assertEquals(MatchStatus.FINISHED, matches.findByIdOrNull(created.id)?.status)
            }

        @Test
        fun `should delete a match`() =
            trx.run {
                val created = newMatch()

                matches.deleteById(created.id)

                assertNull(matches.findByIdOrNull(created.id))
            }
    }

    @Nested
    inner class MatchProgress {
        @Test
        fun `should create progress for a match`() =
            trx.run {
                val match = newMatch()

                val progress = newProgress(match)

                assertNotEquals(0, progress.id)
                assertEquals(match.id, progress.match.id)
            }

        @Test
        fun `should find progress by match id`() =
            trx.run {
                val match = newMatch()
                val progress = newProgress(match)

                val found = matchProgresses.findByMatchId(match.id)

                assertNotNull(found)
                assertEquals(progress.id, found?.id)
            }

        @Test
        fun `should return null when there is no progress for the match`() =
            trx.run {
                val match = newMatch()

                assertNull(matchProgresses.findByMatchId(match.id))
            }

        @Test
        fun `should update reps in place`() =
            trx.run {
                val match = newMatch()
                val progress = newProgress(match)

                progress.redCurrentReps = 7
                matchProgresses.save(progress)

                assertEquals(7, matchProgresses.findByMatchId(match.id)?.redCurrentReps)
            }

        @Test
        fun `should delete all progress rows`() =
            trx.run {
                newProgress(newMatch())
                newProgress(newMatch())

                matchProgresses.deleteAll()

                assertEquals(0, matchProgresses.count())
            }
    }
}
