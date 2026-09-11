package com.caliarena.repo.entities.tournament

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.repo.entities.match.MatchEntity
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
@Table(name = "tournament_state")
class TournamentStateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    var tournament: TournamentEntity = TournamentEntity(),
    @Enumerated(EnumType.STRING)
    @Column(name = "current_screen", nullable = false, length = 20)
    var currentScreen: ScreenState = ScreenState.WAITING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_match_id")
    var currentMatch: MatchEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bracket_id")
    var currentBracket: BracketEntity? = null,
    @Column(name = "current_division", length = 40)
    var currentDivision: String? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0L,
) {
    fun toDomain() =
        TournamentState(
            id = id,
            tournamentId = tournament.id,
            currentScreen = currentScreen,
            currentMatchId = currentMatch?.id,
            currentBracketId = currentBracket?.id,
            currentDivision = currentDivision,
            updatedAt = Instant.ofEpochSecond(updatedAt),
        )

    companion object {
        fun TournamentState.fromDomain(
            tournament: TournamentEntity,
            currentMatch: MatchEntity?,
            currentBracket: BracketEntity?,
        ) = TournamentStateEntity(
            id = this.id,
            tournament = tournament,
            currentScreen = this.currentScreen,
            currentMatch = currentMatch,
            currentBracket = currentBracket,
            currentDivision = this.currentDivision,
            updatedAt = this.updatedAt.epochSecond,
        )
    }
}
