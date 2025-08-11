package com.github.bestheroz.demo.controller

import com.github.bestheroz.demo.dtos.user.UserChangePasswordDto
import com.github.bestheroz.demo.dtos.user.UserCreateDto
import com.github.bestheroz.demo.dtos.user.UserDto
import com.github.bestheroz.demo.dtos.user.UserLoginDto
import com.github.bestheroz.demo.dtos.user.UserUpdateDto
import com.github.bestheroz.demo.services.UserService
import com.github.bestheroz.standard.common.authenticate.CurrentUser
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/users")
@Tag(name = "User", description = "유저 API")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    fun getUserList(payload: UserDto.Request): ListResult<UserDto.Response> =
        runBlocking {
            userService.getUserList(payload)
        }

    @GetMapping("check-login-id")
    @Operation(summary = "로그인 아이디 중복 확인")
    fun checkLoginId(
        @Schema(description = "로그인 아이디") @RequestParam loginId: String,
        @Schema(description = "유저 ID") @RequestParam(required = false) userId: Long?,
    ): Boolean = runBlocking { userService.checkLoginId(loginId, userId) }

    @PostMapping("login")
    @Operation(summary = "유저 로그인")
    fun loginUser(
        @RequestBody payload: UserLoginDto.Request,
    ): TokenDto =
        runBlocking {
            userService.loginUser(payload)
        }

    @GetMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    fun getUser(
        @PathVariable id: Long,
    ): UserDto.Response = runBlocking { userService.getUser(id) }

    @GetMapping("renew-token")
    @Operation(
        summary = "유저 토큰 갱신",
        description =
            (
                """*어세스 토큰* 만료 시 *리플래시 토큰* 으로 *어세스 토큰* 을 갱신합니다.
    "(동시에 여러 사용자가 접속하고 있다면 *리플래시 토큰* 값이 달라서 갱신이 안될 수 있습니다.)"""
            ),
    )
    fun renewToken(
        @Schema(description = "리플래시 토큰") @RequestHeader(value = "Authorization") refreshToken: String,
    ): TokenDto = runBlocking { userService.renewToken(refreshToken) }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun createUser(
        @RequestBody @Valid payload: UserCreateDto.Request,
        @CurrentUser operator: Operator,
    ): UserDto.Response = runBlocking { userService.createUser(payload, operator) }

    @PutMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody @Valid payload: UserUpdateDto.Request,
        @CurrentUser operator: Operator,
    ): UserDto.Response = runBlocking { userService.updateUser(id, payload, operator) }

    @PatchMapping("{id}/password")
    @Operation(summary = "유저 비밀번호 변경")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun changePassword(
        @PathVariable id: Long,
        @RequestBody @Valid payload: UserChangePasswordDto.Request,
        @CurrentUser operator: Operator,
    ): UserDto.Response = runBlocking { userService.changePassword(id, payload, operator) }

    @DeleteMapping("logout")
    @Operation(
        summary = "유저 로그아웃",
        description = "리플래시 토큰을 삭제합니다.",
        responses = [ApiResponse(responseCode = "204")],
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun logout(
        @CurrentUser operator: Operator,
    ): Unit =
        runBlocking {
            userService.logout(operator.id)
        }

    @DeleteMapping("{id}")
    @Operation(description = "(Soft delete)", responses = [ApiResponse(responseCode = "204")])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    fun deleteUser(
        @PathVariable id: Long,
        @CurrentUser operator: Operator,
    ): Unit =
        runBlocking {
            userService.deleteUser(id, operator)
        }
}
