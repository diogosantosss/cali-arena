package com.caliarena

import com.caliarena.domain.club.Club
import java.time.Instant

interface RepositoryClub : Repository<Club> {

    fun createClub(
        name: String,
        shortName: String?,
        createdAt: Instant,
    ): Club

    fun findByName(name: String): Club?
}