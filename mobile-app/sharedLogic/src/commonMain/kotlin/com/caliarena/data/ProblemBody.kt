package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class ProblemBody(
    val type: String,
    val title: String,
    val status: Int,
)
