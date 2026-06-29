package com.caliarena.repo

import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.repo.entities.UserEntity.Companion.fromDomain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class UserRepositoryTest {

//    @Autowired
//    lateinit var userRepository: UserRepository
//
//    @Test
//    fun `save and find user by id`() {
//        val user = User(
//            id = 0,
//            username = "joao",
//            password = PasswordValidationInfo(validationInfo = "hashed_password"),
//            role = UserRole.JUDGE,
//            createdAt = Instant.now(),
//        )
//
//        val saved = userRepository.save(user.fromDomain())
//
//        val found = userRepository.findById(saved.id).orElse(null)?.toDomain()
//
//        assertNotNull(found)
//        assertEquals(1, found?.id)
//        assertEquals("joao", found?.username)
//        assertEquals(UserRole.JUDGE, found?.role)
//    }
}