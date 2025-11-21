package com.github.bestheroz.standard.common.log

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Aspect
@Component
class TraceLogger(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = KotlinLogging.logger {}
        // 상수 제거 - KotlinLogging은 문자열 템플릿 직접 사용
    }

    @Around(
        """
        execution(!private * com.github.bestheroz..*Controller.*(..)) ||
        execution(!private * com.github.bestheroz..*Service.*(..)) ||
        execution(!private * com.github.bestheroz..*Repository.*(..))
        """,
    )
    @Throws(Throwable::class)
    fun writeLog(pjp: ProceedingJoinPoint): Any? {
        val signature =
            pjp.staticPart.signature
                .toString()
                .removePrefix(pjp.staticPart.signature.declaringType.`package`.name + ".")

        if (signature.containsAny("HealthController", "HealthRepository")) {
            return pjp.proceed()
        }

        val stopWatch = StopWatch(signature)
        stopWatch.start()

        return try {
            log.info { "$signature START ......." }

            val retVal = pjp.proceed()
            stopWatch.stop()

            when {
                signature.containsAny(
                    "Repository.",
                    "RepositoryCustom.",
                    ".domain.",
                    "SpecificationExecutor.",
                ) -> {
                    log.info { "$signature E N D [${stopWatch.totalTimeMillis}ms]" }
                }

                else -> {
                    log.info {
                        val returnValue =
                            retVal?.run {
                                val str = objectMapper.writeValueAsString(retVal)
                                str.abbreviate(1000, "--skip massive text-- total length : ${str.length}")
                            } ?: "null"
                        "$signature E N D [${stopWatch.totalTimeMillis}ms] - return: $returnValue"
                    }
                }
            }

            retVal
        } catch (e: Throwable) {
            if (stopWatch.isRunning) {
                stopWatch.stop()
            }
            log.info { "$signature THROW [${stopWatch.totalTimeMillis}ms]" }
            throw e
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean = StringUtils.containsAny(this, *substrings)

    private fun String?.abbreviate(
        maxWidth: Int,
        abbrevMarker: String,
    ): String = StringUtils.abbreviate(this ?: "null", abbrevMarker, maxWidth)
}
