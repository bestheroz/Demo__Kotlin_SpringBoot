package com.github.bestheroz.standard.common.exception

data class TooManyRequests429Exception(
    val exceptionCode: ExceptionCode = ExceptionCode.CONCURRENCY_ERROR,
    val data: Any? = null,
) : RuntimeException()
