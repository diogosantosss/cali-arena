package com.caliarena.repo.jpa.match

import com.caliarena.repo.entities.match.MatchProgressEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchProgRepositoryJpa : JpaRepository<MatchProgressEntity, Long> {
    fun findByMatchId(matchId: Int): MatchProgressEntity?
}
