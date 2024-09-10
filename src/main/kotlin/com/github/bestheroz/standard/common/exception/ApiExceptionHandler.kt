package com.github.bestheroz.standard.common.exception

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.response.ApiResult
import com.github.bestheroz.standard.common.response.Result
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
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.lang.IllegalStateException

@ControllerAdvice
@RestController
class ApiExceptionHandler {
    companion object {
        private val log = logger()
    }

    // 아래서 놓친 예외가 있을때 이곳으로 확인하기 위해 존재한다.
    // 놓친 예외는 이곳에서 확인하여 추가해주면 된다.
    @ExceptionHandler(Throwable::class)
    fun exception(e: Throwable?): ResponseEntity<ApiResult<*>> {
        log.error(LogUtils.getStackTrace(e))
        return Result.error()
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun noResourceFoundException(e: NoResourceFoundException?): ResponseEntity<ApiResult<*>> {
        log.error(LogUtils.getStackTrace(e))
        return ResponseEntity.notFound().build<ApiResult<*>>()
    }

    @ExceptionHandler(RequestException400::class)
    fun requestException400(e: RequestException400): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity.badRequest().body(ApiResult.of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(AuthenticationException401::class)
    fun authenticationException401(e: AuthenticationException401): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        val builder: ResponseEntity.BodyBuilder = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        if (e.exceptionCode == ExceptionCode.EXPIRED_TOKEN) {
            builder.header("token", "must-renew")
        }
        return builder.body(ApiResult.of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(AuthorityException403::class)
    fun authorityException403(e: AuthorityException403): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResult.of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(AuthorizationDeniedException::class, AccessDeniedException::class)
    fun authorizationDeniedException(e: AccessDeniedException?): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResult.of(ExceptionCode.UNKNOWN_AUTHORITY))
    }

    @ExceptionHandler(SystemException500::class)
    fun systemException500(e: SystemException500): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity
            .internalServerError()
            .body(ApiResult.of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun illegalArgumentException(e: IllegalArgumentException?): ResponseEntity<ApiResult<*>> {
        log.warn(LogUtils.getStackTrace(e))
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResult.of(ExceptionCode.INVALID_PARAMETER))
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun usernameNotFoundException(e: UsernameNotFoundException?): ResponseEntity<ApiResult<*>> = Result.unauthenticated()

    @ExceptionHandler(
        BindException::class,
    )
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
