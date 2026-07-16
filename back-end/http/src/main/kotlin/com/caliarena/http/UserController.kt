package com.caliarena.http

import com.caliarena.domain.token.TokenExternalInfo
import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.User
import com.caliarena.http.model.Problem
import com.caliarena.http.model.user.CreateUserInput
import com.caliarena.http.model.user.UpdateRoleInput
import com.caliarena.http.model.user.UpdateRoleOutput
import com.caliarena.http.model.user.UserInfoOutput
import com.caliarena.http.model.user.UserLoginInput
import com.caliarena.http.model.user.UserLoginOutput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.UserAuthService
import com.caliarena.service.UserError
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
    @PostMapping
    fun createUser(
        @RequestBody userInput: CreateUserInput,
    ): ResponseEntity<Any> =
        userService
            .createUser(userInput.username, userInput.password)
            .toResponse<UserError, User>(
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
            .toResponse<UserError, TokenExternalInfo>(
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
            .toResponse<UserError, User>(
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

    private fun UserError.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            UserError.ErrorUpdatingUserRole ->
                Problem.ErrorUpdatingUserRole.response(HttpStatus.BAD_REQUEST)

            UserError.InsecurePassword ->
                Problem.InsecurePassword.response(HttpStatus.BAD_REQUEST)

            UserError.InvalidRole ->
                Problem.InvalidRole.response(HttpStatus.BAD_REQUEST)

            UserError.UserNotFound ->
                Problem.UserNotFound.response(HttpStatus.NOT_FOUND)

            UserError.AlreadyUsedUsername ->
                Problem.AlreadyUsedUsername.response(HttpStatus.CONFLICT)

            UserError.UserOrPasswordAreInvalid ->
                Problem.UserOrPasswordAreInvalid.response(HttpStatus.UNAUTHORIZED)

            UserError.NotAuthorized -> Problem.NotAuthorized.response(HttpStatus.FORBIDDEN)
        }
}
