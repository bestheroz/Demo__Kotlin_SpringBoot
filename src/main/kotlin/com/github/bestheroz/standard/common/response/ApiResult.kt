package com.github.bestheroz.standard.common.response

import com.github.bestheroz.standard.common.exception.ExceptionCode

data class ApiResult<T>(
    val code: String,
    val message: String,
    val data: T,
) {
    companion object {
        fun of(exceptionCode: ExceptionCode): ApiResult<*> = of<Any?>(exceptionCode, null)

        fun <T> of(
            exceptionCode: ExceptionCode,
            data: T,
        ): ApiResult<T> = ApiResult<T>(exceptionCode.name, exceptionCode.getMessage(), data)
    }
}
