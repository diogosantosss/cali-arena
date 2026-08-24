package com.caliarena.repo

import com.caliarena.repo.entities.match.MatchProgressEntity
import org.springframework.data.repository.CrudRepository

interface MatchProgressRepository : CrudRepository<MatchProgressEntity, Int> {
    fun findByMatchId(matchId: Int): MatchProgressEntity?
}
