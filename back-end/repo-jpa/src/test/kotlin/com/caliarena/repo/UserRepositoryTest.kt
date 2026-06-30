package com.caliarena.repo

import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestConfiguration::class])
@Transactional
class UserRepositoryTest {
    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `should have 3 users from insert test data`() {
        val users = userRepository.findAll()
        users.forEach { println(it) }
        assertEquals(3, users.size)
    }
}
