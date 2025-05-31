package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.config.SecurityConfig
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.StopWatch
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {
    companion object {
        private const val REQUEST_COMPLETE_EXECUTE_TIME =
            "{} ....... Request Complete Execute Time ....... : {}"
        private const val REQUEST_PARAMETERS = "<{}>{}?{}"
        private val log = logger()
    }

    // AntPathMatcher를 한 개만 생성해두고 재사용
    private val pathMatcher = AntPathMatcher()

    // SecurityConfig 에 정의된 각 HTTP 메서드별 public 경로 패턴을 문자열 리스트로 변환
    private val publicGetPatterns: List<String> = SecurityConfig.GET_PUBLIC.toList()
    private val publicPostPatterns: List<String> = SecurityConfig.POST_PUBLIC.toList()
    private val publicDeletePatterns: List<String> = SecurityConfig.DELETE_PUBLIC.toList()

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestURI: String = UrlPathHelper().getPathWithinApplication(request)

        if (requestURI == "/") {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        if (!requestURI.startsWith("/api/v1/health/")) {
            log.info(
                REQUEST_PARAMETERS,
                request.method,
                requestURI,
                StringUtils.defaultString(request.queryString),
            )
        }

        val stopWatch = StopWatch()
        stopWatch.start()

        try {
            if (isPublicPath(request, requestURI)) {
                filterChain.doFilter(request, response)
                return
            }

            val token = jwtTokenProvider.resolveAccessToken(request)
            if (token == null) {
                log.info("No access token found")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }

            if (!jwtTokenProvider.validateToken(token)) {
                log.info("Invalid access token - refresh token required")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }

            val userDetails: UserDetails = jwtTokenProvider.getOperator(token)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

            filterChain.doFilter(request, response)
        } finally {
            stopWatch.stop()
            if (!requestURI.startsWith("/api/v1/health/")) {
                log.info(REQUEST_COMPLETE_EXECUTE_TIME, requestURI, stopWatch)
            }
        }
    }

    /**
     * HTTP 메서드별로 미리 정의된 publicPatterns(List<String>)에 대해 AntPathMatcher.match()를 사용하여 요청 URI가 해당 패턴 중
     * 하나에 매치되는지 검사
     */
    private fun isPublicPath(
        request: HttpServletRequest,
        requestURI: String,
    ): Boolean =
        when (request.method) {
            HttpMethod.GET.toString() -> {
                publicGetPatterns.any { pattern -> pathMatcher.match(pattern, requestURI) }
            }
            HttpMethod.POST.toString() -> {
                publicPostPatterns.any { pattern -> pathMatcher.match(pattern, requestURI) }
            }
            HttpMethod.DELETE.toString() -> {
                publicDeletePatterns.any { pattern -> pathMatcher.match(pattern, requestURI) }
            }
            else -> {
                false
            }
        }
}
