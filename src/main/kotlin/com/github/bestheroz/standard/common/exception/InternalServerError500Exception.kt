package com.github.bestheroz.standard.common.exception

data class InternalServerError500Exception(
    val exceptionCode: ExceptionCode = ExceptionCode.UNKNOWN_SYSTEM_ERROR,
    val data: Any? = null,
) : RuntimeException()
