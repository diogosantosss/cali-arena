package com.caliarena.service

import com.caliarena.domain.token.Token
import com.caliarena.domain.token.Token.Companion.canBeToken
import com.caliarena.domain.token.Token.Companion.generateTokenValue
import com.caliarena.domain.token.TokenEncoder
import com.caliarena.domain.token.TokenExternalInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.domain.user.UsersDomainConfig
import com.caliarena.repo.entities.user.TokenEntity
import com.caliarena.repo.entities.user.TokenEntity.Companion.toDomain
import com.caliarena.repo.entities.user.UserEntity
import com.caliarena.repo.trx.TransactionManager
import jakarta.inject.Named
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock

@Named
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserAuthService::class.java)
    }

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
    ): Either<ApiError, User> {
        if (!PasswordValidationInfo.isSafePassword(password)) {
            return failure(ApiError.INSECURE_PASSWORD)
        }

        val passwordValidationInfo = createPasswordValidationInformation(password)

        return trxManager.run {
            if (users.findByUsername(username) != null) {
                return@run failure(ApiError.ALREADY_USED_USERNAME)
            }

            val userEntity =
                users.save(
                    UserEntity(
                        username = username,
                        password = passwordValidationInfo.validationInfo,
                        role = UserRole.JUDGE,
                        createdAt = clock.instant().epochSecond,
                    ),
                )

            return@run success(userEntity.toDomain())
        }
    }

    fun createToken(
        username: String,
        password: String,
    ): Either<ApiError, TokenExternalInfo> {
        if (username.isBlank() || password.isBlank()) {
            return failure(ApiError.USER_OR_PASSWORD_ARE_INVALID)
        }

        return trxManager.run {
            val userEntity =
                users.findByUsername(username)
                    ?: return@run failure(ApiError.USER_OR_PASSWORD_ARE_INVALID)

            val user = userEntity.toDomain()

            if (!validatePassword(password, user.password)) {
                return@run failure(ApiError.USER_OR_PASSWORD_ARE_INVALID)
            }

            val tokenValue = generateTokenValue(config)
            val now = clock.instant()

            logger.debug("token value {}, user {}", tokenValue, user.username)

            val token =
                Token(
                    tokenEncoder.createValidationInformation(tokenValue),
                    user.id,
                    createdAt = now,
                    lastUsedAt = now,
                )

            users.findByIdOrNull(token.userId)?.let { entity ->
                tokens.deleteOldestTokensExceeding(entity.id, config.maxTokensPerUser - 1)
                tokens.save(
                    TokenEntity(
                        tokenValidation = token.tokenValidationInfo.validationInfo,
                        user = entity,
                        createdAt = token.createdAt.epochSecond,
                        lastUsedAt = token.lastUsedAt.epochSecond,
                    ),
                )
            }

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
            tokens.deleteByTokenValidation(tokenValidationInfo.validationInfo)
            true
        }
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token, config)) {
            return null
        }
        return trxManager.run {
            val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
            val tokenEntity = tokens.findByIdOrNull(tokenValidationInfo.validationInfo)

            if (tokenEntity != null && tokenEntity.toDomain().isTimeValid(clock, config)) {
                tokenEntity.lastUsedAt = clock.instant().epochSecond
                tokens.save(tokenEntity)
                tokenEntity.user.toDomain()
            } else {
                null
            }
        }
    }

    fun updateUserRole(
        token: String,
        userToUpdateId: Int,
        role: String,
    ): Either<ApiError, User> =
        trxManager.run {
            val validationInfo =
                tokenEncoder.createValidationInformation(token).validationInfo

            val requester =
                tokens
                    .findByIdOrNull(validationInfo)
                    ?.user ?: return@run failure(ApiError.USER_NOT_FOUND)

            if (requester.role != UserRole.ADMIN) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            val role =
                UserRole.entries.find { it.name.equals(role, true) }
                    ?: return@run failure(ApiError.INVALID_ROLE)

            val target =
                users.findByIdOrNull(userToUpdateId)
                    ?: return@run failure(ApiError.USER_NOT_FOUND)

            target.role = role

            success(users.save(target).toDomain())
        }

    fun getUsers(): List<User> =
        trxManager.run {
            users.findAll().map { it.toDomain() }
        }
}
