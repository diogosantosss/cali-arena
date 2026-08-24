package com.caliarena.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManagerJpa
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
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [TestConfig::class])
class AthleteRepositoryTest {
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

    private fun Transaction.newClub(name: String = "club-${System.nanoTime()}"): ClubEntity =
        clubs.save(ClubEntity(name = name, createdAt = now().epochSecond))

    private fun Transaction.newAthlete(
        name: String = "athlete-${System.nanoTime()}",
        gender: GenderType = GenderType.MALE,
        club: ClubEntity = newClub(),
    ): AthleteEntity = athletes.save(AthleteEntity(name = name, gender = gender, club = club, createdAt = now().epochSecond))

    @Nested
    inner class Create {
        @Test
        fun `should create an athlete with the given fields`() =
            trx.run {
                val club = newClub()
                val created = newAthlete(name = "Diogo", gender = GenderType.MALE, club = club)

                assertNotEqualsZero(created.id)
                assertEquals("Diogo", created.name)
                assertEquals(GenderType.MALE, created.gender)
                assertEquals(club.id, created.club.id)
            }

        @Test
        fun `should persist the athlete so it can be found by id`() =
            trx.run {
                val created = newAthlete()

                val found = athletes.findByIdOrNull(created.id)

                assertNotNull(found)
                assertEquals(created.name, found?.name)
            }
    }

    @Nested
    inner class FindByClubId {
        @Test
        fun `should return only the athletes of the given club`() =
            trx.run {
                val clubA = newClub()
                val clubB = newClub()
                val inA = newAthlete(club = clubA)
                newAthlete(club = clubB)

                val found = athletes.findByClubId(clubA.id)

                assertEquals(1, found.size)
                assertEquals(inA.id, found.first().id)
            }

        @Test
        fun `should return empty list when club has no athletes`() =
            trx.run {
                val empty = newClub()

                assertTrue(athletes.findByClubId(empty.id).isEmpty())
            }
    }

    @Nested
    inner class FindByGender {
        @Test
        fun `should return only athletes with the given gender`() =
            trx.run {
                newAthlete(name = "male-1", gender = GenderType.MALE)
                newAthlete(name = "female-1", gender = GenderType.FEMALE)

                val males = athletes.findByGender(GenderType.MALE)

                assertTrue(males.all { it.gender == GenderType.MALE })
                assertTrue(males.any { it.name == "male-1" })
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(athletes.findByIdOrNull(-1))
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing athlete`() =
            trx.run {
                val created = newAthlete(name = "old-name")

                created.name = "new-name"
                val saved = athletes.save(created)

                assertEquals("new-name", saved.name)
                assertEquals(created.id, saved.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the athlete`() =
            trx.run {
                val created = newAthlete()

                athletes.deleteById(created.id)

                assertNull(athletes.findByIdOrNull(created.id))
            }
    }

    private fun assertNotEqualsZero(id: Int) {
        assertNotEquals(0, id)
    }
}
