package com.caliarena.service

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.trx.Transaction
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
import java.util.Optional

class AthleteServiceTest : ServiceTest() {
    private lateinit var service: AthleteService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.athletes).thenReturn(athletes)
        lenient().whenever(transaction.clubs).thenReturn(clubs)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = AthleteService(trxManager, clock)
    }

    private val now = clock.instant()

    private fun clubEntity(
        id: Int = 1,
        name: String = "Club A",
    ) = ClubEntity(id, name, null, now.epochSecond)

    private fun athleteEntity(
        id: Int = 1,
        name: String = "João Silva",
        gender: GenderType = GenderType.MALE,
        club: ClubEntity = clubEntity(),
    ) = AthleteEntity(id, name, gender, club, now.epochSecond)

    @Nested
    inner class CreateAthlete {
        @Test
        fun `should create athlete successfully`() {
            val club = clubEntity()
            val created = athleteEntity(name = "João Silva", gender = GenderType.MALE, club = club)

            whenever(clubs.findById(1)).thenReturn(Optional.of(club))
            whenever(athletes.save(any())).thenReturn(created)

            val result = service.createAthlete("João Silva", "MALE", 1)

            assertEquals(success(created.toDomain()), result)

            verify(clubs).findById(1)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(clubs.findById(1)).thenReturn(Optional.empty())

            val result = service.createAthlete("João Silva", "MALE", 1)

            assertEquals(failure(ApiError.CLUB_NOT_FOUND), result)

            verify(athletes, never()).save(any())
        }

        @Test
        fun `should fail when gender is invalid`() {
            whenever(clubs.findById(1)).thenReturn(Optional.of(clubEntity()))

            val result = service.createAthlete("João Silva", "INVALID", 1)

            assertEquals(failure(ApiError.INVALID_GENDER), result)

            verify(athletes, never()).save(any())
        }
    }

    @Nested
    inner class GetAthleteById {
        @Test
        fun `should return athlete when found`() {
            whenever(athletes.findById(1)).thenReturn(Optional.of(athleteEntity()))

            val result = service.getAthleteById(1)

            assertEquals(success(athleteEntity().toDomain()), result)
        }

        @Test
        fun `should fail when athlete does not exist`() {
            whenever(athletes.findById(1)).thenReturn(Optional.empty())

            val result = service.getAthleteById(1)

            assertEquals(failure(ApiError.ATHLETE_NOT_FOUND), result)
        }
    }

    @Nested
    inner class GetAllAthletes {
        @Test
        fun `should return all athletes`() {
            val list =
                listOf(
                    athleteEntity(id = 1),
                    athleteEntity(id = 2, name = "Maria Silva", gender = GenderType.FEMALE),
                )
            whenever(athletes.findAll()).thenReturn(list)

            val result = service.getAllAthletes()

            assertEquals(list.map { it.toDomain() }, result)
        }

        @Test
        fun `should return empty list when no athletes exist`() {
            whenever(athletes.findAll()).thenReturn(emptyList())

            assertTrue(service.getAllAthletes().isEmpty())
        }
    }

    @Nested
    inner class GetAthletesByClub {
        @Test
        fun `should return athletes of the given club`() {
            val club = clubEntity()
            val list = listOf(athleteEntity(club = club))

            whenever(clubs.findById(1)).thenReturn(Optional.of(club))
            whenever(athletes.findByClubId(1)).thenReturn(list)

            val result = service.getAthletesByClub(1)

            assertEquals(success(list.map { it.toDomain() }), result)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(clubs.findById(1)).thenReturn(Optional.empty())

            val result = service.getAthletesByClub(1)

            assertEquals(failure(ApiError.CLUB_NOT_FOUND), result)
        }
    }

    @Nested
    inner class GetAthletesByGender {
        @Test
        fun `should return athletes with the given gender`() {
            val list = listOf(athleteEntity(gender = GenderType.FEMALE))
            whenever(athletes.findByGender(GenderType.FEMALE)).thenReturn(list)

            val result = service.getAthletesByGender("FEMALE")

            assertEquals(success(list.map { it.toDomain() }), result)
        }

        @Test
        fun `should fail when gender is invalid`() {
            val result = service.getAthletesByGender("INVALID")

            assertEquals(failure(ApiError.INVALID_GENDER), result)
        }
    }

    @Nested
    inner class UpdateAthlete {
        @Test
        fun `should update athlete successfully`() {
            val clubA = clubEntity()
            val existing = athleteEntity(name = "old", club = clubA)
            val newClub = clubEntity(id = 2, name = "Club B")

            whenever(athletes.findById(1)).thenReturn(Optional.of(existing))
            whenever(clubs.findById(2)).thenReturn(Optional.of(newClub))
            whenever(athletes.save(any())).thenAnswer { it.getArgument<AthleteEntity>(0) }

            val result = service.updateAthlete(1, "new-name", "MALE", 2)

            assertEquals(success(Athlete(1, "new-name", GenderType.MALE, 2, now)), result)
        }

        @Test
        fun `should fail when athlete does not exist`() {
            whenever(athletes.findById(1)).thenReturn(Optional.empty())

            val result = service.updateAthlete(1, "new-name", "MALE", 2)

            assertEquals(failure(ApiError.ATHLETE_NOT_FOUND), result)
        }

        @Test
        fun `should fail when gender is invalid`() {
            whenever(athletes.findById(1)).thenReturn(Optional.of(athleteEntity()))

            val result = service.updateAthlete(1, "new-name", "INVALID", 2)

            assertEquals(failure(ApiError.INVALID_GENDER), result)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(athletes.findById(1)).thenReturn(Optional.of(athleteEntity()))
            whenever(clubs.findById(2)).thenReturn(Optional.empty())

            val result = service.updateAthlete(1, "new-name", "MALE", 2)

            assertEquals(failure(ApiError.CLUB_NOT_FOUND), result)
        }
    }
}
