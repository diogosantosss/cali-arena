package com.caliarena.service

import com.caliarena.domain.club.Club
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.trx.TransactionManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock

sealed class ClubError {
    data object ClubAlreadyExists : ClubError()

    data object ClubNotFound : ClubError()

    data object UpdatingClub : ClubError()
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
            if (clubs.findByName(name) != null) {
                return@run failure(ClubError.ClubAlreadyExists)
            }

            val club =
                clubs.save(
                    ClubEntity(
                        name = name,
                        shortName = shortName,
                        createdAt = clock.instant().epochSecond,
                    ),
                )

            success(club.toDomain())
        }

    fun getClubById(id: Int): Either<ClubError, Club> =
        trx.run {
            val club =
                clubs.findByIdOrNull(id)
                    ?: return@run failure(ClubError.ClubNotFound)

            success(club.toDomain())
        }

    fun getAllClubs(): List<Club> =
        trx.run {
            clubs.findAll().map(ClubEntity::toDomain)
        }

    fun updateClub(
        id: Int,
        name: String,
        shortName: String?,
    ): Either<ClubError, Club> =
        trx.run {
            val existing =
                clubs.findByIdOrNull(id)
                    ?: return@run failure(ClubError.ClubNotFound)

            val nameConflict = clubs.findByName(name)
            if (nameConflict != null && nameConflict.id != id) {
                return@run failure(ClubError.ClubAlreadyExists)
            }

            existing.name = name
            existing.shortName = shortName

            success(clubs.save(existing).toDomain())
        }
}
