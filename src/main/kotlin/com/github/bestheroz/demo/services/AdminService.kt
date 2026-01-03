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
        adminRepository
            .findById(id)
            .map(AdminDto.Response::of)
            .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN) }

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
    fun updateAdmin(
        id: Long,
        payload: AdminUpdateDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(payload.loginId, id).ifPresent {
            throw BadRequest400Exception(ExceptionCode.ALREADY_JOINED_ACCOUNT)
        }

        val admin =
            adminRepository.findById(id).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
            }

        if (admin.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        }
        if (!payload.managerFlag && admin.id == operator.id) {
            throw BadRequest400Exception(ExceptionCode.CANNOT_UPDATE_YOURSELF)
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
        return AdminDto.Response.of(admin)
    }

    @Transactional
    fun deleteAdmin(
        id: Long,
        operator: Operator,
    ) {
        val admin =
            adminRepository.findById(id).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
            }

        if (admin.removedFlag) {
            throw BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
        }
        if (admin.id == operator.id) {
            throw BadRequest400Exception(ExceptionCode.CANNOT_REMOVE_YOURSELF)
        }

        admin.remove(operator)
        adminRepository.save(admin)
    }

    @Transactional
    fun changePassword(
        id: Long,
        payload: AdminChangePasswordDto.Request,
        operator: Operator,
    ): AdminDto.Response {
        val admin =
            adminRepository.findById(id).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
            }

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
        adminRepository.save(admin)
        return AdminDto.Response.of(admin)
    }

    @Transactional
    fun loginAdmin(payload: AdminLoginDto.Request): TokenDto {
        val admin =
            adminRepository.findByLoginIdAndRemovedFlagFalse(payload.loginId).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNJOINED_ACCOUNT)
            }

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
        adminRepository.save(admin)
        return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), admin.token ?: "")
    }

    @Transactional
    fun renewToken(refreshToken: String): TokenDto {
        val admin =
            adminRepository.findById(jwtTokenProvider.getId(refreshToken)).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
            }

        if (admin.removedFlag || admin.token == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw Unauthorized401Exception()
        }

        if (admin.token == refreshToken) {
            admin.renewToken(jwtTokenProvider.createRefreshToken(Operator(admin)))
        }

        val token = admin.token
        if (token != null && (jwtTokenProvider.issuedRefreshTokenIn3Seconds(token) || token == refreshToken)) {
            return TokenDto(jwtTokenProvider.createAccessToken(Operator(admin)), token)
        }

        throw Unauthorized401Exception()
    }

    @Transactional
    fun logout(id: Long) {
        val admin =
            adminRepository.findById(id).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_ADMIN)
            }
        admin.logout()
        adminRepository.save(admin)
    }

    fun checkLoginId(
        loginId: String,
        id: Long?,
    ): Boolean = !adminRepository.findByLoginIdAndRemovedFlagFalseAndIdNot(loginId, id).isPresent
}
