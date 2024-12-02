package com.github.bestheroz.standard.common.exception

data class Forbidden403Exception(
    val exceptionCode: ExceptionCode = ExceptionCode.UNKNOWN_AUTHORITY,
    val data: Any? = null,
) : RuntimeException()
