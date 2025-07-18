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
class AdminService(
    private val adminRepository: AdminRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    suspend fun getAdminList(payload: AdminDto.Request): ListResult<AdminDto.Response> =
        withContext(Dispatchers.IO) {
            adminRepository.findAll(
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
            )
        }.map(AdminDto.Response::of)
            .let(ListResult.Companion::of)

    suspend fun getAdmin(id: Long): AdminDto.Response =
        withContext(Dispatchers.IO) { adminRepository.findById(id) }
            .map(AdminDto.Response::of)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }

    @Transactional
    suspend fun createAdmin(
        payload: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        withContext(Dispatchers.IO) {
            adminRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId)
        }.ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return adminRepository.save(payload.toEntity(operator)).let(AdminDto.Response::of)
    }

    @Transactional
    suspend fun updateAdmin(
        id: Long,
        payload: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        coroutineScope {
            val adminLoginIdDeferred =
                async(Dispatchers.IO) {
                    adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(payload.loginId, id)
                }
            val adminDeferred = async(Dispatchers.IO) { adminRepository.findById(id) }

            adminLoginIdDeferred.await().ifPresent {
                adminDeferred.cancel()
                throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
            }

            adminDeferred
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
    suspend fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) {
        val admin = withContext(Dispatchers.IO) { adminRepository.findById(id) }
        return admin
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
                }
                if (it.id == operator.id) {
                    throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
                }
            }.let {
                it.remove(operator)
                withContext(Dispatchers.IO) { adminRepository.save(it) }
            }
    }

    @Transactional
    suspend fun changePassword(
        id: Long,
        payload: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        withContext(Dispatchers.IO) { adminRepository.findById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .also {
                if (it.removedFlag) {
                    throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
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
            .let { withContext(Dispatchers.IO) { adminRepository.save(it) } }
            .let(AdminDto.Response::of)

    @Transactional
    suspend fun loginAdmin(payload: AdminLoginDto.Request): TokenDto =
        withContext(Dispatchers.IO) {
            adminRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId)
        }.orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
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
            .let { withContext(Dispatchers.IO) { adminRepository.save(it) } }
            .let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto =
        withContext(Dispatchers.IO) { adminRepository.findById(jwtTokenProvider.getId(refreshToken)) }
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
            }.let { withContext(Dispatchers.IO) { adminRepository.save(it) } }

    @Transactional
    suspend fun logout(id: Long) =
        withContext(Dispatchers.IO) { adminRepository.findById(id) }
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .apply { logout() }
            .let { withContext(Dispatchers.IO) { adminRepository.save(it) } }

    suspend fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id)
        }.isEmpty
}
