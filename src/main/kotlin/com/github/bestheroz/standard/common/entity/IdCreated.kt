package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.*
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.Instant

@MappedSuperclass
abstract class IdCreated {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @Column(nullable = false, updatable = false)
    lateinit var createdObjectType: UserTypeEnum

    @Column(name = "created_object_id", nullable = false, updatable = false)
    var createdObjectId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas(
        JoinColumnOrFormula(
            formula =
                JoinFormula(
                    value = "CASE WHEN created_object_type = 'admin' THEN created_object_id ELSE null END",
                    referencedColumnName = "id",
                ),
        ),
    )
    var createdByAdmin: Admin? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas(
        JoinColumnOrFormula(
            formula =
                JoinFormula(
                    value = "CASE WHEN created_object_type = 'user' THEN created_object_id ELSE null END",
                    referencedColumnName = "id",
                ),
        ),
    )
    var createdByUser: User? = null

    fun setCreatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        createdAt = instant
        createdObjectId = operator.id
        createdObjectType = operator.type
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                createdByAdmin = Admin.of(operator)
            }
            UserTypeEnum.USER -> {
                createdByUser = User.of(operator)
            }
        }
    }

    val createdBy: UserSimpleDto
        get() =
            when (createdObjectType) {
                UserTypeEnum.ADMIN -> UserSimpleDto.of(createdByAdmin!!)
                UserTypeEnum.USER -> UserSimpleDto.of(createdByUser!!)
            }
}
