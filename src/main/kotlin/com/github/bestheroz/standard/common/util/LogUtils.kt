package com.github.bestheroz.standard.common.util

object LogUtils {
    fun getStackTrace(throwable: Throwable): String {
        return throwable.stackTrace
            .asSequence()
            .filter { stackTraceElement ->
                stackTraceElement.className.startsWith("com.github.bestheroz") ||
                        !stackTraceElement.toString().startsWith("\tat")
            }
            .joinToString(separator = "\n") { it.toString() }
    }
}
