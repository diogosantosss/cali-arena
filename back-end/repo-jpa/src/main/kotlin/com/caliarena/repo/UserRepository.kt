package com.caliarena.repo

import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.user.UserEntity
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<UserEntity, Int> {
    fun findByUsername(username: String): UserEntity?

    fun findAllByRole(role: UserRole): List<UserEntity>
}
