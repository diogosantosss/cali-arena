package com.caliarena.repo.jpa.athlete

import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AthleteRepositoryJpa : JpaRepository<AthleteEntity, Int> {
    fun findByClubId(clubId: Int): List<AthleteEntity>

    fun findByGender(gender: GenderType): List<AthleteEntity>
}
