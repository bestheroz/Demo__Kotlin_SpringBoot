package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.exception.AuthenticationException401
import com.github.bestheroz.standard.common.exception.ExceptionCode
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Aspect
@Component
class CurrentUserAspect {
    @Around("execution(* com.github.bestheroz..*(.., @com.github.bestheroz.standard.common.authenticate.CurrentUser (*), ..))")
    @Throws(Throwable::class)
    fun checkCurrentUser(joinPoint: ProceedingJoinPoint): Any? {
        SecurityContextHolder.getContext().authentication.takeIf { it.isAuthenticated && it.principal != null }
            ?: throw AuthenticationException401(ExceptionCode.EXPIRED_TOKEN)

        return joinPoint.proceed()
    }
}
