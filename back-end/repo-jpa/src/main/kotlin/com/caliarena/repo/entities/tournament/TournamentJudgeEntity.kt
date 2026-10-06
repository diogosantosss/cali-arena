package com.caliarena.repo.entities.tournament

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table

/**
 * Judges assigned to a tournament. Only these users (plus the tournament host and admins)
 * are allowed to drive the tournament matches.
 */
@Entity
@IdClass(TournamentJudgeId::class)
@Table(name = "tournament_judges")
class TournamentJudgeEntity(
    @Id
    @Column(name = "tournament_id", nullable = false)
    var tournamentId: Int = 0,
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: Int = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
)

data class TournamentJudgeId(
    var tournamentId: Int = 0,
    var userId: Int = 0,
)
