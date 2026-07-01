package com.caliarena.repo.entities.tournament

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketStage
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
@Table(name = "brackets")
class BracketEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    var tournament: TournamentEntity = TournamentEntity(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var gender: GenderType = GenderType.MALE,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var stage: BracketStage = BracketStage.QUALIFIERS,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() =
        Bracket(
            id = id,
            tournamentId = tournament.id,
            gender = gender,
            stage = stage,
            createdAt = Instant.ofEpochSecond(createdAt),
        )

    companion object {
        fun Bracket.fromDomain(tournament: TournamentEntity) =
            BracketEntity(
                id = this.id,
                tournament = tournament,
                gender = this.gender,
                stage = this.stage,
                createdAt = this.createdAt.epochSecond,
            )
    }
}
