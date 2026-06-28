package com.caliarena.domain.match

import java.time.Instant

data class MatchEvent(
    val id: Int,
    val matchId: Int,
    val judgeId: Int,
    val eventType: MatchEventType,
    val payload: String?,
    val createdAt: Instant,
)
