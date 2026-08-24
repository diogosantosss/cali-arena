package com.caliarena.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import org.springframework.data.repository.CrudRepository

interface AthleteRepository : CrudRepository<AthleteEntity, Int> {
    fun findByClubId(clubId: Int): List<AthleteEntity>

    fun findByGender(gender: GenderType): List<AthleteEntity>
}
