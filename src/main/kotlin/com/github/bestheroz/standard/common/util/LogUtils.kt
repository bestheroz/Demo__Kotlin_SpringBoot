package com.github.bestheroz.standard.common.util

import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*
import java.util.stream.Collectors

object LogUtils {
    fun getStackTrace(e: Throwable?): String =
        Arrays
            .stream(ExceptionUtils.getStackFrames(e))
            .filter { item: String -> item.startsWith("\tat com.github.bestheroz") || !item.startsWith("\tat") }
            .collect(Collectors.joining("\n"))
}
