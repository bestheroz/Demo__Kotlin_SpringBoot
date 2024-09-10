package com.github.bestheroz.standard.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ThreadContextElement
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@Configuration
class CoroutineConfig : DisposableBean {
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    @Bean
    fun coroutineScope(): CoroutineScope = coroutineScope

    override fun destroy() {
        job.cancel() // 스코프 취소
    }
}

class RequestContextElement(
    private val attributes: RequestAttributes,
) : AbstractCoroutineContextElement(Key),
    ThreadContextElement<RequestAttributes?> {
    companion object Key : CoroutineContext.Key<RequestContextElement>

    override fun updateThreadContext(context: CoroutineContext): RequestAttributes? {
        val oldState = RequestContextHolder.getRequestAttributes()
        RequestContextHolder.setRequestAttributes(attributes)
        return oldState
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: RequestAttributes?,
    ) {
        RequestContextHolder.setRequestAttributes(oldState)
    }
}

fun requestContextElement(): RequestContextElement {
    val attributes = RequestContextHolder.currentRequestAttributes()
    return RequestContextElement(attributes)
}

class SecurityContextElement(
    private val securityContext: SecurityContext,
) : AbstractCoroutineContextElement(Key),
    ThreadContextElement<SecurityContext?> {
    companion object Key : CoroutineContext.Key<SecurityContextElement>

    override fun updateThreadContext(context: CoroutineContext): SecurityContext? {
        val oldContext = SecurityContextHolder.getContext()
        SecurityContextHolder.setContext(securityContext)
        return oldContext
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: SecurityContext?,
    ) {
        SecurityContextHolder.setContext(oldState)
    }
}

fun securityContextElement(): SecurityContextElement {
    val securityContext = SecurityContextHolder.getContext()
    return SecurityContextElement(securityContext)
}
