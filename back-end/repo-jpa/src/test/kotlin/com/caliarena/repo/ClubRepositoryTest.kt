package com.caliarena.repo

import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManagerJpa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class ClubRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            matchProgresses.deleteAll()
            matches.deleteAll()
            screenRoutines.deleteAll()
            tournamentStates.deleteAll()
            brackets.deleteAll()
            tournaments.deleteAll()
            tokens.deleteAll()
            users.deleteAll()
            athletes.deleteAll()
            clubs.deleteAll()
        }
    }

    private fun now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newClub(
        name: String = "club-${System.nanoTime()}",
        shortName: String? = null,
    ): ClubEntity = clubs.save(ClubEntity(name = name, shortName = shortName, createdAt = now().epochSecond))

    @Nested
    inner class Create {
        @Test
        fun `should create a club with the given fields`() =
            trx.run {
                val created = newClub(name = "Sporting", shortName = "SCP")

                assertNotEquals(0, created.id)
                assertEquals("Sporting", created.name)
                assertEquals("SCP", created.shortName)
            }

        @Test
        fun `should create a club without short name`() =
            trx.run {
                val created = newClub()

                assertNull(created.shortName)
            }
    }

    @Nested
    inner class FindByName {
        @Test
        fun `should find an existing club by name`() =
            trx.run {
                newClub(name = "Benfica")

                val found = clubs.findByName("Benfica")

                assertNotNull(found)
                assertEquals("Benfica", found?.name)
            }

        @Test
        fun `should return null when name does not exist`() =
            trx.run {
                assertNull(clubs.findByName("does-not-exist"))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing club by id`() =
            trx.run {
                val created = newClub()

                assertNotNull(clubs.findByIdOrNull(created.id))
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(clubs.findByIdOrNull(-1))
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing club`() =
            trx.run {
                val created = newClub(name = "old")

                created.name = "updated"
                val saved = clubs.save(created)

                assertEquals("updated", saved.name)
                assertEquals(created.id, saved.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the club`() =
            trx.run {
                val created = newClub()

                clubs.deleteById(created.id)

                assertNull(clubs.findByIdOrNull(created.id))
            }
    }
}
