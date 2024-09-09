package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.util.EnvironmentUtils.isLocal
import com.p6spy.engine.logging.Category
import com.p6spy.engine.spy.P6SpyOptions
import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.hibernate.engine.jdbc.internal.FormatStyle
import org.springframework.context.annotation.Configuration
import java.text.MessageFormat

@Configuration
class P6spyConfig {
    @PostConstruct
    fun setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().logMessageFormat = P6spyPrettySqlFormatter::class.java.name
    }

    class P6spyPrettySqlFormatter : MessageFormattingStrategy {
        override fun formatMessage(
            connectionId: Int,
            now: String,
            elapsed: Long,
            category: String,
            prepared: String,
            sql: String,
            url: String,
        ): String =
            if (StringUtils.equals(sql, "select now()")) {
                MessageFormat.format(
                    "OperationTime: {0}ms | connectionId : {1} | {2} | readiness: {3}",
                    elapsed,
                    connectionId,
                    category,
                    sql,
                )
            } else {
                MessageFormat.format(
                    "OperationTime: {0}ms | connectionId : {1} | {2}{3}\n",
                    elapsed,
                    connectionId,
                    category,
                    if (StringUtils.isEmpty(sql)) {
                        ""
                    } else {
                        "\n" +
                            this.formatSql(
                                category,
                                sql,
                            )
                    },
                )
            }

        private fun formatSql(
            category: String,
            sql: String,
        ): String {
            if (StringUtils.isEmpty(sql)) {
                return StringUtils.EMPTY
            }
            if (Category.STATEMENT.name == category) {
                if (isLocal()) {
                    return if (StringUtils.startsWithAny("create", "alter", "comment")) {
                        FormatStyle.DDL.formatter.format(sql)
                    } else {
                        FormatStyle.HIGHLIGHT
                            .formatter
                            .format(FormatStyle.BASIC.formatter.format(sql))
                    }
                }
            }
            return sql
        }
    }
}
