package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.token.Token
import com.caliarena.domain.token.Token.Companion.canBeToken
import com.caliarena.domain.token.Token.Companion.generateTokenValue
import com.caliarena.domain.token.TokenEncoder
import com.caliarena.domain.token.TokenExternalInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.domain.user.UsersDomainConfig
import jakarta.inject.Named
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock

sealed class UserError {
    data object AlreadyUsedUsername : UserError()

    data object InsecurePassword : UserError()

    data object UserNotFound : UserError()

    data object InvalidRole : UserError()

    data object ErrorUpdatingUserRole : UserError()

    data object UserOrPasswordAreInvalid : UserError()
}

@Named
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {
    fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ) = passwordEncoder.matches(
        password,
        validationInfo.validationInfo,
    )

    private fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(
            passwordEncoder.encode(password)!!,
        )

    fun createUser(
        username: String,
        password: String,
    ): Either<UserError, User> {
        if (!PasswordValidationInfo.isSafePassword(password)) {
            return failure(UserError.InsecurePassword)
        }

        val passwordValidationInfo = createPasswordValidationInformation(password)

        return trxManager.run {
            if (repoUser.findByUsername(username) == null) {
                return@run failure(UserError.AlreadyUsedUsername)
            }

            val user =
                repoUser.createUser(
                    username = username,
                    passwordValidationInfo = passwordValidationInfo,
                    role = UserRole.JUDGE,
                    createdAt = clock.instant(),
                )

            return@run success(user)
        }
    }

    fun createToken(
        username: String,
        password: String,
    ): Either<UserError, TokenExternalInfo> {
        if (username.isBlank() || password.isBlank()) {
            return failure(UserError.UserOrPasswordAreInvalid)
        }

        return trxManager.run {
            val user =
                repoUser.findByUsername(username)
                    ?: return@run failure(UserError.UserOrPasswordAreInvalid)

            if (!validatePassword(password, user.password)) {
                return@run failure(UserError.UserOrPasswordAreInvalid)
            }

            val tokenValue = generateTokenValue(config)
            val now = clock.instant()

            val token =
                Token(
                    tokenEncoder.createValidationInformation(tokenValue),
                    user.id,
                    createdAt = now,
                    lastUsedAt = now,
                )

            repoUser.createToken(token, config.maxTokensPerUser)

            success(
                TokenExternalInfo(
                    tokenValue,
                    token.expiration(config),
                ),
            )
        }
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        return trxManager.run {
            repoUser.removeTokenByTokenValidation(tokenValidationInfo)
            true
        }
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token, config)) {
            return null
        }
        return trxManager.run {
            val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
            val userAndToken: Pair<User, Token>? = repoUser.getTokenByTokenValidation(tokenValidationInfo)
            if (userAndToken != null && userAndToken.second.isTimeValid(clock, config)) {
                repoUser.updateTokenLastUsed(userAndToken.second, clock.instant())
                userAndToken.first
            } else {
                null
            }
        }
    }

    fun updateUserRole(
        token: String,
        role: String,
    ): Either<UserError, User> =
        trxManager.run {
            val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
            val user =
                repoUser.getTokenByTokenValidation(tokenValidationInfo)
                    ?: return@run failure(UserError.UserNotFound)

            val role =
                UserRole.entries.find { it.name == role }
                    ?: return@run failure(UserError.InvalidRole)

            val updatedUser =
                repoUser.updateUserRole(user.first.id, role) ?: return@run failure(UserError.ErrorUpdatingUserRole)

            success(updatedUser)
        }
}
