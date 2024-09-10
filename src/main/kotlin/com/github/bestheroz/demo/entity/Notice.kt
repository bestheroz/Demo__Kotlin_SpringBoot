package com.github.bestheroz.demo.entity

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import java.time.Instant

@Entity
data class Notice(
    @Column(nullable = false) var title: String,
    @Column(nullable = false) var content: String,
    @Column(nullable = false) var useFlag: Boolean,
    @Column(nullable = false) var removedFlag: Boolean = false,
    private var removedAt: Instant? = null,
) : IdCreatedUpdated() {
    companion object {
        fun of(
            title: String,
            content: String,
            useFlag: Boolean,
            operator: Operator,
        ) = Notice(
            title = title,
            content = content,
            useFlag = useFlag,
        ).apply {
            val now = Instant.now()
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }
    }

    fun update(
        title: String,
        content: String,
        useFlag: Boolean,
        operator: Operator,
    ) {
        this.title = title
        this.content = content
        this.useFlag = useFlag
        val now = Instant.now()
        this.setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        this.removedFlag = true
        val now = Instant.now()
        this.removedAt = now
        this.setUpdatedBy(operator, now)
    }
}
