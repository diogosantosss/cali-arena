package com.caliarena.repo.entities.athlete

import com.caliarena.domain.athlete.Athlete
import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.club.ClubEntity
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
@Table(name = "athletes")
class AthleteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    @Column(nullable = false)
    val name: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val gender: GenderType = GenderType.MALE,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    val club: ClubEntity = ClubEntity(),
    @Column(name = "created_at", nullable = false)
    val createdAt: Long = 0L,
) {
    fun toDomain() =
        Athlete(
            id = id,
            name = name,
            gender = gender,
            clubId = club.id,
            createdAt = Instant.ofEpochSecond(createdAt),
        )

    companion object {
        fun Athlete.fromDomain(club: ClubEntity) =
            AthleteEntity(
                id = this.id,
                name = this.name,
                gender = this.gender,
                club = club,
                createdAt = this.createdAt.epochSecond,
            )
    }
}
