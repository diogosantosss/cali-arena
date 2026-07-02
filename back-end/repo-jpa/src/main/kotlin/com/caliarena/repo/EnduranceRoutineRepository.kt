package com.caliarena.repo

import com.caliarena.RepositoryEnduranceRoutine
import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity.Companion.fromDomain
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.jpa.routine.EndRoutineRepositoryJpa
import com.caliarena.repo.jpa.routine.ExerciseRepositoryJpa
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
class EnduranceRoutineRepository(
    private val routineJpa: EndRoutineRepositoryJpa,
    private val exerciseJpa: ExerciseRepositoryJpa,
) : RepositoryEnduranceRoutine {
    override fun createRoutine(
        name: String,
        timeCapSeconds: Int?,
        createdAt: Instant,
    ): EnduranceRoutine =
        routineJpa
            .save(
                EnduranceRoutineEntity(
                    name = name,
                    timeCapSeconds = timeCapSeconds,
                    createdAt = createdAt.epochSecond,
                ),
            ).toDomain()

    override fun findByName(name: String): EnduranceRoutine? = routineJpa.findByName(name)?.toDomain()

    override fun createExercise(
        routineId: Int,
        name: String,
        targetReps: Int,
        addedWeight: BigDecimal?,
        exerciseOrder: Int,
        supersetOrder: Int?,
        type: ExerciseType,
    ): Exercise? {
        val routine = routineJpa.findByIdOrNull(routineId) ?: return null
        return exerciseJpa
            .save(
                ExerciseEntity(
                    routine = routine,
                    name = name,
                    targetReps = targetReps,
                    addedWeight = addedWeight,
                    exerciseOrder = exerciseOrder,
                    supersetOrder = supersetOrder,
                    type = type,
                ),
            ).toDomain()
    }

    override fun findExercisesByRoutineId(routineId: Int): List<Exercise> =
        exerciseJpa.findExercisesByRoutineId(routineId).map(ExerciseEntity::toDomain)

    override fun findById(id: Int): EnduranceRoutine? = routineJpa.findByIdOrNull(id)?.toDomain()

    override fun findAll(): List<EnduranceRoutine> = routineJpa.findAll().map { it.toDomain() }

    override fun save(entity: EnduranceRoutine): EnduranceRoutine? = routineJpa.save(entity.fromDomain()).toDomain()

    override fun deleteById(id: Int) = routineJpa.deleteById(id)

    override fun clear() {
        exerciseJpa.deleteAll()
        routineJpa.deleteAll()
    }
}
