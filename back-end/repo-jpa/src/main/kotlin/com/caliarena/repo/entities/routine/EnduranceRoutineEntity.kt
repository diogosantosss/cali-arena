package com.caliarena.repo.entities.routine

import com.caliarena.domain.routine.EnduranceRoutine
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "endurance_routines")
class EnduranceRoutineEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @Column(nullable = false, length = 100)
    var name: String = "",
    @Column(name = "time_cap_seconds")
    var timeCapSeconds: Int? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() =
        EnduranceRoutine(
            id = id,
            name = name,
            timeCapSeconds = timeCapSeconds,
            createdAt = Instant.ofEpochSecond(createdAt),
        )

    companion object {
        fun EnduranceRoutine.fromDomain() =
            EnduranceRoutineEntity(
                id = this.id,
                name = this.name,
                timeCapSeconds = this.timeCapSeconds,
                createdAt = this.createdAt.epochSecond,
            )
    }
}
