package com.caliarena.repo.jpa.match

import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.match.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MatchRepositoryJpa : JpaRepository<MatchEntity, Int> {
    fun findByBracketId(bracketId: Int): List<MatchEntity>

    fun findByStatus(status: MatchStatus): List<MatchEntity>
}
