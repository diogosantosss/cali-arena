package com.caliarena.service

import com.caliarena.domain.club.Club
import com.caliarena.repo.entities.club.ClubEntity
import com.caliarena.repo.trx.TransactionManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class ClubService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createClub(
        name: String,
        shortName: String?,
    ): Either<ApiError, Club> =
        trx.run {
            if (clubs.findByName(name) != null) {
                return@run failure(ApiError.CLUB_ALREADY_EXISTS)
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

    fun getClubById(id: Int): Either<ApiError, Club> =
        trx.run {
            val club =
                clubs.findByIdOrNull(id)
                    ?: return@run failure(ApiError.CLUB_NOT_FOUND)

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
    ): Either<ApiError, Club> =
        trx.run {
            val existing =
                clubs.findByIdOrNull(id)
                    ?: return@run failure(ApiError.CLUB_NOT_FOUND)

            val nameConflict = clubs.findByName(name)
            if (nameConflict != null && nameConflict.id != id) {
                return@run failure(ApiError.CLUB_ALREADY_EXISTS)
            }

            existing.name = name
            existing.shortName = shortName

            success(clubs.save(existing).toDomain())
        }
}
