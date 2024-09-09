package com.github.bestheroz.standard.common.util

import org.springframework.util.Assert
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object DateUtils {
    fun toStringNow(pattern: String): String {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return toString(Instant.now(), pattern)
    }

    fun toString(
        instant: Instant?,
        pattern: String,
    ): String {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return toString(instant, pattern, ZoneId.of("UTC"))
    }

    fun toString(
        instant: Instant?,
        pattern: String,
        zoneId: ZoneId,
    ): String {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return instant?.let {
            OffsetDateTime.ofInstant(it, zoneId).format(DateTimeFormatter.ofPattern(pattern))
        } ?: ""
    }

    fun toString(
        timestamp: Long?,
        pattern: String,
    ): String {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return toString(timestamp, pattern, ZoneId.of("UTC"))
    }

    fun toString(
        timestamp: Long?,
        pattern: String,
        zoneId: ZoneId,
    ): String {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return timestamp?.let {
            OffsetDateTime
                .ofInstant(Instant.ofEpochMilli(it), zoneId)
                .format(DateTimeFormatter.ofPattern(pattern))
        } ?: ""
    }

    fun toString(
        date: Date?,
        pattern: String,
    ): String {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return date?.let { toString(it.time, pattern, ZoneId.of("UTC")) } ?: ""
    }

    fun toString(
        string: String?,
        fromPattern: String,
        toPattern: String,
    ): String {
        Assert.hasText(fromPattern, "fromPattern parameter must not be empty or null")
        Assert.hasText(toPattern, "toPattern parameter must not be empty or null")
        return string?.let {
            parseOffsetDateTime(it, fromPattern)?.format(DateTimeFormatter.ofPattern(toPattern))
        } ?: ""
    }

    fun parseOffsetDateTime(
        text: String?,
        pattern: String,
    ): OffsetDateTime? {
        Assert.hasText(pattern, "pattern parameter must not be empty or null")
        return text?.let {
            OffsetDateTime.parse(it, DateTimeFormatter.ofPattern(pattern))
        }
    }

    fun parseOffsetDateTimeAtUTC(text: String?): OffsetDateTime? = text?.let { OffsetDateTime.parse(it) }
}
