package com.caliarena.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class RepSide {
    RED,
    BLUE,
}

@Serializable
enum class JudgeActionType {
    ADJUST,
    FINISH,
}

@Serializable
enum class JudgeOutputType {
    STARTED,
    REPS,
    FINISHED,
    ERROR,
}

@Serializable
data class JudgeActionInput(
    val action: JudgeActionType,
    val side: RepSide,
    val reps: Int? = null,
)

sealed interface JudgeWsEvent

@Serializable
data class JudgeRepsEvent(
    val side: RepSide,
    val reps: Int,
    val exerciseId: Int? = null,
) : JudgeWsEvent

@Serializable
data class JudgeFinishedEvent(
    val side: RepSide,
    val finishedAt: String,
) : JudgeWsEvent

@Serializable
data class JudgeErrorEvent(
    val message: String,
) : JudgeWsEvent

data object UnhandledJudgeEvent : JudgeWsEvent

data object MatchConnectionLost : JudgeWsEvent

fun JudgeErrorEvent.toErrorCode(): ErrorCode = ErrorCode.fromType(message)

private const val TYPE_KEY = "type"

fun parseJudgeEvent(
    json: Json,
    body: String,
): JudgeWsEvent =
    try {
        val obj = json.parseToJsonElement(body).jsonObject
        when (obj[TYPE_KEY]?.jsonPrimitive?.contentOrNull) {
            JudgeOutputType.REPS.name ->
                json.decodeFromJsonElement(JudgeRepsEvent.serializer(), obj)

            JudgeOutputType.FINISHED.name ->
                json.decodeFromJsonElement(JudgeFinishedEvent.serializer(), obj)

            JudgeOutputType.ERROR.name ->
                json.decodeFromJsonElement(JudgeErrorEvent.serializer(), obj)

            else -> UnhandledJudgeEvent
        }
    } catch (_: Exception) {
        UnhandledJudgeEvent
    }
