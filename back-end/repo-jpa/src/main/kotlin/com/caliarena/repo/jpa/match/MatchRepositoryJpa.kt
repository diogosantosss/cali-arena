package com.caliarena.repo.jpa.match

import com.caliarena.repo.entities.match.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchRepositoryJpa : JpaRepository<MatchEntity, Long>
