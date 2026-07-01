package com.caliarena.repo.entities.club

import com.caliarena.domain.club.Club
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "clubs")
class ClubEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @Column(nullable = false, unique = true, length = 100)
    var name: String = "",
    @Column(name = "short_name", length = 20)
    var shortName: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() =
        Club(
            id = this.id,
            name = this.name,
            shortName = this.shortName,
            createdAt = Instant.ofEpochSecond(this.createdAt),
        )

    companion object {
        fun Club.fromDomain() =
            ClubEntity(
                id = this.id,
                name = this.name,
                shortName = this.shortName,
                createdAt = this.createdAt.epochSecond,
            )
    }
}
