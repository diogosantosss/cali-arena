package com.caliarena

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import java.time.Instant

interface RepositoryAthlete : Repository<Athlete> {

    fun createAthlete(name: String, gender: GenderType, clubId: Int, createdAt: Instant): Athlete?

    fun findByClubId(clubId: Int): List<Athlete>

    fun findByGender(gender: GenderType): List<Athlete>
}
