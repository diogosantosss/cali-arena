package com.caliarena.repo.jpa.club

import com.caliarena.repo.entities.club.ClubEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ClubRepositoryJpa : JpaRepository<ClubEntity, Int> {
    fun findByName(name: String): ClubEntity?
}
