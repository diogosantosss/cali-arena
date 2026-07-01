package com.caliarena.repo.jpa.tournament

import com.caliarena.repo.entities.tournament.TournamentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentRepositoryJpa : JpaRepository<TournamentEntity, Long>
