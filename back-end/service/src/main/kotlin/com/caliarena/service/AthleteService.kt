package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
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
            repoClub.findById(clubId)
                ?: return@run failure(AthleteError.ClubNotFound)

            GenderType.entries.find { it.name == gender }
                ?: return@run failure(AthleteError.InvalidGender)

            val athlete =
                repoAthlete.createAthlete(
                    name = name,
                    gender = GenderType.valueOf(gender),
                    clubId = clubId,
                    createdAt = clock.instant(),
                ) ?: return@run failure(AthleteError.CreatingAthlete)

            success(athlete)
        }

    fun getAthleteById(id: Int): Either<AthleteError, Athlete> =
        trx.run {
            val athlete =
                repoAthlete.findById(id)
                    ?: return@run failure(AthleteError.AthleteNotFound)

            success(athlete)
        }

    fun getAllAthletes(): List<Athlete> =
        trx.run {
            repoAthlete.findAll()
        }

    fun getAthletesByClub(clubId: Int): Either<AthleteError, List<Athlete>> =
        trx.run {
            repoClub.findById(clubId)
                ?: return@run failure(AthleteError.ClubNotFound)

            success(repoAthlete.findByClubId(clubId))
        }

    fun getAthletesByGender(gender: GenderType): List<Athlete> =
        trx.run {
            repoAthlete.findByGender(gender)
        }

    fun updateAthlete(
        id: Int,
        name: String,
        gender: GenderType,
        clubId: Int,
    ): Either<AthleteError, Athlete> =
        trx.run {
            val existing =
                repoAthlete.findById(id)
                    ?: return@run failure(AthleteError.AthleteNotFound)

            repoClub.findById(clubId)
                ?: return@run failure(AthleteError.ClubNotFound)

            val updated =
                repoAthlete.save(
                    existing.copy(name = name, gender = gender, clubId = clubId),
                ) ?: return@run failure(AthleteError.UpdatingAthlete)

            success(updated)
        }
}
