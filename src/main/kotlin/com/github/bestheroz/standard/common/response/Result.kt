package com.github.bestheroz.standard.common.response

import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.response.ApiResult.Companion.of
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object Result {
    fun created(): ResponseEntity<*> = ResponseEntity.status(201).build<Any>()

    fun <T> created(data: T): ResponseEntity<T> = ResponseEntity.status(201).body(data)

    fun ok(): ResponseEntity<*> = ResponseEntity.noContent().build<Any>()

    fun <T> ok(data: T): ResponseEntity<T> = ResponseEntity.ok(data)

    fun error(): ResponseEntity<ApiResult<*>> =
        ResponseEntity
            .internalServerError()
            .body(of(ExceptionCode.UNKNOWN_SYSTEM_ERROR))

    fun unauthenticated(): ResponseEntity<ApiResult<*>> = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
}
