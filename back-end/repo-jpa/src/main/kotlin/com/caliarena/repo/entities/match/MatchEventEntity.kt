package com.caliarena.repo.entities.match

import com.caliarena.domain.match.MatchEvent
import com.caliarena.domain.match.MatchEventType
import com.caliarena.repo.entities.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "match_events")
class MatchEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var match: MatchEntity = MatchEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    var judge: UserEntity = UserEntity(),
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    var eventType: MatchEventType = MatchEventType.MATCH_STARTED,
    @Column(columnDefinition = "TEXT")
    var payload: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() =
        MatchEvent(
            id = id,
            matchId = match.id,
            judgeId = judge.id,
            eventType = eventType,
            payload = payload,
            createdAt = Instant.ofEpochSecond(createdAt),
        )

    companion object {
        fun MatchEvent.fromDomain(
            match: MatchEntity,
            judge: UserEntity,
        ) = MatchEventEntity(
            id = this.id,
            match = match,
            judge = judge,
            eventType = this.eventType,
            payload = this.payload,
            createdAt = this.createdAt.epochSecond,
        )
    }
}
