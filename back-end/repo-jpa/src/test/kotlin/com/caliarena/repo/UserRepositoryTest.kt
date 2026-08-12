package com.caliarena.repo

import com.caliarena.Transaction
import com.caliarena.domain.token.Token
import com.caliarena.domain.token.TokenValidationInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.jpa.TransactionManagerJpa
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
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
class UserRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @BeforeEach
    fun cleanup() {
        trx.run {
            repoUser.clear()
        }
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `should create a user with the given fields`() =
            trx.run {
                val user = newUser("alice")

                assertNotEquals(0, user.id)
                assertEquals("alice", user.username)
                assertEquals(UserRole.JUDGE, user.role)
            }

        @Test
        fun `should persist the user so it can be found by id`() =
            trx.run {
                val created = newUser("bob")

                val found = repoUser.findById(created.id)

                assertNotNull(found)
                assertEquals(created.username, found?.username)
            }
    }

    @Nested
    inner class FindByUsername {
        @Test
        fun `should find an existing user by username`() =
            trx.run {
                newUser("carlos")

                val found = repoUser.findByUsername("carlos")

                assertNotNull(found)
                assertEquals("carlos", found?.username)
            }

        @Test
        fun `should return null when username does not exist`() =
            trx.run {
                assertNull(repoUser.findByUsername("does-not-exist"))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing user by id`() =
            trx.run {
                val created = newUser()

                val found = repoUser.findById(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(repoUser.findById(-1))
            }
    }

    @Nested
    inner class FindAll {
        @Test
        fun `should return all created users`() =
            trx.run {
                newUser("u1")
                newUser("u2")
                newUser("u3")

                assertEquals(3, repoUser.findAll().size)
            }

        @Test
        fun `should return empty list when there are no users`() =
            trx.run {
                assertTrue(repoUser.findAll().isEmpty())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing user`() =
            trx.run {
                val created = newUser("dave")
                val updated = created.copy(username = "dave-updated")

                val saved = repoUser.save(updated)

                assertEquals("dave-updated", saved?.username)
                assertEquals(created.id, saved?.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the user`() =
            trx.run {
                val created = newUser()

                repoUser.deleteById(created.id)

                assertNull(repoUser.findById(created.id))
            }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all users`() =
            trx.run {
                newUser("u1")
                newUser("u2")

                repoUser.clear()

                assertTrue(repoUser.findAll().isEmpty())
            }
    }

    @Nested
    inner class CreateToken {
        @Test
        fun `should create a token for an existing user`() =
            trx.run {
                val user = newUser()
                val token = newToken(user.id)

                repoUser.createToken(token, maxToken = 3)

                val result = repoUser.getTokenByTokenValidation(token.tokenValidationInfo)
                assertNotNull(result)
                assertEquals(user.id, result?.first?.id)
            }

        @Test
        fun `should delete the oldest token when max tokens is exceeded`() =
            trx.run {
                val user = newUser()
                val base = now()

                val t1 = newToken(user.id, "token-1", base)
                val t2 = newToken(user.id, "token-2", base.plusSeconds(1))
                val t3 = newToken(user.id, "token-3", base.plusSeconds(2))

                repoUser.createToken(t1, maxToken = 2)
                repoUser.createToken(t2, maxToken = 2)
                repoUser.createToken(t3, maxToken = 2)

                assertNull(repoUser.getTokenByTokenValidation(t1.tokenValidationInfo))
                assertNotNull(repoUser.getTokenByTokenValidation(t2.tokenValidationInfo))
                assertNotNull(repoUser.getTokenByTokenValidation(t3.tokenValidationInfo))
            }

        @Test
        fun `should not create a token for a non-existing user`() =
            trx.run {
                val token = newToken(userId = -1, validation = "orphan-token")

                repoUser.createToken(token, maxToken = 3)

                assertNull(repoUser.getTokenByTokenValidation(token.tokenValidationInfo))
            }
    }

    @Nested
    inner class GetTokenByTokenValidation {
        @Test
        fun `should return the user and token when validation exists`() =
            trx.run {
                val user = newUser()
                val token = newToken(user.id)
                repoUser.createToken(token, maxToken = 3)

                val result = repoUser.getTokenByTokenValidation(token.tokenValidationInfo)

                assertNotNull(result)
                assertEquals(user.id, result?.first?.id)
                assertEquals(token.tokenValidationInfo, result?.second?.tokenValidationInfo)
            }

        @Test
        fun `should return null when token validation does not exist`() =
            trx.run {
                assertNull(repoUser.getTokenByTokenValidation(TokenValidationInfo("does-not-exist")))
            }
    }

    @Nested
    inner class UpdateTokenLastUsed {
        @Test
        fun `should update the last used timestamp`() =
            trx.run {
                val user = newUser()
                val token = newToken(user.id)
                repoUser.createToken(token, maxToken = 3)

                val newTime = now().plusSeconds(60)
                repoUser.updateTokenLastUsed(token, newTime)

                val result = repoUser.getTokenByTokenValidation(token.tokenValidationInfo)
                assertEquals(newTime, result?.second?.lastUsedAt)
            }

        @Test
        fun `should do nothing when token does not exist`() =
            trx.run {
                val nonExisting = newToken(userId = -1, validation = "ghost-token")

                assertDoesNotThrow {
                    repoUser.updateTokenLastUsed(nonExisting, now())
                }
            }
    }

    @Nested
    inner class RemoveTokenByTokenValidation {
        @Test
        fun `should remove an existing token and return 1`() =
            trx.run {
                val user = newUser()
                val token = newToken(user.id)
                repoUser.createToken(token, maxToken = 3)

                val deletions = repoUser.removeTokenByTokenValidation(token.tokenValidationInfo)

                assertEquals(1, deletions)
                assertNull(repoUser.getTokenByTokenValidation(token.tokenValidationInfo))
            }

        @Test
        fun `should return 0 when token does not exist`() =
            trx.run {
                assertEquals(0, repoUser.removeTokenByTokenValidation(TokenValidationInfo("does-not-exist")))
            }
    }

    private fun Transaction.now() = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    private fun Transaction.newUser(username: String = "user-${System.nanoTime()}"): User =
        repoUser.createUser(
            username = username,
            passwordValidationInfo = PasswordValidationInfo("hashed_pw"),
            role = UserRole.JUDGE,
            createdAt = now(),
        )

    private fun Transaction.newToken(
        userId: Int,
        validation: String = "token-${System.nanoTime()}",
        lastUsedAt: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS),
    ) = Token(
        tokenValidationInfo = TokenValidationInfo(validation),
        userId = userId,
        createdAt = lastUsedAt,
        lastUsedAt = lastUsedAt,
    )
}
