package com.caliarena.repo

import com.caliarena.RepositoryClub
import com.caliarena.domain.club.Club
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.entities.club.ClubEntity.Companion.fromDomain
import com.caliarena.repo.jpa.club.ClubRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ClubRepository(
    private val clubRepositoryJpa: ClubRepositoryJpa,
) : RepositoryClub {
    override fun createClub(
        name: String,
        shortName: String?,
        createdAt: Instant,
    ): Club =
        clubRepositoryJpa
            .save(
                ClubEntity(
                    name = name,
                    shortName = shortName,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()

    override fun findByName(name: String): Club? = clubRepositoryJpa.findByName(name)?.toDomain()

    override fun findById(id: Int): Club? = clubRepositoryJpa.findByIdOrNull(id)?.toDomain()

    override fun findAll(): List<Club> = clubRepositoryJpa.findAll().map(ClubEntity::toDomain)

    override fun save(entity: Club): Club? = clubRepositoryJpa.save(entity.fromDomain()).toDomain()

    override fun deleteById(id: Int) = clubRepositoryJpa.deleteById(id)

    override fun clear() = clubRepositoryJpa.deleteAll()
}
