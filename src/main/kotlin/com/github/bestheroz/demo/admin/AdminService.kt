package com.github.bestheroz.demo.admin

import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.standard.common.authenticate.JwtTokenProvider
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.dto.TokenDto
import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.verifyPassword
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminService(
    private val adminRepository: AdminRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    companion object {
        private val log = logger()
    }

    @Transactional(readOnly = true)
    fun getAdminList(request: AdminDto.Request): ListResult<AdminDto.Response> =
        adminRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(
                    request.page - 1,
                    request.pageSize,
                    Sort.by("id").descending(),
                ),
            ).map(AdminDto.Response::of)
            .let {
                ListResult.of(it)
            }

    @Transactional(readOnly = true)
    fun getAdmin(id: Long): AdminDto.Response =
        adminRepository
            .findById(id)
            .map(AdminDto.Response::of)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_ADMIN) }

    fun createAdmin(
        request: AdminCreateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository
            .findByLoginIdAndRemovedFlagFalse(
                request.loginId,
            ).ifPresent { throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT) }
        return adminRepository.save(request.toEntity(operator)).let { AdminDto.Response.of(it) }
    }

    fun updateAdmin(
        id: Long,
        request: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        adminRepository
            .findById(id)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
            .let { admin ->

                admin.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
                admin.takeIf { !request.managerFlag && it.id == operator.id }?.let {
                    throw RequestException400(ExceptionCode.CANNOT_UPDATE_YOURSELF)
                }
                admin.takeIf { !it.managerFlag && !request.managerFlag && !operator.managerFlag }?.let {
                    throw RequestException400(ExceptionCode.UNKNOWN_AUTHORITY)
                }
                adminRepository
                    .findByLoginIdAndRemovedFlagFalseAndIdNot(request.loginId, id)
                    .ifPresent { throw RequestException400(ExceptionCode.ALREADY_JOINED_ACCOUNT) }

                admin.update(
                    request.loginId,
                    request.password,
                    request.name,
                    request.useFlag,
                    request.managerFlag,
                    request.authorities,
                    operator,
                )
                return AdminDto.Response.of(admin)
            }

    fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) = adminRepository
        .findById(id)
        .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
        .let { admin ->
            admin.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
            admin.takeIf { it.id == operator.id }?.let { throw RequestException400(ExceptionCode.CANNOT_REMOVE_YOURSELF) }
            admin.remove(operator)
        }

    fun changePassword(
        id: Long,
        request: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response =
        adminRepository
            .findById(id)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
            .let { admin ->
                admin.takeIf { it.removedFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
                admin.password?.takeUnless { verifyPassword(request.oldPassword, it) }?.let {
                    log.warn("password not match")
                    throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                }
                admin.password?.takeIf { it == request.newPassword }?.let {
                    throw RequestException400(ExceptionCode.CHANGE_TO_SAME_PASSWORD)
                }
                admin.changePassword(request.newPassword, operator)
                return AdminDto.Response.of(admin)
            }

    fun loginAdmin(request: AdminLoginDto.Request): TokenDto =
        adminRepository
            .findByLoginIdAndRemovedFlagFalse(request.loginId)
            .orElseThrow { RequestException400(ExceptionCode.UNJOINED_ACCOUNT) }
            .let { admin ->

                admin.takeUnless { admin.useFlag }?.let { throw RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
                admin.password?.takeUnless { verifyPassword(request.password, it) }?.let {
                    log.warn("password not match")
                    throw RequestException400(ExceptionCode.INVALID_PASSWORD)
                }
                admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
                return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), admin.token ?: "")
            }

    fun renewToken(refreshToken: String): TokenDto =
        adminRepository
            .findById(jwtTokenProvider.getId(refreshToken))
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
            .let { admin ->
                admin
                    .takeIf { admin.removedFlag || admin.token == null || !jwtTokenProvider.validateToken(refreshToken) }
                    ?.let { throw AuthenticationException401() }

                admin.token?.let { it ->
                    if (jwtTokenProvider.issuedRefreshTokenIn3Seconds(it)) {
                        return TokenDto(
                            jwtTokenProvider.createAccessToken(Operator(admin)),
                            it,
                        )
                    } else if (it == refreshToken) {
                        admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
                        return TokenDto(
                            jwtTokenProvider.createAccessToken(Operator(admin)),
                            it,
                        )
                    }
                }
                throw AuthenticationException401()
            }

    fun logout(id: Long) =
        adminRepository
            .findById(id)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_ADMIN) }
            .logout()

    @Transactional(readOnly = true)
    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id).isEmpty
}
