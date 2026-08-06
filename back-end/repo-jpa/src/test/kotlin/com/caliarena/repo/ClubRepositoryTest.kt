package com.caliarena.repo

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

@SpringBootTest(classes = [TestConfig::class])
class ClubRepositoryTest {
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
    inner class CreateClub {
        @Test
        fun `should create a club with the given fields`() =
            trx.run {
                val club =
                    repoClub.createClub(
                        name = "alpha",
                        shortName = "A",
                        createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    )

                assertNotEquals(0, club.id)
                assertEquals("alpha", club.name)
                assertEquals("A", club.shortName)
            }
    }

    @Nested
    inner class FindByName {
        @Test
        fun `should find an existing club by name`() =
            trx.run {
                repoClub.createClub("bravo", "B", Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val found = repoClub.findByName("bravo")

                assertNotNull(found)
                assertEquals("bravo", found?.name)
            }

        @Test
        fun `should return null when name does not exist`() =
            trx.run {
                assertNull(repoClub.findByName("missing"))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing club by id`() =
            trx.run {
                val created = repoClub.createClub("charlie", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val found = repoClub.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoClub.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created clubs`() =
            trx.run {
                repoClub.createClub("c1", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoClub.createClub("c2", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                val clubs = repoClub.findAll()

                assertEquals(2, clubs.size)
            }

        @Test
        fun `should return empty list when there are no clubs`() =
            trx.run {
                assertTrue(repoClub.findAll().isEmpty())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing club`() =
            trx.run {
                val created = repoClub.createClub("delta", "D", Instant.now().truncatedTo(ChronoUnit.SECONDS))
                val updated = created.copy(name = "delta-updated")

                val saved = repoClub.save(updated)

                assertNotNull(saved)
                assertEquals("delta-updated", saved?.name)
                assertEquals(created.id, saved?.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the club`() =
            trx.run {
                val created = repoClub.createClub("echo", "E", Instant.now().truncatedTo(ChronoUnit.SECONDS))

                repoClub.deleteById(created.id)

                assertNull(repoClub.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all clubs`() =
            trx.run {
                repoClub.createClub("f1", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))
                repoClub.createClub("f2", null, Instant.now().truncatedTo(ChronoUnit.SECONDS))

                repoClub.clear()

                assertTrue(repoClub.findAll().isEmpty())
            }
    }
}
