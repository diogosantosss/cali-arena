package com.caliarena.repo.entities.tournament

import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tournaments")
class TournamentEntity(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Int = 0,
    @Column(nullable = false, length = 100)
    var name: String = "",
    @Column(length = 100)
    var location: String? = null,
    @Column(name = "start_date")
    var startDate: Long? = null,
    @Column(name = "end_date")
    var endDate: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TournamentStatus = TournamentStatus.DRAFT,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() =
        Tournament(
            id = id,
            name = name,
            location = location,
            startDate = startDate?.let { Instant.ofEpochSecond(it) },
            endDate = endDate?.let { Instant.ofEpochSecond(it) },
            status = status,
            createdAt = Instant.ofEpochSecond(createdAt),
        )

    companion object {
        fun Tournament.fromDomain() =
            TournamentEntity(
                id = this.id,
                name = this.name,
                location = this.location,
                startDate = this.startDate?.epochSecond,
                endDate = this.endDate?.epochSecond,
                status = this.status,
                createdAt = this.createdAt.epochSecond,
            )
    }
}
