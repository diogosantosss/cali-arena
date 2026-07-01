package com.caliarena.repo

import com.caliarena.RepositoryAthlete
import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.athlete.AthleteEntity.Companion.fromDomain
import com.caliarena.repo.jpa.athlete.AthleteRepositoryJpa
import com.caliarena.repo.jpa.club.ClubRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class AthleteRepository(
    private val athleteRepositoryJpa: AthleteRepositoryJpa,
    private val clubRepositoryJpa: ClubRepositoryJpa,
) : RepositoryAthlete {
    override fun createAthlete(
        name: String,
        gender: GenderType,
        clubId: Int,
        createdAt: Instant,
    ): Athlete? {
        val club = clubRepositoryJpa.findByIdOrNull(clubId) ?: return null
        return athleteRepositoryJpa
            .save(
                AthleteEntity(
                    name = name,
                    gender = gender,
                    club = club,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()
    }

    override fun findByClubId(clubId: Int): List<Athlete> = athleteRepositoryJpa.findByClubId(clubId).map(AthleteEntity::toDomain)

    override fun findByGender(gender: GenderType): List<Athlete> = athleteRepositoryJpa.findByGender(gender).map(AthleteEntity::toDomain)

    override fun findById(id: Int): Athlete? = athleteRepositoryJpa.findByIdOrNull(id)?.toDomain()

    override fun findAll(): List<Athlete> = athleteRepositoryJpa.findAll().map(AthleteEntity::toDomain)

    override fun save(entity: Athlete): Athlete? {
        val club = clubRepositoryJpa.findByIdOrNull(entity.clubId) ?: return null
        return athleteRepositoryJpa.save(entity.fromDomain(club)).toDomain()
    }

    override fun deleteById(id: Int) = athleteRepositoryJpa.deleteById(id)

    override fun clear() = athleteRepositoryJpa.deleteAll()
}
