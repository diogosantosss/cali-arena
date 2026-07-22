package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.club.Club
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ClubServiceTest : ServiceTest() {
    private lateinit var service: ClubService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoClub).thenReturn(repoClub)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service = ClubService(trxManager, clock)
    }

    @Nested
    inner class CreateClub {
        private val now = clock.instant()

        private val club =
            Club(
                id = 1,
                name = "Sporting CP",
                shortName = "SCP",
                createdAt = now,
            )

        @Test
        fun `should create club successfully`() {
            whenever(repoClub.findByName("Sporting CP")).thenReturn(null)

            whenever(
                repoClub.createClub(
                    name = "Sporting CP",
                    shortName = "SCP",
                    createdAt = now,
                ),
            ).thenReturn(club)

            val result = service.createClub("Sporting CP", "SCP")

            assertEquals(success(club), result)

            verify(repoClub).findByName("Sporting CP")
            verify(repoClub).createClub(
                name = "Sporting CP",
                shortName = "SCP",
                createdAt = now,
            )
        }

        @Test
        fun `should create club successfully with null shortName`() {
            val clubNoShortName = club.copy(shortName = null)

            whenever(repoClub.findByName("Sporting CP")).thenReturn(null)

            whenever(
                repoClub.createClub(
                    name = "Sporting CP",
                    shortName = null,
                    createdAt = now,
                ),
            ).thenReturn(clubNoShortName)

            val result = service.createClub("Sporting CP", null)

            assertEquals(success(clubNoShortName), result)
        }

        @Test
        fun `should fail when club name already exists`() {
            whenever(repoClub.findByName("Sporting CP")).thenReturn(club)

            val result = service.createClub("Sporting CP", "SCP")

            assertEquals(failure(ClubError.ClubAlreadyExists), result)

            verify(repoClub, never()).createClub(any(), anyOrNull(), any())
        }
    }

    @Nested
    inner class GetClubById {
        private val now = clock.instant()

        private val club =
            Club(
                id = 1,
                name = "Sporting CP",
                shortName = "SCP",
                createdAt = now,
            )

        @Test
        fun `should return club when found`() {
            whenever(repoClub.findById(1)).thenReturn(club)

            val result = service.getClubById(1)

            assertEquals(success(club), result)

            verify(repoClub).findById(1)
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(repoClub.findById(1)).thenReturn(null)

            val result = service.getClubById(1)

            assertEquals(failure(ClubError.ClubNotFound), result)
        }
    }

    @Nested
    inner class GetAllClubs {
        private val now = clock.instant()

        private val clubs =
            listOf(
                Club(id = 1, name = "Sporting CP", shortName = "SCP", createdAt = now),
                Club(id = 2, name = "Benfica", shortName = "SLB", createdAt = now),
            )

        @Test
        fun `should return all clubs`() {
            whenever(repoClub.findAll()).thenReturn(clubs)

            val result = service.getAllClubs()

            assertEquals(clubs, result)

            verify(repoClub).findAll()
        }

        @Test
        fun `should return empty list when no clubs exist`() {
            whenever(repoClub.findAll()).thenReturn(emptyList())

            val result = service.getAllClubs()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class UpdateClub {
        private val now = clock.instant()

        private val existing =
            Club(
                id = 1,
                name = "Sporting CP",
                shortName = "SCP",
                createdAt = now,
            )

        private val updated =
            existing.copy(name = "Sporting Clube de Portugal", shortName = "SCP")

        @Test
        fun `should update club successfully`() {
            whenever(repoClub.findById(1)).thenReturn(existing)
            whenever(repoClub.findByName("Sporting Clube de Portugal")).thenReturn(null)
            whenever(repoClub.save(updated)).thenReturn(updated)

            val result = service.updateClub(1, "Sporting Clube de Portugal", "SCP")

            assertEquals(success(updated), result)

            verify(repoClub).findById(1)
            verify(repoClub).findByName("Sporting Clube de Portugal")
            verify(repoClub).save(updated)
        }

        @Test
        fun `should update club successfully when name belongs to same club`() {
            whenever(repoClub.findById(1)).thenReturn(existing)
            whenever(repoClub.findByName("Sporting CP")).thenReturn(existing)
            whenever(repoClub.save(existing.copy(shortName = "SC"))).thenReturn(existing.copy(shortName = "SC"))

            val result = service.updateClub(1, "Sporting CP", "SC")

            assertEquals(success(existing.copy(shortName = "SC")), result)

            verify(repoClub).save(existing.copy(shortName = "SC"))
        }

        @Test
        fun `should fail when club does not exist`() {
            whenever(repoClub.findById(1)).thenReturn(null)

            val result = service.updateClub(1, "Sporting Clube de Portugal", "SCP")

            assertEquals(failure(ClubError.ClubNotFound), result)

            verify(repoClub, never()).findByName(any())
            verify(repoClub, never()).save(any())
        }

        @Test
        fun `should fail when new name is already used by another club`() {
            val otherClub = Club(id = 2, name = "Benfica", shortName = "SLB", createdAt = now)

            whenever(repoClub.findById(1)).thenReturn(existing)
            whenever(repoClub.findByName("Benfica")).thenReturn(otherClub)

            val result = service.updateClub(1, "Benfica", "SLB")

            assertEquals(failure(ClubError.ClubAlreadyExists), result)

            verify(repoClub, never()).save(any())
        }

        @Test
        fun `should fail when repository fails to save updated club`() {
            whenever(repoClub.findById(1)).thenReturn(existing)
            whenever(repoClub.findByName("Sporting Clube de Portugal")).thenReturn(null)
            whenever(repoClub.save(any())).thenReturn(null)

            val result = service.updateClub(1, "Sporting Clube de Portugal", "SCP")

            assertEquals(failure(ClubError.UpdatingClub), result)
        }

        @Test
        fun `should update club successfully with null shortName`() {
            val updatedNoShortName = existing.copy(name = "Sporting Clube de Portugal", shortName = null)

            whenever(repoClub.findById(1)).thenReturn(existing)
            whenever(repoClub.findByName("Sporting Clube de Portugal")).thenReturn(null)
            whenever(repoClub.save(updatedNoShortName)).thenReturn(updatedNoShortName)

            val result = service.updateClub(1, "Sporting Clube de Portugal", null)

            assertEquals(success(updatedNoShortName), result)
        }
    }
}
