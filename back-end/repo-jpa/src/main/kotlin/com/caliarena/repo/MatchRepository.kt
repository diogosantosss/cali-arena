package com.caliarena.repo

import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.match.MatchEntity
import org.springframework.data.repository.CrudRepository

interface MatchRepository : CrudRepository<MatchEntity, Int> {
    fun findByBracketId(bracketId: Int): List<MatchEntity>

    fun findByStatus(status: MatchStatus): List<MatchEntity>

    fun findAllByJudgeId(judgeId: Int): List<MatchEntity>
}
