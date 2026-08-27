package com.caliarena.service

import com.caliarena.domain.club.Club
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class ClubServiceTest : ServiceTest() {
    private lateinit var service: ClubService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.clubs).thenReturn(clubs)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = ClubService(trxManager, clock)
    }

    private val now = clock.instant()

    private fun clubEntity(
        id: Int = 1,
        name: String = "Sporting CP",
        shortName: String? = "SCP",
    ) = ClubEntity(id, name, shortName, now.epochSecond)

    @Nested
    inner class CreateClub {
        @Test
        fun `should create club successfully`() {
            whenever(clubs.findByName("Sporting CP")).thenReturn(null)
            whenever(clubs.save(any())).thenReturn(clubEntity())

            val result = service.createClub("Sporting CP", "SCP")

            assertEquals(success(clubEntity().toDomain()), result)

            val captor = argumentCaptor<ClubEntity>()
            verify(clubs).save(captor.capture())
            assertEquals("Sporting CP", captor.firstValue.name)
            assertEquals("SCP", captor.firstValue.shortName)
            assertEquals(now.epochSecond, captor.firstValue.createdAt)
        }

        @Test
        fun `should create club successfully with null shortName`() {
            whenever(clubs.findByName("Sporting CP")).thenReturn(null)
            whenever(clubs.save(any())).thenReturn(clubEntity(shortName = null))

            val result = service.createClub("Sporting CP", null)

            assertEquals(success(clubEntity(shortName = null).toDomain()), result)
        }

        @Test
        fun `should fail when club name already exists`() {
            whenever(clubs.findByName("Sporting CP")).thenReturn(clubEntity())

            val result = service.createClub("Sporting CP", "SCP")

            assertEquals(failure(ApiError.CLUB_ALREADY_EXISTS), result)

            verify(clubs, never()).save(any())
        }
    }

    @Nested
    inner class GetClubById {
        @Test
        fun `should return club when found`() {
            whenever(clubs.findById(1)).thenReturn(Optional.of(clubEntity()))

            val result = service.getClubById(1)

            assertEquals(success(clubEntity().toDomain()), result)

            verify(clubs).findById(1)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(clubs.findById(1)).thenReturn(Optional.empty())

            val result = service.getClubById(1)

            assertEquals(failure(ApiError.CLUB_NOT_FOUND), result)
        }
    }

    @Nested
    inner class GetAllClubs {
        @Test
        fun `should return all clubs`() {
            val list =
                listOf(
                    clubEntity(id = 1),
                    clubEntity(id = 2, name = "Benfica", shortName = "SLB"),
                )
            whenever(clubs.findAll()).thenReturn(list)

            val result = service.getAllClubs()

            assertEquals(list.map { it.toDomain() }, result)

            verify(clubs).findAll()
        }

        @Test
        fun `should return empty list when no clubs exist`() {
            whenever(clubs.findAll()).thenReturn(emptyList())

            val result = service.getAllClubs()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class UpdateClub {
        @Test
        fun `should update club successfully`() {
            val existing = clubEntity(name = "Sporting CP")
            whenever(clubs.findById(1)).thenReturn(Optional.of(existing))
            whenever(clubs.findByName("Sporting Clube de Portugal")).thenReturn(null)
            whenever(clubs.save(any())).thenAnswer { it.getArgument<ClubEntity>(0) }

            val result = service.updateClub(1, "Sporting Clube de Portugal", "SCP")

            assertEquals(success(Club(1, "Sporting Clube de Portugal", "SCP", now)), result)
        }

        @Test
        fun `should update club successfully when name belongs to same club`() {
            val existing = clubEntity(name = "Sporting CP")
            whenever(clubs.findById(1)).thenReturn(Optional.of(existing))
            whenever(clubs.findByName("Sporting CP")).thenReturn(existing)
            whenever(clubs.save(any())).thenAnswer { it.getArgument<ClubEntity>(0) }

            val result = service.updateClub(1, "Sporting CP", "SC")

            assertEquals(success(Club(1, "Sporting CP", "SC", now)), result)
        }

        @Test
        fun `should update club successfully with null shortName`() {
            val existing = clubEntity(name = "Sporting CP")
            whenever(clubs.findById(1)).thenReturn(Optional.of(existing))
            whenever(clubs.findByName("Sporting Clube de Portugal")).thenReturn(null)
            whenever(clubs.save(any())).thenAnswer { it.getArgument<ClubEntity>(0) }

            val result = service.updateClub(1, "Sporting Clube de Portugal", null)

            assertEquals(success(Club(1, "Sporting Clube de Portugal", null, now)), result)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(clubs.findById(1)).thenReturn(Optional.empty())

            val result = service.updateClub(1, "Sporting Clube de Portugal", "SCP")

            assertEquals(failure(ApiError.CLUB_NOT_FOUND), result)

            verify(clubs, never()).findByName(any())
            verify(clubs, never()).save(any())
        }

        @Test
        fun `should fail when new name is already used by another club`() {
            val otherClub = clubEntity(id = 2, name = "Benfica", shortName = "SLB")

            whenever(clubs.findById(1)).thenReturn(Optional.of(clubEntity()))
            whenever(clubs.findByName("Benfica")).thenReturn(otherClub)

            val result = service.updateClub(1, "Benfica", "SLB")

            assertEquals(failure(ApiError.CLUB_ALREADY_EXISTS), result)

            verify(clubs, never()).save(any())
        }
    }
}
