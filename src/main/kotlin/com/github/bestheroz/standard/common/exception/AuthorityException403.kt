package com.github.bestheroz.standard.common.exception

data class AuthorityException403(
    val exceptionCode: ExceptionCode = ExceptionCode.UNKNOWN_AUTHORITY,
    val data: Any? = null,
) : RuntimeException()
