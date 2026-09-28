package com.caliarena.repo

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestContainersExample : AbstractRepositoryTest() {
    @Test
    fun establishConnection() {
        assertTrue(postgres.isCreated)
        assertTrue(postgres.isRunning)
    }
}
