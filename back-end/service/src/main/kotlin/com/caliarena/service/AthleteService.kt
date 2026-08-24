package com.caliarena.service

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.trx.TransactionManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock

sealed class AthleteError {
    data object AthleteNotFound : AthleteError()

    data object ClubNotFound : AthleteError()

    data object InvalidGender : AthleteError()

    data object CreatingAthlete : AthleteError()

    data object UpdatingAthlete : AthleteError()
}

@Service
class AthleteService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createAthlete(
        name: String,
        gender: String,
        clubId: Int,
    ): Either<AthleteError, Athlete> =
        trx.run {
            val club =
                clubs.findByIdOrNull(clubId)
                    ?: return@run failure(AthleteError.ClubNotFound)

            val genderType =
                GenderType.entries.find { it.name == gender }
                    ?: return@run failure(AthleteError.InvalidGender)

            val athlete =
                athletes.save(
                    AthleteEntity(
                        name = name,
                        gender = genderType,
                        club = club,
                        createdAt = clock.instant().epochSecond,
                    ),
                )

            success(athlete.toDomain())
        }

    fun getAthleteById(id: Int): Either<AthleteError, Athlete> =
        trx.run {
            val athlete =
                athletes.findByIdOrNull(id)
                    ?: return@run failure(AthleteError.AthleteNotFound)

            success(athlete.toDomain())
        }

    fun getAllAthletes(): List<Athlete> =
        trx.run {
            athletes.findAll().map(AthleteEntity::toDomain)
        }

    fun getAthletesByClub(clubId: Int): Either<AthleteError, List<Athlete>> =
        trx.run {
            clubs.findByIdOrNull(clubId)
                ?: return@run failure(AthleteError.ClubNotFound)

            success(athletes.findByClubId(clubId).map(AthleteEntity::toDomain))
        }

    fun getAthletesByGender(gender: String): Either<AthleteError, List<Athlete>> =
        trx.run {
            val genderType =
                GenderType.entries.find { it.name == gender }
                    ?: return@run failure(AthleteError.InvalidGender)

            success(athletes.findByGender(genderType).map(AthleteEntity::toDomain))
        }

    fun updateAthlete(
        id: Int,
        name: String,
        gender: String,
        clubId: Int,
    ): Either<AthleteError, Athlete> =
        trx.run {
            val existing =
                athletes.findByIdOrNull(id)
                    ?: return@run failure(AthleteError.AthleteNotFound)

            val genderType =
                GenderType.entries.find { it.name == gender }
                    ?: return@run failure(AthleteError.InvalidGender)

            val club =
                clubs.findByIdOrNull(clubId)
                    ?: return@run failure(AthleteError.ClubNotFound)

            existing.name = name
            existing.gender = genderType
            existing.club = club

            success(athletes.save(existing).toDomain())
        }
}
