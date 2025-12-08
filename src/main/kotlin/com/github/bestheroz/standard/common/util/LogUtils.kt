package com.github.bestheroz.standard.common.util

import org.apache.commons.lang3.exception.ExceptionUtils

object LogUtils {
    fun getStackTrace(e: Throwable?): String =
        ExceptionUtils
            .getStackFrames(e)
            .filter { it.startsWith("\tat com.github.bestheroz") || !it.startsWith("\tat") }
            .joinToString("\n")
}
