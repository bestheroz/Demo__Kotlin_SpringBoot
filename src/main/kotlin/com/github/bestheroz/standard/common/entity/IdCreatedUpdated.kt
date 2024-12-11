package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.Column
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
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
                    value = "CASE WHEN updated_object_type = 'admin' THEN updated_object_id ELSE null END",
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
                    value = "CASE WHEN updated_object_type = 'user' THEN updated_object_id ELSE null END",
                    referencedColumnName = "id",
                ),
        ),
    )
    var updatedByUser: User? = null

    fun setUpdatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        updatedAt = instant
        updatedObjectId = operator.id
        updatedObjectType = operator.type
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                updatedByAdmin = Admin.of(operator)
            }
            UserTypeEnum.USER -> {
                updatedByUser = User.of(operator)
            }
        }
    }

    val updatedBy: UserSimpleDto
        get() =
            when (updatedObjectType) {
                UserTypeEnum.ADMIN -> UserSimpleDto.of(updatedByAdmin!!)
                UserTypeEnum.USER -> UserSimpleDto.of(updatedByUser!!)
            } ?: throw IllegalStateException("Updated user not found")
}
