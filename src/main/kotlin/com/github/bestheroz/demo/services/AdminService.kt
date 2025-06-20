package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.admin.AdminChangePasswordDto
import com.github.bestheroz.demo.dtos.admin.AdminCreateDto
import com.github.bestheroz.demo.dtos.admin.AdminDto
import com.github.bestheroz.demo.dtos.admin.AdminLoginDto
import com.github.bestheroz.demo.dtos.admin.AdminUpdateDto
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.demo.repository.AdminSpecification
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
import org.springframework.data.jpa.domain.Specification
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

    fun getAdminList(payload: AdminDto.Request): ListResult<AdminDto.Response> =
        adminRepository
            .findAll(
                Specification.allOf(
                    listOfNotNull(
                        AdminSpecification.removedFlagIsFalse(),
                        payload.id?.let { AdminSpecification.equalId(it) },
                        payload.loginId?.let { AdminSpecification.containsLoginId(it) },
                        payload.name?.let { AdminSpecification.containsName(it) },
                        payload.useFlag?.let { AdminSpecification.equalUseFlag(it) },
                        payload.managerFlag?.let { AdminSpecification.equalManagerFlag(it) },
                    ),
                ),
                PageRequest.of(payload.page - 1, payload.pageSize, Sort.by("id").descending()),
            ).map(AdminDto.Response::of)
            .let(ListResult.Companion::of)

    fun getAdmin(id: Long): AdminDto.Response =
        adminRepository.findById(id).map(AdminDto.Response::of).orElseThrow {
            BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        }

    @Transactional
    fun createAdmin(
        payload: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        return adminRepository.save(payload.toEntity(operator)).let(AdminDto.Response::of)
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        payload: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        val adminLoginIdDeferred =
            coroutineScope.async(Dispatchers.IO) {
                adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(payload.loginId, id)
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
                if (!payload.managerFlag && it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF)
                }
            }.apply {
                update(
                    payload.loginId,
                    payload.password,
                    payload.name,
                    payload.useFlag,
                    payload.managerFlag,
                    payload.authorities,
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
        payload: AdminChangePasswordDto.Request,
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
                    ?.takeUnless { PasswordUtil.isPasswordValid(payload.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                it.password
                    ?.takeIf { it == payload.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
            }.apply { changePassword(payload.newPassword, operator) }
            .let(AdminDto.Response::of)

    @Transactional
    fun loginAdmin(payload: AdminLoginDto.Request): TokenDto =
        adminRepository
            .findByLoginIdAndRemovedFlagFalse(payload.loginId)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .also {
                if (!it.useFlag || it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                }
                it.password
                    ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.password, password) }
                    ?.let {
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
