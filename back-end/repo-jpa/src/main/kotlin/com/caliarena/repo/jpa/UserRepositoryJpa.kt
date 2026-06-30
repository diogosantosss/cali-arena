package com.caliarena.repo.jpa

import com.caliarena.repo.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepositoryJpa : JpaRepository<UserEntity, Int> {
    fun findByUsername(username: String): UserEntity?
}
