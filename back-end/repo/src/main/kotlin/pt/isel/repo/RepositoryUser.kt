package pt.isel.repo

import com.caliarena.domain.token.Token
import com.caliarena.domain.token.TokenValidationInfo
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import java.time.Instant

/**
 * Repository interface for user-related operations
 *
 * @see Repository
 */
interface RepositoryUser : Repository<User> {

    fun createUser(
        username: String,
        passwordValidationInfo: PasswordValidationInfo,
        role: UserRole,
    ): User

    fun findByUsername(username: String): User?

    fun getTokenByTokenValidation(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun createToken(
        token: Token,
        maxToken: Int,
    )

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    )

    fun removeTokenByTokenValidation(tokenValidationInfo: TokenValidationInfo): Int
}