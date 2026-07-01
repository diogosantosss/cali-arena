package com.caliarena.repo.entities.routine

import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
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
import java.math.BigDecimal

@Entity
@Table(name = "exercises")
class ExerciseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    var routine: EnduranceRoutineEntity = EnduranceRoutineEntity(),
    @Column(nullable = false, length = 100)
    var name: String = "",
    @Column(name = "target_reps", nullable = false)
    var targetReps: Int = 0,
    @Column(name = "added_weight", precision = 6, scale = 2)
    var addedWeight: BigDecimal? = null,
    @Column(name = "exercise_order", nullable = false)
    var exerciseOrder: Int = 0,
    @Column(name = "superset_order")
    var supersetOrder: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: ExerciseType = ExerciseType.NORMAL,
) {
    fun toDomain() =
        Exercise(
            id = id,
            routineId = routine.id,
            name = name,
            targetReps = targetReps,
            addedWeight = addedWeight,
            exerciseOrder = exerciseOrder,
            supersetOrder = supersetOrder,
            type = type,
        )

    companion object {
        fun Exercise.fromDomain(routine: EnduranceRoutineEntity) =
            ExerciseEntity(
                id = this.id,
                routine = routine,
                name = this.name,
                targetReps = this.targetReps,
                addedWeight = this.addedWeight,
                exerciseOrder = this.exerciseOrder,
                supersetOrder = this.supersetOrder,
                type = this.type,
            )
    }
}
