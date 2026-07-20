package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.club.Club
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AthleteServiceTest : ServiceTest() {
    private lateinit var service: AthleteService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoAthlete).thenReturn(repoAthlete)
        lenient().whenever(transaction.repoClub).thenReturn(repoClub)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = AthleteService(trxManager, clock)
    }

    @Nested
    inner class CreateAthlete {
        private val now = clock.instant()

        private val club = Club(id = 1, name = "Club A", shortName = "CA", createdAt = now)

        private val athlete =
            Athlete(
                id = 1,
                name = "João Silva",
                gender = GenderType.MALE,
                clubId = club.id,
                createdAt = now,
            )

        @Test
        fun `should create athlete successfully`() {
            whenever(repoClub.findById(1)).thenReturn(club)

            whenever(
                repoAthlete.createAthlete(
                    name = "João Silva",
                    gender = GenderType.MALE,
                    clubId = 1,
                    createdAt = now,
                ),
            ).thenReturn(athlete)

            val result = service.createAthlete("João Silva", "MALE", 1)

            assertEquals(success(athlete), result)

            verify(repoClub).findById(1)
            verify(repoAthlete).createAthlete(
                name = "João Silva",
                gender = GenderType.MALE,
                clubId = 1,
                createdAt = now,
            )
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(repoClub.findById(1)).thenReturn(null)

            val result = service.createAthlete("João Silva", "MALE", 1)

            assertEquals(failure(AthleteError.ClubNotFound), result)

            verify(repoAthlete, never()).createAthlete(any(), any(), any(), any())
        }

        @Test
        fun `should fail when gender is invalid`() {
            whenever(repoClub.findById(1)).thenReturn(club)

            val result = service.createAthlete("João Silva", "INVALID_GENDER", 1)

            assertEquals(failure(AthleteError.InvalidGender), result)

            verify(repoAthlete, never()).createAthlete(any(), any(), any(), any())
        }

        @Test
        fun `should fail when repository fails to create athlete`() {
            whenever(repoClub.findById(1)).thenReturn(club)

            whenever(
                repoAthlete.createAthlete(
                    name = "João Silva",
                    gender = GenderType.MALE,
                    clubId = 1,
                    createdAt = now,
                ),
            ).thenReturn(null)

            val result = service.createAthlete("João Silva", "MALE", 1)

            assertEquals(failure(AthleteError.CreatingAthlete), result)
        }
    }

    @Nested
    inner class GetAthleteById {
        private val now = clock.instant()

        private val athlete =
            Athlete(
                id = 1,
                name = "João Silva",
                gender = GenderType.MALE,
                clubId = 1,
                createdAt = now,
            )

        @Test
        fun `should return athlete when found`() {
            whenever(repoAthlete.findById(1)).thenReturn(athlete)

            val result = service.getAthleteById(1)

            assertEquals(success(athlete), result)

            verify(repoAthlete).findById(1)
        }

        @Test
        fun `should fail when athlete does not exist`() {
            whenever(repoAthlete.findById(1)).thenReturn(null)

            val result = service.getAthleteById(1)

            assertEquals(failure(AthleteError.AthleteNotFound), result)
        }
    }

    @Nested
    inner class GetAllAthletes {
        private val now = clock.instant()

        private val athletes =
            listOf(
                Athlete(id = 1, name = "João Silva", gender = GenderType.MALE, clubId = 1, createdAt = now),
                Athlete(id = 2, name = "Ana Costa", gender = GenderType.FEMALE, clubId = 1, createdAt = now),
            )

        @Test
        fun `should return all athletes`() {
            whenever(repoAthlete.findAll()).thenReturn(athletes)

            val result = service.getAllAthletes()

            assertEquals(athletes, result)

            verify(repoAthlete).findAll()
        }

        @Test
        fun `should return empty list when no athletes exist`() {
            whenever(repoAthlete.findAll()).thenReturn(emptyList())

            val result = service.getAllAthletes()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class GetAthletesByClub {
        private val now = clock.instant()

        private val club = Club(id = 1, name = "Club A", shortName = "CA", createdAt = now)

        private val athletes =
            listOf(
                Athlete(id = 1, name = "João Silva", gender = GenderType.MALE, clubId = 1, createdAt = now),
                Athlete(id = 2, name = "Ana Costa", gender = GenderType.FEMALE, clubId = 1, createdAt = now),
            )

        @Test
        fun `should return athletes for existing club`() {
            whenever(repoClub.findById(1)).thenReturn(club)
            whenever(repoAthlete.findByClubId(1)).thenReturn(athletes)

            val result = service.getAthletesByClub(1)

            assertEquals(success(athletes), result)

            verify(repoClub).findById(1)
            verify(repoAthlete).findByClubId(1)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(repoClub.findById(1)).thenReturn(null)

            val result = service.getAthletesByClub(1)

            assertEquals(failure(AthleteError.ClubNotFound), result)

            verify(repoAthlete, never()).findByClubId(any())
        }

        @Test
        fun `should return empty list when club has no athletes`() {
            whenever(repoClub.findById(1)).thenReturn(club)
            whenever(repoAthlete.findByClubId(1)).thenReturn(emptyList())

            val result = service.getAthletesByClub(1)

            assertEquals(success(emptyList<Athlete>()), result)
        }
    }

    @Nested
    inner class GetAthletesByGender {
        private val now = clock.instant()

        private val maleAthletes =
            listOf(
                Athlete(id = 1, name = "João Silva", gender = GenderType.MALE, clubId = 1, createdAt = now),
                Athlete(id = 3, name = "Carlos Ramos", gender = GenderType.MALE, clubId = 2, createdAt = now),
            )

        @Test
        fun `should return athletes filtered by gender`() {
            whenever(repoAthlete.findByGender(GenderType.MALE)).thenReturn(maleAthletes)

            val result = service.getAthletesByGender(GenderType.MALE)

            assertEquals(maleAthletes, result)

            verify(repoAthlete).findByGender(GenderType.MALE)
        }

        @Test
        fun `should return empty list when no athletes match gender`() {
            whenever(repoAthlete.findByGender(GenderType.FEMALE)).thenReturn(emptyList())

            val result = service.getAthletesByGender(GenderType.FEMALE)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class UpdateAthlete {
        private val now = clock.instant()

        private val club = Club(id = 1, name = "Club A", shortName = "CA", createdAt = now)
        private val newClub = Club(id = 2, name = "Club B", shortName = "CB", createdAt = now)

        private val existing =
            Athlete(
                id = 1,
                name = "João Silva",
                gender = GenderType.MALE,
                clubId = 1,
                createdAt = now,
            )

        private val updated =
            existing.copy(name = "João Atualizado", gender = GenderType.FEMALE, clubId = 2)

        @Test
        fun `should update athlete successfully`() {
            whenever(repoAthlete.findById(1)).thenReturn(existing)
            whenever(repoClub.findById(2)).thenReturn(newClub)
            whenever(repoAthlete.save(updated)).thenReturn(updated)

            val result = service.updateAthlete(1, "João Atualizado", GenderType.FEMALE, 2)

            assertEquals(success(updated), result)

            verify(repoAthlete).findById(1)
            verify(repoClub).findById(2)
            verify(repoAthlete).save(updated)
        }

        @Test
        fun `should fail when athlete does not exist`() {
            whenever(repoAthlete.findById(1)).thenReturn(null)

            val result = service.updateAthlete(1, "João Atualizado", GenderType.MALE, 1)

            assertEquals(failure(AthleteError.AthleteNotFound), result)

            verify(repoClub, never()).findById(any())
            verify(repoAthlete, never()).save(any())
        }

        @Test
        fun `should fail when new club does not exist`() {
            whenever(repoAthlete.findById(1)).thenReturn(existing)
            whenever(repoClub.findById(99)).thenReturn(null)

            val result = service.updateAthlete(1, "João Atualizado", GenderType.MALE, 99)

            assertEquals(failure(AthleteError.ClubNotFound), result)

            verify(repoAthlete, never()).save(any())
        }

        @Test
        fun `should fail when repository fails to save updated athlete`() {
            whenever(repoAthlete.findById(1)).thenReturn(existing)
            whenever(repoClub.findById(1)).thenReturn(club)
            whenever(repoAthlete.save(any())).thenReturn(null)

            val result = service.updateAthlete(1, "João Atualizado", GenderType.MALE, 1)

            assertEquals(failure(AthleteError.UpdatingAthlete), result)
        }
    }
}
