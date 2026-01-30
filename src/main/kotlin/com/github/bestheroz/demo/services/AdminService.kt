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
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil
import io.github.oshai.kotlinlogging.KotlinLogging
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
        private val logger = KotlinLogging.logger {}
    }

    suspend fun getAdminList(payload: AdminDto.Request): ListResult<AdminDto.Response> =
        withContext(Dispatchers.IO) {
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
        }

    suspend fun getAdmin(id: Long): AdminDto.Response =
        withContext(Dispatchers.IO) {
            adminRepository
                .findById(id)
                .map(AdminDto.Response::of)
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        }

    @Transactional
    suspend fun createAdmin(
        payload: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        withContext(Dispatchers.IO) {
            adminRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId)
        }.ifPresent { throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return withContext(Dispatchers.IO) {
            val saved = adminRepository.save(payload.toEntity(operator))
            adminRepository
                .findById(saved.id)
                .get()
                .let(AdminDto.Response::of)
        }
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

            withContext(Dispatchers.IO) {
                val admin =
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
                        }
                admin.update(
                    payload.loginId,
                    payload.password,
                    payload.name,
                    payload.useFlag,
                    payload.managerFlag,
                    payload.authorities,
                    operator,
                )
                adminRepository.save(admin)
                adminRepository
                    .findById(id)
                    .get()
                    .let(AdminDto.Response::of)
            }
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
    ): AdminDto.Response {
        val admin =
            withContext(Dispatchers.IO) { adminRepository.findById(id) }
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        if (admin.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        }
        admin.password
            ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.oldPassword, password) }
            ?.let {
                logger.warn { "password not match" }
                throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
            }
        admin.password
            ?.takeIf { password -> PasswordUtil.isPasswordValid(payload.newPassword, password) }
            ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }

        admin.changePassword(payload.newPassword, operator)
        return withContext(Dispatchers.IO) {
            adminRepository.save(admin)
            adminRepository
                .findById(id)
                .get()
                .let(AdminDto.Response::of)
        }
    }

    @Transactional
    suspend fun loginAdmin(payload: AdminLoginDto.Request): TokenDto {
        val admin =
            withContext(Dispatchers.IO) {
                adminRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId)
            }.orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
        if (!admin.useFlag || admin.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        }
        admin.password
            ?.takeUnless { password -> PasswordUtil.isPasswordValid(payload.password, password) }
            ?.let {
                logger.warn { "password not match" }
                throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
            }
        admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
        return withContext(Dispatchers.IO) {
            adminRepository.save(admin)
            admin.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }
        }
    }

    @Transactional
    suspend fun renewToken(refreshToken: String): TokenDto =
        withContext(Dispatchers.IO) {
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
                    adminRepository.save(this)
                    token?.let {
                        if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it) || it == refreshToken) {
                            return@withContext TokenDto(jwtTokenProvider.createAccessToken(Operator(this)), it)
                        }
                    }
                    throw Unauthorized401Exception()
                }
        }

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
