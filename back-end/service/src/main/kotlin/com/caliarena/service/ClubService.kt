package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.club.Club
import org.springframework.stereotype.Service
import java.time.Clock

sealed class ClubError {
    data object ClubAlreadyExists : ClubError()

    data object ClubNotFound : ClubError()

    data object CreatingClub : ClubError()
}

@Service
class ClubService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createClub(
        name: String,
        shortName: String?,
    ): Either<ClubError, Club> =
        trx.run {
            if (repoClub.findByName(name) != null) {
                return@run failure(ClubError.ClubAlreadyExists)
            }

            val club =
                repoClub.createClub(
                    name = name,
                    shortName = shortName,
                    createdAt = clock.instant(),
                )

            success(club)
        }

    fun getClubById(id: Int): Either<ClubError, Club> =
        trx.run {
            val club =
                repoClub.findById(id)
                    ?: return@run failure(ClubError.ClubNotFound)

            success(club)
        }

    fun getAllClubs(): List<Club> =
        trx.run {
            repoClub.findAll()
        }

    fun updateClub(
        id: Int,
        name: String,
        shortName: String?,
    ): Either<ClubError, Club> =
        trx.run {
            val existing =
                repoClub.findById(id)
                    ?: return@run failure(ClubError.ClubNotFound)

            val nameConflict = repoClub.findByName(name)
            if (nameConflict != null && nameConflict.id != id) {
                return@run failure(ClubError.ClubAlreadyExists)
            }

            val updated =
                repoClub.save(existing.copy(name = name, shortName = shortName))
                    ?: return@run failure(ClubError.CreatingClub)

            success(updated)
        }
}
