package com.github.bestheroz.standard.common.authenticate

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.LogUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import java.time.Instant
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiration-minutes}") private val accessTokenExpirationMinutes: Long,
    @Value("\${jwt.refresh-token-expiration-minutes}") private val refreshTokenExpirationMinutes: Long,
) {
    companion object {
        private val log = logger()
    }

    private val algorithm: Algorithm = Algorithm.HMAC512(secret)

    fun createAccessToken(customOperator: Operator): String {
        Assert.notNull(customOperator, "customUserDetails must not be null")
        return JWT
            .create()
            .withClaim("id", customOperator.id)
            .withClaim("loginId", customOperator.loginId)
            .withClaim("name", customOperator.name)
            .withClaim("type", customOperator.type.name)
            .withClaim("managerFlag", customOperator.managerFlag)
            .withArrayClaim(
                "authorities",
                customOperator.authorities.map { it.toString() }.toTypedArray(),
            ).withExpiresAt(Date.from(Instant.now().plusSeconds(accessTokenExpirationMinutes * 60)))
            .sign(algorithm)
    }

    fun createRefreshToken(customOperator: Operator): String {
        Assert.notNull(customOperator, "customUserDetails must not be null")
        return JWT
            .create()
            .withClaim("id", customOperator.id)
            .withExpiresAt(Date.from(Instant.now().plusSeconds(refreshTokenExpirationMinutes * 60)))
            .sign(algorithm)
    }

    fun getId(token: String): Long = verifyToken(token).getClaim("id").asLong()

    fun getOperator(token: String): UserDetails {
        val jwt = verifyToken(token)
        return Operator(
            jwt.getClaim("id").asLong(),
            jwt.getClaim("loginId").asString(),
            jwt.getClaim("name").asString(),
            UserTypeEnum.valueOf(jwt.getClaim("type").asString()),
            jwt.getClaim("managerFlag").asBoolean(),
            jwt
                .getClaim("authorities")
                .asList(String::class.java)
                .stream()
                .map { value: String -> AuthorityEnum.valueOf(value) }
                .toList(),
        )
    }

    fun resolveAccessToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")?.let {
            return@resolveAccessToken it.takeIf { it.startsWith("Bearer ") }?.substring(7) ?: it
        }

    fun validateToken(token: String): Boolean {
        try {
            verifyToken(token)
            return true
        } catch (e: JWTVerificationException) {
            log.warn("Invalid JWT token: {}", e.message)
            return false
        }
    }

    fun issuedRefreshTokenIn3Seconds(refreshToken: String): Boolean =
        try {
            Instant.now().plusSeconds(3).isBefore(
                JWT
                    .require(algorithm)
                    .build()
                    .verify(refreshToken)
                    .expiresAt
                    .toInstant(),
            )
        } catch (e: JWTVerificationException) {
            log.warn("Invalid refresh token: {}", e.message)
            log.warn(LogUtils.getStackTrace(e))
            false
        }

    private fun verifyToken(token: String): DecodedJWT = JWT.require(algorithm).build().verify(token.replace("Bearer ", ""))
}
