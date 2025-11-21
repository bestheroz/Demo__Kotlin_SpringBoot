package com.github.bestheroz.standard.common.domain

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.Column
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.Instant

@MappedSuperclass
abstract class IdCreatedUpdated : IdCreated() {
    @Column(nullable = false)
    lateinit var updatedObjectType: UserTypeEnum

    @Column(nullable = false)
    lateinit var updatedAt: Instant

    @Column(name = "updated_object_id", nullable = false)
    var updatedObjectId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas(
        JoinColumnOrFormula(
            formula =
                JoinFormula(
                    value = "CASE WHEN updated_object_type = 'ADMIN' THEN updated_object_id ELSE null END",
                    referencedColumnName = "id",
                ),
        ),
    )
    var updatedByAdmin: Admin? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas(
        JoinColumnOrFormula(
            formula =
                JoinFormula(
                    value = "CASE WHEN updated_object_type = 'USER' THEN updated_object_id ELSE null END",
                    referencedColumnName = "id",
                ),
        ),
    )
    var updatedByUser: User? = null

    @Transient var updater: Operator? = null

    fun setUpdatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        updatedAt = instant
        updatedObjectId = operator.id
        updatedObjectType = operator.type
        updater = operator
    }

    val updatedBy: UserSimpleDto
        get() =
            when (updatedObjectType) {
                UserTypeEnum.ADMIN -> {
                    updater?.let(UserSimpleDto::of)
                        ?: updatedByAdmin?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither updatedByAdmin nor updater exists")
                }

                UserTypeEnum.USER -> {
                    updater?.let(UserSimpleDto::of)
                        ?: updatedByUser?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither updatedByUser nor updater exists")
                }
            }
}
