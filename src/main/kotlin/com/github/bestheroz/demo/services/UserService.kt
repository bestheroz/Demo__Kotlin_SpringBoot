package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.user.UserChangePasswordDto
import com.github.bestheroz.demo.dtos.user.UserCreateDto
import com.github.bestheroz.demo.dtos.user.UserDto
import com.github.bestheroz.demo.dtos.user.UserLoginDto
import com.github.bestheroz.demo.dtos.user.UserUpdateDto
import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.demo.repository.UserSpecification
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.Unauthorized401Exception
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getUserList(payload: UserDto.Request): ListResult<UserDto.Response> =
        withContext(Dispatchers.IO) {
            userRepository.findAll(
                Specification.allOf(
                    listOfNotNull(
                        UserSpecification.removedFlagIsFalse(),
                        payload.id?.let { UserSpecification.equalId(it) },
                        payload.loginId?.let { UserSpecification.containsLoginId(it) },
                        payload.name?.let { UserSpecification.containsName(it) },
                        payload.useFlag?.let { UserSpecification.equalUseFlag(it) },
                    ),
                ),
                PageRequest.of(payload.page - 1, payload.pageSize, Sort.by("id").descending()),
            )
        }.map(UserDto.Response::of)
            .let(ListResult.Companion::of)

    suspend fun getUser(id: Long): UserDto.Response =
        withContext(Dispatchers.IO) { userRepository.findById(id) }
            .map(UserDto.Response::of)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

    @Transactional
    suspend fun createUser(
        payload: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        withContext(Dispatchers.IO) { userRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId) }
            .ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return withContext(Dispatchers.IO) { userRepository.save(payload.toEntity(operator)) }
            .let(UserDto.Response::of)
    }

    @Transactional
    suspend fun updateUser(
        id: Long,
        payload: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response =
        coroutineScope {
            val userLoginIdDeferred =
                async(Dispatchers.IO) {
                    userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(payload.loginId, id)
                }
            val userDeferred = async(Dispatchers.IO) { userRepository.findById(id) }

            userLoginIdDeferred.await().ifPresent {
                userDeferred.cancel()
                throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
            }

            userDeferred
                .await()
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                .also {
                    if (it.removedFlag) {
                        throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                    }
                }.apply {
                    update(
                        payload.loginId,
                        payload.password,
                        payload.name,
                        payload.useFlag,
                        payload.authorities,
                        operator,
                    )
                    userRepository.save(this)
                }.let(UserDto.Response::of)
        }

    @Transactional
    suspend fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        withContext(Dispatchers.IO) { userRepository.findById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
                if (it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.apply { remove(operator) }
            .let { withContext(Dispatchers.IO) { userRepository.save(it) } }
    }

    @Transactional
    suspend fun changePassword(
        id: Long,
        payload: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response =
        withContext(Dispatchers.IO) { userRepository.findById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
                it.password
                    ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.oldPassword, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { password -> PasswordUtil.isPasswordValid(payload.newPassword, password) }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.apply { changePassword(payload.newPassword, operator) }
            .let { withContext(Dispatchers.IO) { userRepository.save(it) } }
            .let(UserDto.Response::of)

    @Transactional
    suspend fun loginUser(payload: UserLoginDto.Request): TokenDto =
        withContext(Dispatchers.IO) { userRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (it.removedFlag || !it.useFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
                }
                it.password
                    ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.password, password) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.apply { renewToken(jwtTokenProvider.createRefreshToken(Operator(this))) }
            .let { withContext(Dispatchers.IO) { userRepository.save(it) } }
            .let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto =
        withContext(Dispatchers.IO) { userRepository.findById(jwtTokenProvider.getId(refreshToken)) }
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
            }.let { withContext(Dispatchers.IO) { userRepository.save(it) } }

    @Transactional
    suspend fun logout(id: Long) {
        withContext(Dispatchers.IO) { userRepository.findById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .apply { logout() }
            .let { withContext(Dispatchers.IO) { userRepository.save(it) } }
    }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id)
        }.isEmpty
}
