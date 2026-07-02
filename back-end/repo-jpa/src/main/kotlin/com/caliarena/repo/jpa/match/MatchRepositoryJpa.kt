package com.caliarena.repo.jpa.match

import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.match.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MatchRepositoryJpa : JpaRepository<MatchEntity, Int> {
    fun findByBracketId(bracketId: Int): List<MatchEntity>

    fun findByStatus(status: MatchStatus): List<MatchEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "UPDATE matches SET red_from_match_id = NULL, blue_from_match_id = NULL",
        nativeQuery = true,
    )
    fun clearFromMatchReferences()
}
