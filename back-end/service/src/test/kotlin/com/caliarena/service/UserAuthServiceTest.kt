package com.caliarena.service

import com.caliarena.domain.token.Token
import com.caliarena.domain.token.TokenValidationInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.user.TokenEntity
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Optional

class UserAuthServiceTest : ServiceTest() {
    private lateinit var service: UserAuthService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.users).thenReturn(users)
        lenient().whenever(transaction.tokens).thenReturn(tokens)

        lenient()
            .doAnswer { invocation ->
                val block = invocation.getArgument<Transaction.() -> Any>(0)
                block(transaction)
            }.whenever(trxManager)
            .run<Any>(any())

        service =
            UserAuthService(
                passwordEncoder,
                tokenEncoder,
                config,
                trxManager,
                clock,
            )
    }

    private val now = clock.instant()

    private fun userEntity(
        id: Int = 1,
        username: String = "diogo",
        passwordHash: String = "encoded-password",
        role: UserRole = UserRole.JUDGE,
    ) = UserEntity(id, username, passwordHash, role, now.epochSecond)

    @Nested
    inner class ValidatePassword {
        @Test
        fun `should return true when password matches`() {
            val validationInfo = PasswordValidationInfo("hashed-password")

            whenever(
                passwordEncoder.matches(
                    "Password1",
                    validationInfo.validationInfo,
                ),
            ).thenReturn(true)

            val result =
                service.validatePassword(
                    "Password1",
                    validationInfo,
                )

            assertTrue(result)

            verify(passwordEncoder).matches(
                "Password1",
                validationInfo.validationInfo,
            )
        }

        @Test
        fun `should return false when password does not match`() {
            val validationInfo = PasswordValidationInfo("hashed-password")

            whenever(
                passwordEncoder.matches(
                    "WrongPassword",
                    validationInfo.validationInfo,
                ),
            ).thenReturn(false)

            val result =
                service.validatePassword(
                    "WrongPassword",
                    validationInfo,
                )

            assertFalse(result)
        }
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `should create user successfully`() {
            val savedEntity =
                userEntity(id = 1, username = "diogo", passwordHash = "encoded")

            whenever(passwordEncoder.encode("Password1"))
                .thenReturn("encoded")

            whenever(users.findByUsername("diogo"))
                .thenReturn(null)

            whenever(users.save(any())).thenReturn(savedEntity)

            val result =
                service.createUser(
                    "diogo",
                    "Password1",
                )

            assertEquals(success(savedEntity.toDomain()), result)

            val captor = argumentCaptor<UserEntity>()
            verify(users).save(captor.capture())
            assertEquals("diogo", captor.firstValue.username)
            assertEquals("encoded", captor.firstValue.password)
            assertEquals(UserRole.JUDGE, captor.firstValue.role)
        }

        @Test
        fun `should fail when username already exists`() {
            whenever(passwordEncoder.encode(any()))
                .thenReturn("encoded")

            whenever(users.findByUsername("diogo"))
                .thenReturn(userEntity())

            val result =
                service.createUser(
                    "diogo",
                    "Password1",
                )

            assertEquals(
                failure(ApiError.ALREADY_USED_USERNAME),
                result,
            )

            verify(users, never()).save(any())
        }

        @Test
        fun `should fail when password is insecure`() {
            val result =
                service.createUser(
                    "diogo",
                    "123",
                )

            assertEquals(
                failure(ApiError.INSECURE_PASSWORD),
                result,
            )

            verifyNoInteractions(users)
            verifyNoInteractions(passwordEncoder)
        }

        @Test
        fun `should create password validation info using encoder`() {
            whenever(passwordEncoder.encode("Password1"))
                .thenReturn("encoded-password")

            whenever(users.findByUsername("diogo"))
                .thenReturn(null)

            whenever(users.save(any()))
                .thenReturn(userEntity(username = "diogo", passwordHash = "encoded-password"))

            service.createUser(
                "diogo",
                "Password1",
            )

            verify(passwordEncoder).encode("Password1")
        }
    }

    @Nested
    inner class CreateToken {
        private val user = userEntity()

        private val validationInfo = TokenValidationInfo("token-validation")

        @Test
        fun `should fail when username is blank`() {
            val result = service.createToken("", "Password1")

            assertEquals(
                failure(ApiError.USER_OR_PASSWORD_ARE_INVALID),
                result,
            )

            verifyNoInteractions(users)
        }

        @Test
        fun `should fail when password is blank`() {
            val result = service.createToken("diogo", "")

            assertEquals(
                failure(ApiError.USER_OR_PASSWORD_ARE_INVALID),
                result,
            )

            verifyNoInteractions(users)
        }

        @Test
        fun `should fail when user does not exist`() {
            whenever(users.findByUsername("diogo"))
                .thenReturn(null)

            val result =
                service.createToken(
                    "diogo",
                    "Password1",
                )

            assertEquals(
                failure(ApiError.USER_OR_PASSWORD_ARE_INVALID),
                result,
            )

            verify(users).findByUsername("diogo")
            verify(tokens, never()).save(any())
        }

        @Test
        fun `should fail when password is incorrect`() {
            whenever(users.findByUsername("diogo"))
                .thenReturn(user)

            whenever(
                passwordEncoder.matches(
                    "WrongPassword",
                    PasswordValidationInfo(user.password).validationInfo,
                ),
            ).thenReturn(false)

            val result =
                service.createToken(
                    "diogo",
                    "WrongPassword",
                )

            assertEquals(
                failure(ApiError.USER_OR_PASSWORD_ARE_INVALID),
                result,
            )

            verify(tokens, never()).save(any())
        }

        @Test
        fun `should create token successfully`() {
            whenever(users.findByUsername("diogo"))
                .thenReturn(user)

            whenever(users.findById(user.id))
                .thenReturn(Optional.of(user))

            whenever(
                passwordEncoder.matches(
                    "Password1",
                    PasswordValidationInfo(user.password).validationInfo,
                ),
            ).thenReturn(true)

            whenever(tokenEncoder.createValidationInformation(any()))
                .thenReturn(validationInfo)

            val result =
                service.createToken(
                    "diogo",
                    "Password1",
                )

            assertTrue(result is Success)

            verify(tokens).deleteOldestTokensExceeding(user.id, config.maxTokensPerUser - 1)

            val captor = argumentCaptor<TokenEntity>()
            verify(tokens).save(captor.capture())

            val createdToken = captor.firstValue

            assertEquals(user.id, createdToken.user.id)
            assertEquals(now.epochSecond, createdToken.createdAt)
            assertEquals(now.epochSecond, createdToken.lastUsedAt)
            assertEquals(validationInfo.validationInfo, createdToken.tokenValidation)

            val tokenInfo = (result as Success).value

            assertNotNull(tokenInfo.tokenValue)
        }
    }

    @Nested
    inner class RevokeToken {
        @Test
        fun `should revoke token successfully`() {
            val validationInfo = TokenValidationInfo("validation")

            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            val result = service.revokeToken("token")

            assertTrue(result)

            verify(tokenEncoder).createValidationInformation("token")
            verify(tokens).deleteByTokenValidation(validationInfo.validationInfo)
        }
    }

    @Nested
    inner class GetUserByToken {
        private val user = userEntity()

        private val validationInfo =
            TokenValidationInfo("validation")

        @Test
        fun `should return null when token format is invalid`() {
            val result =
                service.getUserByToken("invalid-token")

            assertNull(result)

            verifyNoInteractions(tokenEncoder)
            verifyNoInteractions(tokens)
        }

        @Test
        fun `should return null when token does not exist`() {
            val token =
                Token.generateTokenValue(config)

            whenever(tokenEncoder.createValidationInformation(token))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.empty())

            val result =
                service.getUserByToken(token)

            assertNull(result)

            verify(tokens).findById(validationInfo.validationInfo)
        }

        @Test
        fun `should return null when token is expired`() {
            val tokenValue =
                Token.generateTokenValue(config)

            val expiredToken =
                TokenEntity(
                    tokenValidation = validationInfo.validationInfo,
                    user = user,
                    createdAt = now.minus(config.tokenTtl).minusSeconds(10).epochSecond,
                    lastUsedAt = now.minus(config.tokenRollingTtl).minusSeconds(10).epochSecond,
                )

            whenever(tokenEncoder.createValidationInformation(tokenValue))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.of(expiredToken))

            val result =
                service.getUserByToken(tokenValue)

            assertNull(result)

            verify(tokens, never()).save(any())
        }

        @Test
        fun `should return user when token is valid and refresh last used`() {
            val tokenValue =
                Token.generateTokenValue(config)

            val validToken =
                TokenEntity(
                    tokenValidation = validationInfo.validationInfo,
                    user = user,
                    createdAt = now.minusSeconds(60).epochSecond,
                    lastUsedAt = now.minusSeconds(30).epochSecond,
                )

            whenever(tokenEncoder.createValidationInformation(tokenValue))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.of(validToken))

            val result =
                service.getUserByToken(tokenValue)

            assertEquals(user.toDomain(), result)

            val captor = argumentCaptor<TokenEntity>()
            verify(tokens).save(captor.capture())
            assertEquals(now.epochSecond, captor.firstValue.lastUsedAt)
        }
    }

    @Nested
    inner class UpdateUserRole {
        private val admin = userEntity(id = 1, username = "admin", role = UserRole.ADMIN)
        private val judge = userEntity(id = 2, username = "judge", role = UserRole.JUDGE)

        private val validationInfo = TokenValidationInfo("validation")

        private fun adminTokenEntity() = TokenEntity(validationInfo.validationInfo, admin, now.epochSecond, now.epochSecond)

        @Test
        fun `should fail when authenticated user does not exist`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.empty())

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(ApiError.USER_NOT_FOUND),
                result,
            )
        }

        @Test
        fun `should fail when authenticated user is not admin`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.of(TokenEntity(validationInfo.validationInfo, judge, now.epochSecond, now.epochSecond)))

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(ApiError.NOT_AUTHORIZED),
                result,
            )

            verify(users, never()).save(any())
        }

        @Test
        fun `should fail when role is invalid`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.of(adminTokenEntity()))

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "INVALID_ROLE",
                )

            assertEquals(
                failure(ApiError.INVALID_ROLE),
                result,
            )

            verify(users, never()).save(any())
        }

        @Test
        fun `should fail when target user does not exist`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.of(adminTokenEntity()))

            whenever(users.findById(2))
                .thenReturn(Optional.empty())

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(ApiError.USER_NOT_FOUND),
                result,
            )

            verify(users).findById(2)
            verify(users, never()).save(any())
        }

        @Test
        fun `should update user role successfully`() {
            val updatedJudge = userEntity(id = 2, username = "judge", role = UserRole.ADMIN)

            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(tokens.findById(validationInfo.validationInfo))
                .thenReturn(Optional.of(adminTokenEntity()))

            whenever(users.findById(2))
                .thenReturn(Optional.of(updatedJudge))

            whenever(users.save(any())).thenReturn(updatedJudge)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                success(User(2, "judge", PasswordValidationInfo("encoded-password"), UserRole.ADMIN, now)),
                result,
            )
        }
    }
}
