package com.caliarena.repo.jpa.tournament

import com.caliarena.repo.entities.tournament.TournamentStateEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentStateRepositoryJpa : JpaRepository<TournamentStateEntity, Long>
