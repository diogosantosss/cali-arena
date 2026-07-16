package com.caliarena.repo

import com.caliarena.RepositoryAthlete
import com.caliarena.RepositoryClub
import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.jpa.TransactionManagerJpa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfiguration::class])
class AthleteRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            repoAthlete.clear()
            repoClub.clear()
        }
    }

    @Nested
    inner class CreateAthlete {
        @Test
        fun `should create an athlete for an existing club`() =
            trx.run {
                val club = newClub(repoClub, "crossfit")

                val athlete =
                    repoAthlete.createAthlete(
                        name = "alice",
                        gender = GenderType.FEMALE,
                        clubId = club.id,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotNull(athlete)
                assertNotEquals(0, athlete?.id)
                assertEquals("alice", athlete?.name)
                assertEquals(GenderType.FEMALE, athlete?.gender)
                assertEquals(club.id, athlete?.clubId)
            }

        @Test
        fun `should return null when club does not exist`() =
            trx.run {
                val athlete =
                    repoAthlete.createAthlete(
                        name = "ghost",
                        gender = GenderType.MALE,
                        clubId = -1,
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNull(athlete)
            }
    }

    @Nested
    inner class FindByClubId {
        @Test
        fun `should return athletes for a club`() =
            trx.run {
                val club = newClub(repoClub, "club-a")
                newAthlete(repoAthlete, club.id, "a1", GenderType.MALE)
                newAthlete(repoAthlete, club.id, "a2", GenderType.FEMALE)

                val athletes = repoAthlete.findByClubId(club.id)

                assertEquals(2, athletes.size)
            }

        @Test
        fun `should return empty list when club has no athletes`() =
            trx.run {
                val club = newClub(repoClub, "club-b")

                val athletes = repoAthlete.findByClubId(club.id)

                assertTrue(athletes.isEmpty())
            }
    }

    @Nested
    inner class FindByGender {
        @Test
        fun `should return athletes with the given gender`() =
            trx.run {
                val club = newClub(repoClub, "club-c")
                newAthlete(repoAthlete, club.id, "m1", GenderType.MALE)
                newAthlete(repoAthlete, club.id, "f1", GenderType.FEMALE)

                val athletes = repoAthlete.findByGender(GenderType.FEMALE)

                assertEquals(1, athletes.size)
                assertEquals("f1", athletes.first().name)
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing athlete by id`() =
            trx.run {
                val club = newClub(repoClub)
                val created = newAthlete(repoAthlete, club.id)

                val found = repoAthlete.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoAthlete.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created athletes`() =
            trx.run {
                val club = newClub(repoClub)
                newAthlete(repoAthlete, club.id, "a1")
                newAthlete(repoAthlete, club.id, "a2")

                val athletes = repoAthlete.findAll()

                assertEquals(2, athletes.size)
            }

        @Test
        fun `should return empty list when there are no athletes`() =
            trx.run {
                assertTrue(repoAthlete.findAll().isEmpty())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing athlete`() =
            trx.run {
                val club = newClub(repoClub, "club-save")
                val created = newAthlete(repoAthlete, club.id, "john")
                val updated = created.copy(name = "john-updated")

                val saved = repoAthlete.save(updated)

                assertNotNull(saved)
                assertEquals("john-updated", saved?.name)
                assertEquals(created.id, saved?.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the athlete`() =
            trx.run {
                val club = newClub(repoClub)
                val created = newAthlete(repoAthlete, club.id)

                repoAthlete.deleteById(created.id)

                assertNull(repoAthlete.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all athletes`() =
            trx.run {
                val club = newClub(repoClub)
                newAthlete(repoAthlete, club.id, "a1")
                newAthlete(repoAthlete, club.id, "a2")

                repoAthlete.clear()

                assertTrue(repoAthlete.findAll().isEmpty())
            }
    }

    private fun newClub(
        repoClub: RepositoryClub,
        name: String = "club-${System.nanoTime()}",
    ) = repoClub.createClub(
        name = name,
        shortName = "c",
        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
    )

    private fun newAthlete(
        repoAthlete: RepositoryAthlete,
        clubId: Int,
        name: String = "athlete-${System.nanoTime()}",
        gender: GenderType = GenderType.MALE,
    ) = repoAthlete.createAthlete(
        name = name,
        gender = gender,
        clubId = clubId,
        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
    )!!
}
