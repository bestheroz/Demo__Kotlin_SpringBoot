package com.github.bestheroz.standard.common.exception

import com.github.bestheroz.standard.common.response.ApiResult
import com.github.bestheroz.standard.common.response.ApiResult.Companion.of
import com.github.bestheroz.standard.common.util.LogUtils
import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.lang.IllegalStateException

@RestControllerAdvice
class ApiExceptionHandler {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    // 아래서 놓친 예외가 있을때 이곳으로 확인하기 위해 존재한다.
    // 놓친 예외는 이곳에서 확인하여 추가해주면 된다.
    @ExceptionHandler(Throwable::class)
    fun exception(e: Throwable?): ResponseEntity<ApiResult<*>> {
        logger.error { LogUtils.getStackTrace(e) }
        return ResponseEntity.internalServerError().body(of(ExceptionCode.UNKNOWN_SYSTEM_ERROR))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun noResourceFoundException(e: NoResourceFoundException?): ResponseEntity<ApiResult<*>> {
        logger.error { LogUtils.getStackTrace(e) }
        return ResponseEntity.notFound().build<ApiResult<*>>()
    }

    @ExceptionHandler(BadRequest400Exception::class)
    fun requestException400(e: BadRequest400Exception): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.badRequest().body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(Unauthorized401Exception::class)
    fun authenticationException401(e: Unauthorized401Exception): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        val builder: ResponseEntity.BodyBuilder = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        when (e.exceptionCode) {
            ExceptionCode.EXPIRED_TOKEN -> builder.header("token", "must-renew")
            ExceptionCode.MISSING_AUTHENTICATION ->
                logger.error { "@CurrentUser annotation used without proper authentication" }
            else -> {}
        }
        return builder.body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(Forbidden403Exception::class)
    fun authorityException403(e: Forbidden403Exception): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(AuthorizationDeniedException::class, AccessDeniedException::class)
    fun authorizationDeniedException(e: AccessDeniedException?): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(of(ExceptionCode.UNKNOWN_AUTHORITY))
    }

    @ExceptionHandler(InternalServerError500Exception::class)
    fun systemException500(e: InternalServerError500Exception): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.internalServerError().body(of(e.exceptionCode, e.data))
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun illegalArgumentException(e: Throwable?): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(of(ExceptionCode.INVALID_PARAMETER))
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun usernameNotFoundException(e: UsernameNotFoundException?): ResponseEntity<ApiResult<*>> = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    @ExceptionHandler(BindException::class)
    fun bindException(e: Throwable?): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.badRequest().build()
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun missingServletRequestParameterException(
        e: MissingServletRequestParameterException,
    ): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(of(ExceptionCode.INVALID_PARAMETER))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun methodArgumentNotValidException(
        e: MethodArgumentNotValidException,
    ): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        val errors =
            e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn { "Validation failed: $errors" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(of(ExceptionCode.INVALID_PARAMETER, errors))
    }

    @ExceptionHandler(
        HttpMediaTypeNotAcceptableException::class,
        HttpMediaTypeNotSupportedException::class,
        HttpRequestMethodNotSupportedException::class,
        HttpClientErrorException::class,
    )
    fun httpMediaTypeNotAcceptableException(e: Throwable?): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.badRequest().build()
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun duplicateKeyException(e: DuplicateKeyException?): ResponseEntity<ApiResult<*>> {
        logger.warn { LogUtils.getStackTrace(e) }
        return ResponseEntity.badRequest().build()
    }
}
