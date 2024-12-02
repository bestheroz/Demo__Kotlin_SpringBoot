package com.github.bestheroz.standard.common.exception

data class Unauthorized401Exception(
    val exceptionCode: ExceptionCode = ExceptionCode.UNKNOWN_AUTHENTICATION,
    val data: Any? = null,
) : RuntimeException()
