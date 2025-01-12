package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.admin.AdminChangePasswordDto
import com.github.bestheroz.demo.dtos.admin.AdminCreateDto
import com.github.bestheroz.demo.dtos.admin.AdminDto
import com.github.bestheroz.demo.dtos.admin.AdminLoginDto
import com.github.bestheroz.demo.dtos.admin.AdminUpdateDto
import com.github.bestheroz.demo.repository.AdminRepository
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
@Transactional
class AdminService(
    private val adminRepository: AdminRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private val log = logger()
    }

    fun getAdminList(request: AdminDto.Request): ListResult<AdminDto.Response> =
        adminRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(AdminDto.Response::of)
            .let(ListResult.Companion::of)

    fun getAdmin(id: Long): AdminDto.Response =
        adminRepository.findById(id).map(AdminDto.Response::of).orElseThrow {
            BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        }

    @Transactional
    fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository.findByLoginIdAndRemovedFlagFalse(request.loginId).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return adminRepository.save(request.toEntity(operator)).let(AdminDto.Response::of)
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        request: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        val adminLoginIdDeferred =
            coroutineScope.async(Dispatchers.IO) {
                adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)
            }
        val adminDeferred = coroutineScope.async(Dispatchers.IO) { adminRepository.findById(id) }

        adminLoginIdDeferred.await().ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        return adminDeferred
            .await()
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                }
                if (!request.managerFlag && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF)
                }
            }.apply {
                update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                adminRepository.save(this)
            }.let(AdminDto.Response::of)
    }

    @Transactional
    fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) = adminRepository
        .findById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        .also {
            if (it.removedFlag) {
                throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
            }
            if (it.id == operator.id) {
                throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
            }
        }.remove(operator)

    @Transactional
    fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        adminRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                }
                it.password
                    ?.takeUnless { PasswordUtil.verifyPassword(request.oldPassword, it) }
                    ?.also {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { it == request.newPassword }
                    ?.also { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.apply { changePassword(request.newPassword, operator) }
            .let(AdminDto.Response::of)

    @Transactional
    fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        adminRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (!it.useFlag || it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                }
                it.password
                    ?.takeUnless { password -> PasswordUtil.verifyPassword(request.password, password) }
                    ?.also {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
            }.apply { renewToken(jwtTokenProvider.createRefreshToken(Operator(this))) }
            .let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        adminRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
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
    fun logout(id: Long) =
        adminRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .logout()

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id).isEmpty
}
