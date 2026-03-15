package com.github.bestheroz.standard.common.util

import java.io.PrintWriter
import java.io.StringWriter

object LogUtils {
    fun getStackTrace(e: Throwable?): String {
        if (e == null) return ""
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return sw
            .toString()
            .lines()
            .filter { it.startsWith("\tat com.github.bestheroz") || !it.startsWith("\tat") }
            .joinToString("\n")
    }
}
