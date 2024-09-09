package com.github.bestheroz.standard.common.exception

data class AuthenticationException401(
    val exceptionCode: ExceptionCode = ExceptionCode.UNKNOWN_AUTHENTICATION,
    val data: Any? = null,
) : RuntimeException()
