package com.caliarena.repo.jpa.athlete

import com.caliarena.repo.entities.athlete.AthleteEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AthleteRepositoryJpa : JpaRepository<AthleteEntity, Long>
