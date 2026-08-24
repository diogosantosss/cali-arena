package com.caliarena.repo

import com.caliarena.repo.entities.user.UserEntity
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<UserEntity, Int> {
    fun findByUsername(username: String): UserEntity?
}
