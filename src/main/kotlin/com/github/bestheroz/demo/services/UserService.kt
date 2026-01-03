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
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil
import io.github.oshai.kotlinlogging.KotlinLogging
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
        private val logger = KotlinLogging.logger {}
    }

    fun getUserList(payload: UserDto.Request): ListResult<UserDto.Response> =
        userRepository
            .findAll(
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
            ).map(UserDto.Response::of)
            .let(ListResult.Companion::of)

    fun getUser(id: Long): UserDto.Response =
        userRepository
            .findById(id)
            .map(UserDto.Response::of)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

    @Transactional
    fun createUser(
        payload: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return userRepository.save(payload.toEntity(operator)).let(UserDto.Response::of)
    }

    @Transactional
    fun updateUser(
        id: Long,
        payload: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(payload.loginId, id).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        val user =
            userRepository.findById(id).orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

        if (user.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        }

        user.update(
            payload.loginId,
            payload.password,
            payload.name,
            payload.useFlag,
            payload.authorities,
            operator,
        )
        userRepository.save(user)
        return UserDto.Response.of(user)
    }

    @Transactional
    fun deleteUser(
        id: Long,
        operator: Operator,
    ) {
        val user =
            userRepository.findById(id).orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

        if (user.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        }
        if (user.id == operator.id) {
            throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
        }

        user.remove(operator)
        userRepository.save(user)
    }

    @Transactional
    fun changePassword(
        id: Long,
        payload: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response {
        val user =
            userRepository.findById(id).orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

        if (user.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        }

        user.password
            ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.oldPassword, password) }
            ?.let {
                logger.warn { "password not match" }
                throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
            }

        user.password
            ?.takeIf { password -> PasswordUtil.isPasswordValid(payload.newPassword, password) }
            ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }

        user.changePassword(payload.newPassword, operator)
        userRepository.save(user)
        return UserDto.Response.of(user)
    }

    @Transactional
    fun loginUser(payload: UserLoginDto.Request): TokenDto {
        val user =
            userRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT)
            }

        if (user.removedFlag || !user.useFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        }

        user.password
            ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.password, password) }
            ?.let {
                logger.warn { "password not match" }
                throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
            }

        user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
        userRepository.save(user)
        return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), user.token ?: "")
    }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto {
        val user =
            userRepository.findById(jwtTokenProvider.getId(refreshToken)).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
            }

        if (user.removedFlag || user.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw Unauthorized401Exception()
        }

        if (user.token == refreshToken) {
            user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
        }

        val token = user.token
        if (token != null && (jwtTokenProvider.issuedRefreshTokenIn3Seconds(token) || token == refreshToken)) {
            return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), token)
        }

        throw Unauthorized401Exception()
    }

    @Transactional
    fun logout(id: Long) {
        val user =
            userRepository.findById(id).orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
        user.logout()
        userRepository.save(user)
    }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = !userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id).isPresent
}
