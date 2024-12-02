package com.github.bestheroz.demo.user

import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.Unauthorized401Exception
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.verifyPassword
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    @Transactional(readOnly = true)
    fun getUserList(request: UserDto.Request): ListResult<UserDto.Response> =
        userRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(UserDto.Response::of)
            .let { ListResult.of(it) }

    @Transactional(readOnly = true)
    fun getUser(id: Long): UserDto.Response =
        userRepository.findById(id).map(UserDto.Response::of).orElseThrow {
            BadRequest400Exception(ExceptionCode.UNKNOWN_USER)
        }

    fun createUser(
        request: UserCreateDto.Request,
        operator: Operator,
    ): UserDto.Response {
        userRepository.findByLoginIdAndRemovedFlagFalse(request.loginId).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return UserDto.Response.of(userRepository.save(request.toEntity(operator)))
    }

    fun updateUser(
        id: Long,
        request: UserUpdateDto.Request,
        operator: Operator,
    ): UserDto.Response =
        userRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .let { user ->
                user
                    .takeIf { it.removedFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }

                userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id).ifPresent {
                    throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
                }

                user.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.authorities,
                    operator,
                )
                return UserDto.Response.of(user)
            }

    fun deleteUser(
        id: Long,
        operator: Operator,
    ) = userRepository
        .findById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
        .let { user ->
            user
                .takeIf { it.removedFlag }
                ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            user
                .takeIf { it.id == operator.id }
                ?.let { throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
            user.remove(operator)
        }

    fun changePassword(
        id: Long,
        request: UserChangePasswordDto.Request,
        operator: Operator,
    ): UserDto.Response =
        userRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .let { user ->
                user
                    .takeIf { it.removedFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                user.password
                    ?.takeUnless { verifyPassword(request.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                user.password
                    ?.takeIf { it == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
                user.changePassword(request.newPassword, operator)
                return UserDto.Response.of(user)
            }

    fun loginUser(request: UserLoginDto.Request): TokenDto =
        userRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            .orElseThrow<BadRequest400Exception> {
                BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT)
            }.let { user ->
                user
                    .takeIf { it.removedFlag || !user.useFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
                user.password
                    ?.takeUnless { verifyPassword(request.password, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), user.token!!)
            }

    fun renewToken(refreshToken: String): TokenDto =
        userRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .let { user ->
                user
                    .takeIf {
                        user.removedFlag || user.token == null || !jwtTokenProvider.validateToken(refreshToken)
                    }?.let { throw Unauthorized401Exception() }
                user.token?.let {
                    if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), it)
                    } else if (it == refreshToken) {
                        user.renewToken(jwtTokenProvider.createRefreshToken(Operator(user)))
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(user)), it)
                    }
                }
                throw Unauthorized401Exception()
            }

    fun logout(id: Long) {
        userRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_USER) }
            .logout()
    }

    @Transactional(readOnly = true)
    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = userRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id).isEmpty
}
