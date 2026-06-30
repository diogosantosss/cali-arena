package com.caliarena.repo

import com.caliarena.domain.token.Token
import com.caliarena.domain.token.TokenValidationInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.TokenEntity
import com.caliarena.repo.entities.TokenEntity.Companion.toDomain
import com.caliarena.repo.entities.UserEntity.Companion.fromDomain
import com.caliarena.repo.jpa.TokenRepositoryJpa
import com.caliarena.repo.jpa.UserRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import pt.isel.repo.RepositoryUser
import java.time.Instant

@Repository
class UserRepository(
    private val userRepositoryJpa: UserRepositoryJpa,
    private val tokenRepositoryJpa: TokenRepositoryJpa,
) : RepositoryUser {
    override fun createUser(
        username: String,
        passwordValidationInfo: PasswordValidationInfo,
        role: UserRole,
    ): User {
        val user =
            User(
                id = 0,
                username = username,
                password = passwordValidationInfo,
                role = role,
                createdAt = Instant.now(),
            )

        return userRepositoryJpa.save(user.fromDomain()).toDomain()
    }

    override fun findByUsername(username: String): User? = userRepositoryJpa.findByUsername(username)?.toDomain()

    override fun getTokenByTokenValidation(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? {
        val tokenEntity =
            tokenRepositoryJpa.findByIdOrNull(tokenValidationInfo.validationInfo)
                ?: return null

        return Pair(tokenEntity.user.toDomain(), tokenEntity.toDomain())
    }

    override fun createToken(
        token: Token,
        maxToken: Int,
    ) {
        val user =
            userRepositoryJpa.findByIdOrNull(token.userId)
                ?: return

        // Delete the oldest token when achieved the max number of tokens
        tokenRepositoryJpa.deleteOldestTokensExceeding(token.userId, maxToken - 1)

        val tokenEntity =
            TokenEntity(
                tokenValidation = token.tokenValidationInfo.validationInfo,
                user = user,
                createdAt = token.createdAt.epochSecond,
                lastUsedAt = token.lastUsedAt.epochSecond,
            )

        tokenRepositoryJpa.save(tokenEntity)
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        val token =
            tokenRepositoryJpa.findByIdOrNull(token.tokenValidationInfo.validationInfo)
                ?: return

        token.lastUsedAt = now.epochSecond
        tokenRepositoryJpa.save(token)
    }

    override fun removeTokenByTokenValidation(tokenValidationInfo: TokenValidationInfo): Int =
        tokenRepositoryJpa.deleteByTokenValidation(tokenValidationInfo.validationInfo)

    override fun findById(id: Int): User? = userRepositoryJpa.findByIdOrNull(id) ?.toDomain()

    override fun findAll(): List<User> = userRepositoryJpa.findAll().map { it.toDomain() }

    override fun save(entity: User): User? = userRepositoryJpa.save(entity.fromDomain()).toDomain()

    override fun deleteById(id: Int) = userRepositoryJpa.deleteById(id)

    override fun clear() = userRepositoryJpa.deleteAll()
}
