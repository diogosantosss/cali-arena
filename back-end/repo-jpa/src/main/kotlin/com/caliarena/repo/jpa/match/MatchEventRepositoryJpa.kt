package com.caliarena.repo.jpa.match

import com.caliarena.repo.entities.match.MatchEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchEventRepositoryJpa : JpaRepository<MatchEventEntity, Long>
