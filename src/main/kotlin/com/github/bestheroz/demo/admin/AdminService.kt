package com.github.bestheroz.demo.admin

import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.Unauthorized401Exception
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.verifyPassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
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
            .let { ListResult.of(it) }

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
        return adminRepository.save(request.toEntity(operator)).let { AdminDto.Response.of(it) }
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

        val admin =
            withContext(Dispatchers.IO) { adminRepository.findById(id) }
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        adminLoginIdDeferred.await().ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }
        admin
            .takeIf { it.removedFlag }
            ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        admin
            .takeIf { !request.managerFlag && it.id == operator.id }
            ?.let { throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF) }

        return admin
            .let { it ->
                it.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                it
            }.let { adminRepository.save(it) }
            .let { AdminDto.Response.of(it) }
    }

    @Transactional
    fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) = adminRepository
        .findById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
        .let { admin ->
            admin
                .takeIf { it.removedFlag }
                ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            admin
                .takeIf { it.id == operator.id }
                ?.let { throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
            admin.remove(operator)
        }

    @Transactional
    fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        adminRepository
            .findById(id)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .let { admin ->
                admin
                    .takeIf { it.removedFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
                admin.password
                    ?.takeUnless { verifyPassword(request.oldPassword, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                admin.password
                    ?.takeIf { it == request.newPassword }
                    ?.let { throw BadRequest400Exception(ExceptionCode.CHANGE_TO_SAME_PASSWORD) }
                admin
            }.let {
                it.changePassword(request.newPassword, operator)
                it
            }.let(AdminDto.Response::of)

    @Transactional
    fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        adminRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT) }
            .let { admin ->
                admin
                    .takeUnless { admin.useFlag }
                    ?.let { throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
                admin.password
                    ?.takeUnless { verifyPassword(request.password, it) }
                    ?.let {
                        log.warn("password not match")
                        throw BadRequest400Exception(ExceptionCode.INVALID_PASSWORD)
                    }
                admin
            }.let {
                it.renewToken(jwtTokenProvider.createRefreshToken(Operator(it)))
                it
            }.let { TokenDto(jwtTokenProvider.createAccessToken(Operator(it)), it.token ?: "") }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto =
        adminRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }
            .let { admin ->
                admin
                    .takeIf {
                        admin.removedFlag ||
                            admin.token == null ||
                            !jwtTokenProvider.validateToken(refreshToken)
                    }?.let { throw Unauthorized401Exception() }

                admin.token?.let { it ->
                    if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), it)
                    } else if (it == refreshToken) {
                        admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
                        return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), it)
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
