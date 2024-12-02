package com.github.bestheroz.standard.context

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import kotlin.coroutines.CoroutineContext

// 컨텍스트 요소 정의
class RequestContextElement(
    val requestAttributes: RequestAttributes,
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<RequestContextElement>

    override val key: CoroutineContext.Key<RequestContextElement> = Key
}

fun requestContextElement(): RequestContextElement =
    RequestContextElement(
        RequestContextHolder.getRequestAttributes()
            ?: throw IllegalStateException("RequestAttributes not found in context"),
    )

// 확장 함수로 컨텍스트 접근 제공
fun CoroutineContext.requestAttributes(): RequestAttributes? = this[RequestContextElement]?.requestAttributes

class SecurityContextElement(
    val securityContext: SecurityContext,
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<SecurityContextElement>

    override val key: CoroutineContext.Key<SecurityContextElement> = Key
}

fun securityContextElement(): SecurityContextElement = SecurityContextElement(SecurityContextHolder.getContext())

fun CoroutineContext.securityContext(): SecurityContext? = this[SecurityContextElement]?.securityContext
