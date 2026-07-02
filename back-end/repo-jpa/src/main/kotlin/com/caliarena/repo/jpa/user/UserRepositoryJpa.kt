package com.caliarena.repo.jpa.user

import com.caliarena.repo.entities.user.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepositoryJpa : JpaRepository<UserEntity, Int> {
    fun findByUsername(username: String): UserEntity?
}
