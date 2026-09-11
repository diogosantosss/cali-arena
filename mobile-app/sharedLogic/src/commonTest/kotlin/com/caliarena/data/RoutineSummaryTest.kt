package com.caliarena.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoutineSummaryTest {
    @Test
    fun abbreviationsUseInitials() {
        assertEquals("MU", muscleUps.abbreviation)
        assertEquals("SBD", straightBarDips.abbreviation)
        assertEquals("PU", pullUps.abbreviation)
        assertEquals("LBPU", lowBarPushUps.abbreviation)
        assertEquals("SQ", squats.abbreviation)
    }

    @Test
    fun addedWeightLabelFormatsWeight() {
        assertNull(squatsNoWeight.addedWeightLabel)
        assertEquals("+20 kg", squats.addedWeightLabel)
        assertEquals("+7.5 kg", cuppedPullUps(7.5).addedWeightLabel)
    }

    @Test
    fun unbrokenExercisesAreLabeled() {
        assertEquals("5 Muscle-Ups unbroken", unbrokenMuscleUps.summaryLabel)
        assertEquals("10 Pull-Ups +7.5 kg", cuppedPullUps(7.5).summaryLabel)
    }

    @Test
    fun supersetMembersUseAbbreviations() {
        assertEquals("1 MU", muscleUps.summaryLabel)
        assertEquals("10 SBD", straightBarDips.summaryLabel)
    }

    @Test
    fun exerciseGroupsGroupSupersetsByExerciseOrder() {
        val routine = quarterfinalsSampleRoutine()

        val groups = routine.exerciseGroups

        assertEquals(4, groups.size)
        assertEquals(listOf(1, 2, 3, 4), groups.map { it.first().exerciseOrder })

        val superset = groups[0]
        assertEquals(listOf(0, 1, 2), superset.map { it.supersetOrder })
    }

    @Test
    fun exerciseGroupsSummaryJoinsSupersets() {
        assertEquals(
            listOf(
                "1 MU - 10 SBD - 10 PU",
                "20 Low-Bar Push-Ups",
                "20 Squats +20 kg",
                "5 Muscle-Ups unbroken",
            ),
            quarterfinalsSampleRoutine().exerciseGroupsSummary,
        )
    }

    private fun quarterfinalsSampleRoutine() =
        RoutineOverviewOutput(
            name = "Quarterfinals (MEN) ELITE",
            timeCapSeconds = 720,
            createdAt = "2026-08-21T16:04:36Z",
            exercises =
                listOf(
                    muscleUps,
                    straightBarDips,
                    pullUps,
                    lowBarPushUps,
                    squats,
                    unbrokenMuscleUps,
                ),
        )

    private val muscleUps = exercise("Muscle-Ups", targetReps = 1, exerciseOrder = 1, supersetOrder = 0)
    private val straightBarDips = exercise("Straight-Bar-Dips", targetReps = 10, exerciseOrder = 1, supersetOrder = 1)
    private val pullUps = exercise("Pull-Ups", targetReps = 10, exerciseOrder = 1, supersetOrder = 2)
    private val lowBarPushUps = exercise("Low-Bar Push-Ups", targetReps = 20, exerciseOrder = 2)
    private val squats = exercise("Squats", targetReps = 20, addedWeight = 20.0, exerciseOrder = 3)
    private val squatsNoWeight = exercise("Squats", targetReps = 20, exerciseOrder = 3)
    private val unbrokenMuscleUps =
        exercise("Muscle-Ups", targetReps = 5, exerciseOrder = 4, type = ExerciseType.UNBROKEN)

    private fun cuppedPullUps(weight: Double) =
        exercise("Pull-Ups", targetReps = 10, addedWeight = weight, exerciseOrder = 9)

    private fun exercise(
        name: String,
        targetReps: Int,
        addedWeight: Double? = null,
        exerciseOrder: Int,
        supersetOrder: Int? = null,
        type: ExerciseType = if (supersetOrder != null) ExerciseType.SUPERSET else ExerciseType.NORMAL,
    ) = ExerciseOutput(
        id = 0,
        routineId = 0,
        name = name,
        targetReps = targetReps,
        addedWeight = addedWeight,
        exerciseOrder = exerciseOrder,
        supersetOrder = supersetOrder,
        type = type,
    )
}
