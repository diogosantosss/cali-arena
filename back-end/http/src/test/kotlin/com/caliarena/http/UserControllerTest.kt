package com.caliarena.http

import com.caliarena.domain.token.TokenExternalInfo
import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.ProblemBody
import com.caliarena.http.model.user.CreateUserInput
import com.caliarena.http.model.user.UpdateRoleInput
import com.caliarena.http.model.user.UpdateRoleOutput
import com.caliarena.http.model.user.UserInfoOutput
import com.caliarena.http.model.user.UserLoginInput
import com.caliarena.http.model.user.UserLoginOutput
import com.caliarena.service.ApiError
import com.caliarena.service.UserAuthService
import com.caliarena.service.failure
import com.caliarena.service.success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class UserControllerTest {
    @Mock
    private lateinit var userService: UserAuthService

    private lateinit var controller: UserController

    private val now = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        controller = UserController(userService)
    }

    private fun user(
        id: Int = 1,
        username: String = "diogo",
        role: UserRole = UserRole.JUDGE,
    ) = User(id, username, PasswordValidationInfo("hash"), role, now)

    private fun authenticatedUser(
        user: User = user(id = 1, username = "admin", role = UserRole.ADMIN),
        token: String = "token",
    ) = AuthenticatedUser(user, token)

    private fun assertProblem(
        response: ResponseEntity<Any>,
        status: HttpStatus,
        problemType: String,
    ) {
        assertEquals(status, response.statusCode)
        assertEquals(PROBLEM_MEDIA_TYPE, response.headers.contentType?.toString())
        assertEquals(ProblemBody(type = "/problem/$problemType", title = problemType, status = status.value()), response.body)
    }

    @Nested
    inner class GetAllUsers {
        @Test
        fun `should get all users mapped to output`() {
            val users = listOf(user(id = 1, username = "admin", role = UserRole.ADMIN), user(id = 2, username = "judge"))
            whenever(userService.getUsers()).thenReturn(users)

            val response = controller.getAllUsers()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(
                listOf(
                    UserInfoOutput(1, "admin", UserRole.ADMIN, now),
                    UserInfoOutput(2, "judge", UserRole.JUDGE, now),
                ),
                response.body,
            )
        }
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `should create user successfully`() {
            val created = user(id = 1, username = "diogo")
            whenever(userService.createUser("diogo", "Password1")).thenReturn(success(created))

            val response = controller.createUser(CreateUserInput(username = "diogo", password = "Password1"))

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals("/api/users/1", response.headers.getFirst("Location"))
            assertEquals(UserInfoOutput(1, "diogo", UserRole.JUDGE, now), response.body)
            verify(userService).createUser("diogo", "Password1")
        }

        @Test
        fun `should return bad request when password is insecure`() {
            whenever(userService.createUser("diogo", "123")).thenReturn(failure(ApiError.INSECURE_PASSWORD))

            val response = controller.createUser(CreateUserInput(username = "diogo", password = "123"))

            assertProblem(response, HttpStatus.BAD_REQUEST, "insecure-password")
        }

        @Test
        fun `should return conflict when username is already used`() {
            whenever(userService.createUser("diogo", "Password1")).thenReturn(failure(ApiError.ALREADY_USED_USERNAME))

            val response = controller.createUser(CreateUserInput(username = "diogo", password = "Password1"))

            assertProblem(response, HttpStatus.CONFLICT, "already-used-username")
        }
    }

    @Nested
    inner class CreateToken {
        @Test
        fun `should create token successfully`() {
            val tokenInfo = TokenExternalInfo(tokenValue = "token-value", tokenExpiration = now)
            whenever(userService.createToken("diogo", "Password1")).thenReturn(success(tokenInfo))

            val response = controller.token(UserLoginInput(username = "diogo", password = "Password1"))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(UserLoginOutput("token-value"), response.body)
        }

        @Test
        fun `should return unauthorized when credentials are invalid`() {
            whenever(userService.createToken("diogo", "WrongPassword")).thenReturn(failure(ApiError.USER_OR_PASSWORD_ARE_INVALID))

            val response = controller.token(UserLoginInput(username = "diogo", password = "WrongPassword"))

            assertProblem(response, HttpStatus.UNAUTHORIZED, "user-or-password-are-invalid")
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `should revoke the token`() {
            whenever(userService.revokeToken("token")).thenReturn(true)

            controller.logout(authenticatedUser())

            verify(userService).revokeToken("token")
        }
    }

    @Nested
    inner class Me {
        @Test
        fun `should return current authenticated user`() {
            val authUser = authenticatedUser()

            val response = controller.me(authUser)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(UserInfoOutput(1, "admin", UserRole.ADMIN, now), response.body)
        }
    }

    @Nested
    inner class UpdateUserRole {
        @Test
        fun `should update user role successfully`() {
            val updated = user(id = 2, username = "judge", role = UserRole.ADMIN)
            whenever(userService.updateUserRole("token", 2, "ADMIN")).thenReturn(success(updated))

            val response = controller.updateUserRole(authenticatedUser(), UpdateRoleInput(userToUpdateId = 2, role = "ADMIN"))

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(UpdateRoleOutput(userId = 2, role = UserRole.ADMIN), response.body)
            verify(userService).updateUserRole("token", 2, "ADMIN")
        }

        @Test
        fun `should return not found when requester token is invalid`() {
            whenever(userService.updateUserRole("token", 2, "ADMIN")).thenReturn(failure(ApiError.USER_NOT_FOUND))

            val response = controller.updateUserRole(authenticatedUser(), UpdateRoleInput(userToUpdateId = 2, role = "ADMIN"))

            assertProblem(response, HttpStatus.NOT_FOUND, "user-not-found")
        }

        @Test
        fun `should return forbidden when requester is not admin`() {
            whenever(userService.updateUserRole("token", 2, "ADMIN")).thenReturn(failure(ApiError.NOT_AUTHORIZED))

            val response =
                controller.updateUserRole(
                    authenticatedUser(user = user(id = 1, username = "judge")),
                    UpdateRoleInput(userToUpdateId = 2, role = "ADMIN"),
                )

            assertProblem(response, HttpStatus.FORBIDDEN, "not-authorized")
        }

        @Test
        fun `should return bad request when role is invalid`() {
            whenever(userService.updateUserRole("token", 2, "INVALID")).thenReturn(failure(ApiError.INVALID_ROLE))

            val response = controller.updateUserRole(authenticatedUser(), UpdateRoleInput(userToUpdateId = 2, role = "INVALID"))

            assertProblem(response, HttpStatus.BAD_REQUEST, "invalid-role")
        }
    }
}
