package com.caliarena.repo.entities

import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(nullable = false, unique = true, length = 64)
    var username: String = "",

    @Column(nullable = false, length = 256)
    var password: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.ADMIN,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() = User(
        id = id,
        username = username,
        password = PasswordValidationInfo(password),
        role = role,
        createdAt = Instant.ofEpochMilli(createdAt),
    )

    companion object {
        fun User.fromDomain() = UserEntity(
            username = this.username,
            password = this.password.validationInfo,
            role = this.role,
            createdAt = this.createdAt.toEpochMilli()
        )
    }
}
