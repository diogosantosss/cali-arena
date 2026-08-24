package com.caliarena.repo

import com.caliarena.repo.entities.club.ClubEntity
import org.springframework.data.repository.CrudRepository

interface ClubRepository : CrudRepository<ClubEntity, Int> {
    fun findByName(name: String): ClubEntity?
}
