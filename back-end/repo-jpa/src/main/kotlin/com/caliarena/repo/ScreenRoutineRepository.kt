package com.caliarena.repo

import com.caliarena.RepoScreenRoutine
import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.repo.entities.routine.ScreenRoutineEntity
import com.caliarena.repo.jpa.routine.ScreenRoutineRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ScreenRoutineRepository(
    private val jpa: ScreenRoutineRepositoryJpa,
) : RepoScreenRoutine {
    override fun findByTournamentId(tournamentId: Int): List<ScreenRoutine> =
        jpa.findByTournamentIdOrderByDisplayOrder(tournamentId).map { it.toDomain() }

    override fun findById(id: Int): ScreenRoutine? = jpa.findByIdOrNull(id)?.toDomain()

    override fun create(
        tournamentId: Int,
        routineId: Int,
        displayOrder: Int,
        label: String?,
        now: Instant,
    ): ScreenRoutine =
        jpa
            .save(
                ScreenRoutineEntity(
                    tournamentId = tournamentId,
                    routineId = routineId,
                    displayOrder = displayOrder,
                    label = label,
                    createdAt = now.epochSecond,
                    updatedAt = now.epochSecond,
                ),
            ).toDomain()

    override fun update(
        id: Int,
        isVisible: Boolean?,
        displayOrder: Int?,
        label: String?,
        now: Instant,
    ): ScreenRoutine? {
        val entity = jpa.findByIdOrNull(id) ?: return null
        isVisible?.let { entity.isVisible = it }
        displayOrder?.let { entity.displayOrder = it }
        label?.let { entity.label = it }
        entity.updatedAt = now.epochSecond
        return jpa.save(entity).toDomain()
    }

    override fun findAll(): List<ScreenRoutine> = jpa.findAll().map { it.toDomain() }

    override fun save(entity: ScreenRoutine): ScreenRoutine? {
        val now = System.currentTimeMillis()
        return jpa
            .save(
                ScreenRoutineEntity(
                    id = entity.id,
                    tournamentId = entity.tournamentId,
                    routineId = entity.routineId,
                    displayOrder = entity.displayOrder,
                    isVisible = entity.isVisible,
                    label = entity.label,
                    createdAt = entity.createdAt,
                    updatedAt = now,
                ),
            ).toDomain()
    }

    override fun deleteById(id: Int) = jpa.deleteById(id)

    override fun clear() = jpa.deleteAll()
}
