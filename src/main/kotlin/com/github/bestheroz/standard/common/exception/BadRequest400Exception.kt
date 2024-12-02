package com.github.bestheroz.standard.common.exception

data class BadRequest400Exception(
    val exceptionCode: ExceptionCode = ExceptionCode.INVALID_PARAMETER,
    val data: Any? = null,
) : RuntimeException()
