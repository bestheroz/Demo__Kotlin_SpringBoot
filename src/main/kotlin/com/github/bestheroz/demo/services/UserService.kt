package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.user.UserChangePasswordDto
import com.github.bestheroz.demo.dtos.user.UserCreateDto
import com.github.bestheroz.demo.dtos.user.UserDto
import com.github.bestheroz.demo.dtos.user.UserLoginDto
import com.github.bestheroz.demo.dtos.user.UserUpdateDto
import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.Unauthorized401Exception
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private val log = logger()
    }

    fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> =
        userRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(UserDto.Response::of)
            .let(ListResult.Companion::of)

    fun getUser(id: Long): UserDto.Response =
        userRepository.findById(id).map(UserDto.Response::of).orElseThrow {
            BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        }

    @Transactional
    fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalse(request.loginId).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return userRepository.save(request.toEntity(operator)).let(UserDto.Response::of)
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val userLoginIdDeferred =
            coroutineScope.async(Dispatchers.IO) {
                userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)
            }
        val userDeferred = coroutineScope.async(Dispatchers.IO) { userRepository.findById(id) }

        userLoginIdDeferred.await().ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return userDeferred
            .await()
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
            }.apply {
                update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.authorities,
                    operator,
                )
                userRepository.save(this)
            }.let(UserDto.Response::of)
    }

    @Transactional
    fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        userRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
                if (it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.remove(operator)
    }

    @Transactional
    fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response =
        userRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
                it.password
                    ?.takeUnless { PasswordUtil.isPasswordValid(request.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { it == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.apply { changePassword(request.newPassword, operator) }
            .let(UserDto.Response::of)

    @Transactional
    fun loginUser(request: UserLoginDto.Request): TokenDto =
        userRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (it.removedFlag || !it.useFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
                it.password
                    ?.takeUnless { password -> PasswordUtil.isPasswordValid(request.password, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.apply { renewToken(jwtTokenProvider.createRefreshToken(Operator(this))) }
            .let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        userRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag || it.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
                    throw Unauthorized401Exception()
                }
            }.apply {
                if (token == refreshToken) {
                    renewToken(jwtTokenProvider.createRefreshToken(Operator(this)))
                }
            }.run {
                token?.let {
                    if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it) || it == refreshToken) {
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(this)), it)
                    }
                }
                throw Unauthorized401Exception()
            }

    @Transactional
    fun logout(id: Long) {
        userRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .logout()
    }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id).isEmpty
}
