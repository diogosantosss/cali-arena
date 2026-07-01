package com.caliarena.repo.jpa.routine

import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface EndRoutineRepositoryJpa : JpaRepository<EnduranceRoutineEntity, Long>
