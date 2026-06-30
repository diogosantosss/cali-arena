package com.caliarena.repo.entities

import com.caliarena.domain.token.Token
import com.caliarena.domain.token.TokenValidationInfo
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tokens")
class TokenEntity(
    @Id
    @Column(name = "token_validation", length = 256)
    var tokenValidation: String = "",
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: UserEntity = UserEntity(),
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Long = 0L,
) {
    companion object {
        fun TokenEntity.toDomain() =
            Token(
                tokenValidationInfo = TokenValidationInfo(tokenValidation),
                userId = user.id,
                createdAt = Instant.ofEpochMilli(createdAt),
                lastUsedAt = Instant.ofEpochMilli(lastUsedAt),
            )
    }
}
