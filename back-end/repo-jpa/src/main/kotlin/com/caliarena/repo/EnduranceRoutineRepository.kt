package com.caliarena.repo

import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import org.springframework.data.repository.CrudRepository

interface EnduranceRoutineRepository : CrudRepository<EnduranceRoutineEntity, Int> {
    fun findByName(name: String): EnduranceRoutineEntity?
}
