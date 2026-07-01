package com.caliarena.repo.jpa.tournament

import com.caliarena.repo.entities.tournament.BracketEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BracketRepositoryJpa : JpaRepository<BracketEntity, Long>
