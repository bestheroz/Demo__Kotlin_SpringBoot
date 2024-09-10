package com.github.bestheroz.standard.common.log

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.StopWatch
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class TraceLogger(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = logger()
        private const val STR_START_EXECUTE_TIME = "{} START ......."
        private const val STR_END_EXECUTE_TIME = "{} E N D [{}ms] - return: {}"
        private const val STR_END_EXECUTE_TIME_FOR_REPOSITORY = "{} E N D [{}ms]"
        private const val STR_END_EXECUTE_TIME_FOR_EXCEPTION = "{} THROW [{}ms]"
    }

    @Around(
        "execution(!private * com.github.bestheroz..*Controller.*(..)) || " +
            "execution(!private * com.github.bestheroz..*Service.*(..)) || " +
            "execution(!private * com.github.bestheroz..*Repository.*(..))",
    )
    @Throws(Throwable::class)
    fun writeLog(pjp: ProceedingJoinPoint): Any? {
        val signature =
            pjp.staticPart.signature.toString().removePrefix(
                pjp.staticPart.signature.declaringType.`package`.name + ".",
            )
        val stopWatch = StopWatch()
        stopWatch.start()

        return try {
            if (!signature.containsAny("HealthController", "HealthRepository")) {
                log.info(STR_START_EXECUTE_TIME, signature)
            }

            val retVal = pjp.proceed()
            stopWatch.stop()

            when {
                signature.containsAny("Repository.", "RepositoryCustom.", ".domain.") -> {
                    if (!signature.contains("HealthRepository")) {
                        log.info(STR_END_EXECUTE_TIME_FOR_REPOSITORY, signature, stopWatch.time)
                    }
                }
                !signature.contains("HealthController") -> {
                    log.info(
                        STR_END_EXECUTE_TIME,
                        signature,
                        stopWatch.time,
                        retVal?.let {
                            val str = objectMapper.writeValueAsString(retVal)
                            str.abbreviate(1000, "--skip massive text-- total length : ${str.length}")
                        } ?: "null",
                    )
                }
            }

            retVal
        } catch (e: Throwable) {
            if (stopWatch.isStarted) {
                stopWatch.stop()
            }
            log.info(STR_END_EXECUTE_TIME_FOR_EXCEPTION, signature, stopWatch.time)
            throw e
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean = StringUtils.containsAny(this, *substrings)

    private fun String.abbreviate(
        maxWidth: Int,
        abbrevMarker: String,
    ): String = StringUtils.abbreviate(StringUtils.defaultString(this, "null"), abbrevMarker, maxWidth)
}
