package com.github.bestheroz.standard.common.util

import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.Arrays
import java.util.stream.Collectors

object LogUtils {
    fun getStackTrace(e: Throwable?): String =
        Arrays
            .stream(ExceptionUtils.getStackFrames(e))
            .filter { it.startsWith("\tat com.github.bestheroz") || !it.startsWith("\tat") }
            .collect(Collectors.joining("\n"))
}
