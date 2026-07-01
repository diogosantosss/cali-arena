package com.caliarena.repo.entities.match

import com.caliarena.domain.match.MatchProgress
import com.caliarena.repo.entities.routine.ExerciseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "match_progress")
class MatchProgressEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true, nullable = false)
    var match: MatchEntity = MatchEntity(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "red_current_exercise_id")
    var redCurrentExercise: ExerciseEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blue_current_exercise_id")
    var blueCurrentExercise: ExerciseEntity? = null,
    @Column(name = "red_current_reps", nullable = false)
    var redCurrentReps: Int = 0,
    @Column(name = "blue_current_reps", nullable = false)
    var blueCurrentReps: Int = 0,
    @Column(name = "red_finished_at")
    var redFinishedAt: Long? = null,
    @Column(name = "blue_finished_at")
    var blueFinishedAt: Long? = null,
    @Column(name = "timer_started_at")
    var timerStartedAt: Long? = null,
    @Column(name = "timer_remaining_seconds")
    var timerRemainingSeconds: Int? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0L,
) {
    fun toDomain() =
        MatchProgress(
            id = id,
            matchId = match.id,
            redCurrentExerciseId = redCurrentExercise?.id,
            blueCurrentExerciseId = blueCurrentExercise?.id,
            redCurrentReps = redCurrentReps,
            blueCurrentReps = blueCurrentReps,
            redFinishedAt = redFinishedAt?.let { Instant.ofEpochSecond(it) },
            blueFinishedAt = blueFinishedAt?.let { Instant.ofEpochSecond(it) },
            timerStartedAt = timerStartedAt?.let { Instant.ofEpochSecond(it) },
            timerRemainingSeconds = timerRemainingSeconds,
            updatedAt = Instant.ofEpochSecond(updatedAt),
        )

    companion object {
        fun MatchProgress.fromDomain(
            match: MatchEntity,
            redCurrentExercise: ExerciseEntity?,
            blueCurrentExercise: ExerciseEntity?,
        ) = MatchProgressEntity(
            id = this.id,
            match = match,
            redCurrentExercise = redCurrentExercise,
            blueCurrentExercise = blueCurrentExercise,
            redCurrentReps = this.redCurrentReps,
            blueCurrentReps = this.blueCurrentReps,
            redFinishedAt = this.redFinishedAt?.epochSecond,
            blueFinishedAt = this.blueFinishedAt?.epochSecond,
            timerStartedAt = this.timerStartedAt?.epochSecond,
            timerRemainingSeconds = this.timerRemainingSeconds,
            updatedAt = this.updatedAt.epochSecond,
        )
    }
}
