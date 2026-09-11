package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class ClubOutput(
    val id: Int,
    val name: String,
    val shortName: String? = null,
    val createdAt: String,
)
