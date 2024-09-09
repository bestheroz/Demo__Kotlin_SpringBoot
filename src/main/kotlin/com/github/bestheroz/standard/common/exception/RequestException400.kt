package com.github.bestheroz.standard.common.exception

data class RequestException400(
    val exceptionCode: ExceptionCode = ExceptionCode.INVALID_PARAMETER,
    val data: Any? = null,
) : RuntimeException()
