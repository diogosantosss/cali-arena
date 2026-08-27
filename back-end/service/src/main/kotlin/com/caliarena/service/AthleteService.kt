package com.caliarena.service

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.trx.TransactionManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class AthleteService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createAthlete(
        name: String,
        gender: String,
        clubId: Int,
    ): Either<ApiError, Athlete> =
        trx.run {
            val club =
                clubs.findByIdOrNull(clubId)
                    ?: return@run failure(ApiError.CLUB_NOT_FOUND)

            val genderType =
                GenderType.entries.find { it.name == gender }
                    ?: return@run failure(ApiError.INVALID_GENDER)

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

    fun getAthleteById(id: Int): Either<ApiError, Athlete> =
        trx.run {
            val athlete =
                athletes.findByIdOrNull(id)
                    ?: return@run failure(ApiError.ATHLETE_NOT_FOUND)

            success(athlete.toDomain())
        }

    fun getAllAthletes(): List<Athlete> =
        trx.run {
            athletes.findAll().map(AthleteEntity::toDomain)
        }

    fun getAthletesByClub(clubId: Int): Either<ApiError, List<Athlete>> =
        trx.run {
            clubs.findByIdOrNull(clubId)
                ?: return@run failure(ApiError.CLUB_NOT_FOUND)

            success(athletes.findByClubId(clubId).map(AthleteEntity::toDomain))
        }

    fun getAthletesByGender(gender: String): Either<ApiError, List<Athlete>> =
        trx.run {
            val genderType =
                GenderType.entries.find { it.name == gender }
                    ?: return@run failure(ApiError.INVALID_GENDER)

            success(athletes.findByGender(genderType).map(AthleteEntity::toDomain))
        }

    fun updateAthlete(
        id: Int,
        name: String,
        gender: String,
        clubId: Int,
    ): Either<ApiError, Athlete> =
        trx.run {
            val existing =
                athletes.findByIdOrNull(id)
                    ?: return@run failure(ApiError.ATHLETE_NOT_FOUND)

            val genderType =
                GenderType.entries.find { it.name == gender }
                    ?: return@run failure(ApiError.INVALID_GENDER)

            val club =
                clubs.findByIdOrNull(clubId)
                    ?: return@run failure(ApiError.CLUB_NOT_FOUND)

            existing.name = name
            existing.gender = genderType
            existing.club = club

            success(athletes.save(existing).toDomain())
        }
}
