package com.caliarena.repo.entities.match

import com.caliarena.domain.match.Match
import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.tournament.BracketEntity
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
@Table(name = "matches")
class MatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bracket_id", nullable = false)
    var bracket: BracketEntity = BracketEntity(),
    @Column(name = "routine_id", nullable = false)
    var routineId: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    var judge: UserEntity = UserEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_red_id")
    var athleteRed: AthleteEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_blue_id")
    var athleteBlue: AthleteEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_athlete_id")
    var winnerAthlete: AthleteEntity? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchStatus = MatchStatus.PENDING,
    @Column(name = "started_at")
    var startedAt: Long? = null,
    @Column(name = "finished_at")
    var finishedAt: Long? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0L,
) {
    fun toDomain() =
        Match(
            id = id,
            bracketId = bracket.id,
            routineId = routineId,
            judgeId = judge.id,
            athleteRedId = athleteRed?.id,
            athleteBlueId = athleteBlue?.id,
            winnerAthleteId = winnerAthlete?.id,
            status = status,
            startedAt = startedAt?.let { Instant.ofEpochSecond(it) },
            finishedAt = finishedAt?.let { Instant.ofEpochSecond(it) },
            createdAt = Instant.ofEpochSecond(createdAt),
        )

    companion object {
        fun Match.fromDomain(
            bracket: BracketEntity,
            athleteRed: AthleteEntity?,
            athleteBlue: AthleteEntity?,
            judge: UserEntity,
            winnerAthlete: AthleteEntity?,
        ) = MatchEntity(
            id = this.id,
            bracket = bracket,
            routineId = this.routineId,
            judge = judge,
            athleteRed = athleteRed,
            athleteBlue = athleteBlue,
            winnerAthlete = winnerAthlete,
            status = this.status,
            startedAt = this.startedAt?.epochSecond,
            finishedAt = this.finishedAt?.epochSecond,
            createdAt = this.createdAt.epochSecond,
        )
    }
}
