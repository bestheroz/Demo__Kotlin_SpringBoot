package com.github.bestheroz.standard.common.exception

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.response.ApiResult
import com.github.bestheroz.standard.common.response.ApiResult.Companion.of
import com.github.bestheroz.standard.common.util.LogUtils
import jakarta.servlet.http.HttpServletResponse
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.lang.IllegalStateException

@RestControllerAdvice
class ApiExceptionHandler {
    companion object {
        private val log = logger()
    }

    // 아래서 놓친 예외가 있을때 이곳으로 확인하기 위해 존재한다.
    // 놓친 예외는 이곳에서 확인하여 추가해주면 된다.
    @ExceptionHandler(Throwable::class)
    fun exception(e: Throwable?): ResponseEntity<ApiResult<*>> {
        log.error(LogUtils.getStackTrace(e))
        return ResponseEntity.internalServerError().body(of(ExceptionCode.UNKNOWN_SYSTEM_ERROR))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun noResourceFoundException(e: NoResourceFoundException?): ResponseEntity<ApiResult<*>> {
        log.error(LogUtils.getStackTrace(e))
        return ResponseEntity.notFound().build<ApiResult<*>>()
    }

    @ExceptionHandler(BadRequest400Exception::class)
    fun requestException400(e: BadRequest400Exception): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.badRequest().body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(Unauthorized401Exception::class)
    fun authenticationException401(e: Unauthorized401Exception): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        val builder: ResponseEntity.BodyBuilder = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        if (e.exceptionCode == ExceptionCode.EXPIRED_TOKEN) {
            builder.header("token", "must-renew")
        }
        return builder.body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(Forbidden403Exception::class)
    fun authorityException403(e: Forbidden403Exception): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(AuthorizationDeniedException::class, AccessDeniedException::class)
    fun authorizationDeniedException(e: AccessDeniedException?): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(of(ExceptionCode.UNKNOWN_AUTHORITY))
    }

    @ExceptionHandler(InternalServerError500Exception::class)
    fun systemException500(e: InternalServerError500Exception): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.internalServerError().body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun illegalArgumentException(e: Throwable?): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(of(ExceptionCode.INVALID_PARAMETER))
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun usernameNotFoundException(e: UsernameNotFoundException?): ResponseEntity<ApiResult<*>> = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    @ExceptionHandler(BindException::class)
    fun bindException(e: Throwable?): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.badRequest().build()
    }

    @ExceptionHandler(
        HttpMediaTypeNotAcceptableException::class,
        HttpMediaTypeNotSupportedException::class,
        HttpRequestMethodNotSupportedException::class,
        HttpClientErrorException::class,
    )
    fun httpMediaTypeNotAcceptableException(
        e: Throwable?,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.badRequest().build()
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun duplicateKeyException(e: DuplicateKeyException?): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.badRequest().build()
    }
}
