package com.caliarena.http.model.match

import java.time.Instant

enum class RepSide { RED, BLUE }

enum class JudgeOutputType { REPS, FINISHED, ERROR }

data class JudgeActionInput(
    val side: RepSide,
    val reps: Int? = null,
)

interface JudgeEvent {
    val type: JudgeOutputType
}

data class JudgeRepsEvent(
    override val type: JudgeOutputType = JudgeOutputType.REPS,
    val side: RepSide,
    val reps: Int,
    val exerciseId: Int?,
) : JudgeEvent

data class JudgeFinishedEvent(
    override val type: JudgeOutputType = JudgeOutputType.FINISHED,
    val side: RepSide,
    val finishedAt: Instant,
) : JudgeEvent

data class JudgeErrorEvent(
    override val type: JudgeOutputType = JudgeOutputType.ERROR,
    val message: String,
) : JudgeEvent
