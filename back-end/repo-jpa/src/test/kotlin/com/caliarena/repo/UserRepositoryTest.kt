package com.caliarena.repo

import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.user.TokenEntity
import com.caliarena.repo.entities.user.UserEntity
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
class UserRepositoryTest {
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

    private fun Transaction.newUser(username: String = "user-${System.nanoTime()}"): UserEntity =
        users.save(
            UserEntity(
                username = username,
                password = "hashed_pw",
                role = UserRole.JUDGE,
                createdAt = now().epochSecond,
            ),
        )

    private fun Transaction.createToken(
        user: UserEntity,
        validation: String,
        lastUsedAt: Instant = now(),
    ): TokenEntity {
        val token =
            TokenEntity(
                tokenValidation = validation,
                user = user,
                createdAt = lastUsedAt.epochSecond,
                lastUsedAt = lastUsedAt.epochSecond,
            )
        // mesmo comportamento do service: manter apenas (max - 1) tokens antes de inserir o novo
        tokens.deleteOldestTokensExceeding(user.id, 1)
        return tokens.save(token)
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `should create a user with the given fields`() =
            trx.run {
                val created = newUser("alice")

                assertNotEquals(0, created.id)
                assertEquals("alice", created.username)
                assertEquals(UserRole.JUDGE, created.role)
            }

        @Test
        fun `should persist the user so it can be found by id`() =
            trx.run {
                val created = newUser("bob")

                val found = users.findByIdOrNull(created.id)

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

                val found = users.findByUsername("carlos")

                assertNotNull(found)
                assertEquals("carlos", found?.username)
            }

        @Test
        fun `should return null when username does not exist`() =
            trx.run {
                assertNull(users.findByUsername("does-not-exist"))
            }
    }

    @Nested
    inner class FindById {
        @Test
        fun `should find an existing user by id`() =
            trx.run {
                val created = newUser()

                val found = users.findByIdOrNull(created.id)

                assertNotNull(found)
                assertEquals(created.id, found?.id)
            }

        @Test
        fun `should return null when id does not exist`() =
            trx.run {
                assertNull(users.findByIdOrNull(-1))
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

                assertEquals(3, users.findAll().count())
            }

        @Test
        fun `should return empty list when there are no users`() =
            trx.run {
                assertEquals(0, users.findAll().count())
            }
    }

    @Nested
    inner class Save {
        @Test
        fun `should update an existing user`() =
            trx.run {
                val created = newUser("dave")
                created.username = "dave-updated"
                val saved = users.save(created)

                assertEquals("dave-updated", saved.username)
                assertEquals(created.id, saved.id)
            }
    }

    @Nested
    inner class DeleteById {
        @Test
        fun `should remove the user`() =
            trx.run {
                val created = newUser()

                users.deleteById(created.id)

                assertNull(users.findByIdOrNull(created.id))
            }
    }

    @Nested
    inner class Tokens {
        @Test
        fun `should create a token for an existing user`() =
            trx.run {
                val user = newUser()
                val validation = "token-${System.nanoTime()}"

                createToken(user, validation)

                val result = tokens.findByIdOrNull(validation)
                assertNotNull(result)
                assertEquals(user.id, result?.user?.id)
            }

        @Test
        fun `should delete the oldest tokens when max is exceeded`() =
            trx.run {
                val user = newUser()
                val base = now()

                createToken(user, "token-1", base)
                createToken(user, "token-2", base.plusSeconds(1))
                createToken(user, "token-3", base.plusSeconds(2))

                assertNull(tokens.findByIdOrNull("token-1"))
                assertNotNull(tokens.findByIdOrNull("token-2"))
                assertNotNull(tokens.findByIdOrNull("token-3"))
            }

        @Test
        fun `should update last used timestamp`() =
            trx.run {
                val user = newUser()
                val newTime = now().plusSeconds(60)
                createToken(user, "token-lu", newTime.minusSeconds(60))

                val stored = tokens.findByIdOrNull("token-lu")!!
                stored.lastUsedAt = newTime.epochSecond
                tokens.save(stored)

                assertEquals(newTime.epochSecond, tokens.findByIdOrNull("token-lu")?.lastUsedAt)
            }

        @Test
        fun `should delete a token by validation`() =
            trx.run {
                val user = newUser()
                createToken(user, "token-del")

                val deletions = tokens.deleteByTokenValidation("token-del")

                assertEquals(1, deletions)
                assertNull(tokens.findByIdOrNull("token-del"))
            }

        @Test
        fun `should return 0 when deleting a non-existing token`() =
            trx.run {
                assertEquals(0, tokens.deleteByTokenValidation("does-not-exist"))
            }
    }
}
