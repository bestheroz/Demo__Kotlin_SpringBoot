package com.github.bestheroz.standard.common.authenticate

import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.Unauthorized401Exception
import com.github.bestheroz.standard.common.log.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Aspect
@Component
class CurrentUserAspect {
    companion object {
        private val log = logger()
    }

    @Around(
        "execution(* com.github.bestheroz..*(.., @com.github.bestheroz.standard.common.authenticate.CurrentUser (*), ..))",
    )
    @Throws(Throwable::class)
    fun checkCurrentUser(joinPoint: ProceedingJoinPoint): Any? {
        val authentication = SecurityContextHolder.getContext().authentication

        if (
            authentication == null || !authentication.isAuthenticated || authentication.principal == null
        ) {
            log.error(
                "@CurrentUser 코드 누락됨 - Authentication missing or invalid for method: ${joinPoint.signature.name}",
            )
            throw Unauthorized401Exception(ExceptionCode.MISSING_AUTHENTICATION)
        }

        return joinPoint.proceed()
    }
}
