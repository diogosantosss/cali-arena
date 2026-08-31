package com.caliarena

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform