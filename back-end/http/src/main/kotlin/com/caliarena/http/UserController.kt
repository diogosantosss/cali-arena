package com.caliarena.http

import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole
import com.caliarena.http.model.toResponseEntity
import com.caliarena.http.model.user.CreateUserInput
import com.caliarena.http.model.user.UpdateRoleInput
import com.caliarena.http.model.user.UpdateRoleOutput
import com.caliarena.http.model.user.UserInfoOutput
import com.caliarena.http.model.user.UserLoginInput
import com.caliarena.http.model.user.UserLoginOutput
import com.caliarena.http.utils.hasAnyRole
import com.caliarena.http.utils.toResponse
import com.caliarena.service.ApiError
import com.caliarena.service.UserAuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserAuthService,
) {
    @GetMapping
    fun getAllUsers(user: AuthenticatedUser): ResponseEntity<Any> {
        if (!user.hasAnyRole(UserRole.ADMIN)) {
            return ApiError.NOT_AUTHORIZED.toResponseEntity()
        }
        return ResponseEntity.ok(
            userService
                .getUsers()
                .map { u ->
                    UserInfoOutput(u.id, u.username, u.role, u.createdAt)
                },
        )
    }

    @PostMapping
    fun createUser(
        @RequestBody userInput: CreateUserInput,
    ): ResponseEntity<Any> =
        userService
            .createUser(userInput.username, userInput.password)
            .toResponse(
                onSuccess = { user ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("Location", "/api/users/${user.id}")
                        .body(UserInfoOutput(user.id, user.username, user.role, user.createdAt))
                },
                onError = { it.toResponseEntity() },
            )

    @PostMapping("/token")
    fun token(
        @RequestBody input: UserLoginInput,
    ): ResponseEntity<Any> =
        userService
            .createToken(input.username, input.password)
            .toResponse(
                onSuccess = { tokenInfo ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(UserLoginOutput(tokenInfo.tokenValue))
                },
                onError = { it.toResponseEntity() },
            )

    @PostMapping("/logout")
    fun logout(user: AuthenticatedUser) {
        userService.revokeToken(user.token)
    }

    @GetMapping("/me")
    fun me(user: AuthenticatedUser): ResponseEntity<UserInfoOutput> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserInfoOutput(
                    user.user.id,
                    user.user.username,
                    user.user.role,
                    user.user.createdAt,
                ),
            )

    @PutMapping("/update/role")
    fun updateUserRole(
        user: AuthenticatedUser,
        @RequestBody input: UpdateRoleInput,
    ): ResponseEntity<Any> =
        userService
            .updateUserRole(user.token, input.userToUpdateId, input.role)
            .toResponse(
                onSuccess = {
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(
                            UpdateRoleOutput(
                                userId = it.id,
                                role = it.role,
                            ),
                        )
                },
                onError = { it.toResponseEntity() },
            )
}
