package com.caliarena.service

import com.caliarena.Transaction
import com.caliarena.domain.token.Token
import com.caliarena.domain.token.TokenValidationInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class UserAuthServiceTest : ServiceTest() {
    private lateinit var service: UserAuthService

    @BeforeEach
    fun setup() {
        lenient().whenever(transaction.repoUser).thenReturn(repoUser)

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
            val now = clock.instant()

            val createdUser =
                User(
                    id = 1,
                    username = "diogo",
                    password = PasswordValidationInfo("encoded"),
                    role = UserRole.JUDGE,
                    createdAt = now,
                )

            whenever(passwordEncoder.encode("Password1"))
                .thenReturn("encoded")

            whenever(repoUser.findByUsername("diogo"))
                .thenReturn(null)

            whenever(
                repoUser.createUser(
                    eq("diogo"),
                    any(),
                    eq(UserRole.JUDGE),
                    eq(now),
                ),
            ).thenReturn(createdUser)

            val result =
                service.createUser(
                    "diogo",
                    "Password1",
                )

            assertEquals(success(createdUser), result)

            verify(repoUser).findByUsername("diogo")
            verify(repoUser).createUser(
                eq("diogo"),
                any(),
                eq(UserRole.JUDGE),
                eq(now),
            )
        }

        @Test
        fun `should fail when username already exists`() {
            val existingUser =
                User(
                    id = 1,
                    username = "diogo",
                    password = PasswordValidationInfo("hash"),
                    role = UserRole.JUDGE,
                    createdAt = clock.instant(),
                )

            whenever(passwordEncoder.encode(any()))
                .thenReturn("encoded")

            whenever(repoUser.findByUsername("diogo"))
                .thenReturn(existingUser)

            val result =
                service.createUser(
                    "diogo",
                    "Password1",
                )

            assertEquals(
                failure(UserError.AlreadyUsedUsername),
                result,
            )

            verify(repoUser, never()).createUser(
                any(),
                any(),
                any(),
                any(),
            )
        }

        @Test
        fun `should fail when password is insecure`() {
            val result =
                service.createUser(
                    "diogo",
                    "123",
                )

            assertEquals(
                failure(UserError.InsecurePassword),
                result,
            )

            verifyNoInteractions(repoUser)
            verifyNoInteractions(passwordEncoder)
        }

        @Test
        fun `should create password validation info using encoder`() {
            val now = clock.instant()

            val createdUser =
                User(
                    id = 1,
                    username = "diogo",
                    password = PasswordValidationInfo("encoded-password"),
                    role = UserRole.JUDGE,
                    createdAt = now,
                )

            whenever(passwordEncoder.encode("Password1"))
                .thenReturn("encoded-password")

            whenever(repoUser.findByUsername("diogo"))
                .thenReturn(null)

            whenever(
                repoUser.createUser(
                    eq("diogo"),
                    any(),
                    eq(UserRole.JUDGE),
                    eq(now),
                ),
            ).thenReturn(createdUser)

            service.createUser(
                "diogo",
                "Password1",
            )

            verify(passwordEncoder).encode("Password1")
        }
    }

    @Nested
    inner class CreateToken {
        private val now = clock.instant()

        private val user =
            User(
                id = 1,
                username = "diogo",
                password = PasswordValidationInfo("encoded-password"),
                role = UserRole.JUDGE,
                createdAt = now,
            )

        private val validationInfo = TokenValidationInfo("token-validation")

        @Test
        fun `should fail when username is blank`() {
            val result = service.createToken("", "Password1")

            assertEquals(
                failure(UserError.UserOrPasswordAreInvalid),
                result,
            )

            verifyNoInteractions(repoUser)
        }

        @Test
        fun `should fail when password is blank`() {
            val result = service.createToken("diogo", "")

            assertEquals(
                failure(UserError.UserOrPasswordAreInvalid),
                result,
            )

            verifyNoInteractions(repoUser)
        }

        @Test
        fun `should fail when user does not exist`() {
            whenever(repoUser.findByUsername("diogo"))
                .thenReturn(null)

            val result =
                service.createToken(
                    "diogo",
                    "Password1",
                )

            assertEquals(
                failure(UserError.UserOrPasswordAreInvalid),
                result,
            )

            verify(repoUser).findByUsername("diogo")
            verify(repoUser, never()).createToken(any(), any())
        }

        @Test
        fun `should fail when password is incorrect`() {
            whenever(repoUser.findByUsername("diogo"))
                .thenReturn(user)

            whenever(
                passwordEncoder.matches(
                    "WrongPassword",
                    user.password.validationInfo,
                ),
            ).thenReturn(false)

            val result =
                service.createToken(
                    "diogo",
                    "WrongPassword",
                )

            assertEquals(
                failure(UserError.UserOrPasswordAreInvalid),
                result,
            )

            verify(repoUser, never()).createToken(any(), any())
        }

        @Test
        fun `should create token successfully`() {
            whenever(repoUser.findByUsername("diogo"))
                .thenReturn(user)

            whenever(
                passwordEncoder.matches(
                    "Password1",
                    user.password.validationInfo,
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

            val tokenCaptor = argumentCaptor<Token>()

            verify(repoUser).createToken(
                tokenCaptor.capture(),
                eq(config.maxTokensPerUser),
            )

            val createdToken = tokenCaptor.firstValue

            assertEquals(user.id, createdToken.userId)
            assertEquals(now, createdToken.createdAt)
            assertEquals(now, createdToken.lastUsedAt)
            assertEquals(validationInfo, createdToken.tokenValidationInfo)

            val tokenInfo = (result as Success).value

            assertNotNull(tokenInfo.tokenValue)
            assertEquals(
                createdToken.expiration(config),
                tokenInfo.tokenExpiration,
            )
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
            verify(repoUser).removeTokenByTokenValidation(validationInfo)
        }
    }

    @Nested
    inner class GetUserByToken {
        private val now = clock.instant()

        private val user =
            User(
                id = 1,
                username = "diogo",
                password = PasswordValidationInfo("password"),
                role = UserRole.JUDGE,
                createdAt = now,
            )

        private val validationInfo =
            TokenValidationInfo("validation")

        @Test
        fun `should return null when token format is invalid`() {
            val result =
                service.getUserByToken("invalid-token")

            assertNull(result)

            verifyNoInteractions(tokenEncoder)
            verifyNoInteractions(repoUser)
        }

        @Test
        fun `should return null when token does not exist`() {
            val token =
                Token.generateTokenValue(config)

            whenever(tokenEncoder.createValidationInformation(token))
                .thenReturn(validationInfo)

            whenever(
                repoUser.getTokenByTokenValidation(validationInfo),
            ).thenReturn(null)

            val result =
                service.getUserByToken(token)

            assertNull(result)

            verify(repoUser).getTokenByTokenValidation(validationInfo)
            verify(repoUser, never()).updateTokenLastUsed(any(), any())
        }

        @Test
        fun `should return null when token is expired`() {
            val tokenValue =
                Token.generateTokenValue(config)

            val expiredToken =
                Token(
                    tokenValidationInfo = validationInfo,
                    userId = user.id,
                    createdAt = now.minus(config.tokenTtl).minusSeconds(10),
                    lastUsedAt = now.minus(config.tokenRollingTtl).minusSeconds(10),
                )

            whenever(tokenEncoder.createValidationInformation(tokenValue))
                .thenReturn(validationInfo)

            whenever(
                repoUser.getTokenByTokenValidation(validationInfo),
            ).thenReturn(user to expiredToken)

            val result =
                service.getUserByToken(tokenValue)

            assertNull(result)

            verify(repoUser, never()).updateTokenLastUsed(any(), any())
        }

        @Test
        fun `should return user when token is valid`() {
            val tokenValue =
                Token.generateTokenValue(config)

            val validToken =
                Token(
                    tokenValidationInfo = validationInfo,
                    userId = user.id,
                    createdAt = now.minusSeconds(60),
                    lastUsedAt = now.minusSeconds(30),
                )

            whenever(tokenEncoder.createValidationInformation(tokenValue))
                .thenReturn(validationInfo)

            whenever(
                repoUser.getTokenByTokenValidation(validationInfo),
            ).thenReturn(user to validToken)

            val result =
                service.getUserByToken(tokenValue)

            assertEquals(user, result)

            verify(repoUser).updateTokenLastUsed(
                validToken,
                now,
            )
        }
    }

    @Nested
    inner class UpdateUserRole {
        private val now = clock.instant()

        private val admin =
            User(
                id = 1,
                username = "admin",
                password = PasswordValidationInfo("password"),
                role = UserRole.ADMIN,
                createdAt = now,
            )

        private val judge =
            User(
                id = 2,
                username = "judge",
                password = PasswordValidationInfo("password"),
                role = UserRole.JUDGE,
                createdAt = now,
            )

        private val validationInfo = TokenValidationInfo("validation")

        private val token =
            Token(
                tokenValidationInfo = validationInfo,
                userId = admin.id,
                createdAt = now,
                lastUsedAt = now,
            )

        @Test
        fun `should fail when authenticated user does not exist`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(repoUser.getTokenByTokenValidation(validationInfo))
                .thenReturn(null)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(UserError.UserNotFound),
                result,
            )
        }

        @Test
        fun `should fail when authenticated user is not admin`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(repoUser.getTokenByTokenValidation(validationInfo))
                .thenReturn(judge to token)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(UserError.NotAuthorized),
                result,
            )

            verify(repoUser, never()).updateUserRole(any(), any())
        }

        @Test
        fun `should fail when role is invalid`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(repoUser.getTokenByTokenValidation(validationInfo))
                .thenReturn(admin to token)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "INVALID_ROLE",
                )

            assertEquals(
                failure(UserError.InvalidRole),
                result,
            )

            verify(repoUser, never()).updateUserRole(any(), any())
        }

        @Test
        fun `should fail when target user does not exist`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(repoUser.getTokenByTokenValidation(validationInfo))
                .thenReturn(admin to token)

            whenever(repoUser.findById(2))
                .thenReturn(null)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(UserError.UserNotFound),
                result,
            )

            verify(repoUser).findById(2)
            verify(repoUser, never()).updateUserRole(any(), any())
        }

        @Test
        fun `should fail when repository fails to update role`() {
            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(repoUser.getTokenByTokenValidation(validationInfo))
                .thenReturn(admin to token)

            whenever(repoUser.findById(2))
                .thenReturn(judge)

            whenever(
                repoUser.updateUserRole(
                    2,
                    UserRole.ADMIN,
                ),
            ).thenReturn(null)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                failure(UserError.ErrorUpdatingUserRole),
                result,
            )
        }

        @Test
        fun `should update user role successfully`() {
            val updatedUser =
                judge.copy(
                    role = UserRole.ADMIN,
                )

            whenever(tokenEncoder.createValidationInformation("token"))
                .thenReturn(validationInfo)

            whenever(repoUser.getTokenByTokenValidation(validationInfo))
                .thenReturn(admin to token)

            whenever(repoUser.findById(2))
                .thenReturn(judge)

            whenever(
                repoUser.updateUserRole(
                    2,
                    UserRole.ADMIN,
                ),
            ).thenReturn(updatedUser)

            val result =
                service.updateUserRole(
                    "token",
                    2,
                    "ADMIN",
                )

            assertEquals(
                success(updatedUser),
                result,
            )

            verify(repoUser).updateUserRole(
                2,
                UserRole.ADMIN,
            )
        }
    }
}
