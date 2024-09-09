package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.*
import java.time.Instant
import javax.persistence.*

@MappedSuperclass
open class IdCreated {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(updatable = false)
    var createdAt: Instant? = null

    @Column(name = "created_object_type", updatable = false)
    var createdObjectType: UserTypeEnum? = null

    @Column(name = "created_object_id", updatable = false)
    var createdObjectId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "created_object_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
    )
    var createdByAdmin: Admin? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "created_object_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
    )
    var createdByUser: User? = null

    fun setCreatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                createdObjectType = UserTypeEnum.ADMIN
                createdByAdmin = Admin.fromOperator(operator)
            }
            UserTypeEnum.USER -> {
                createdObjectType = UserTypeEnum.USER
                createdByUser = User.fromOperator(operator)
            }
        }
        createdAt = instant
        createdObjectId = operator.id
        createdObjectType = operator.type
    }

    val createdBy: UserSimpleDto
        get() =
            when (createdObjectType) {
                UserTypeEnum.ADMIN -> UserSimpleDto.fromEntity(createdByAdmin!!)
                UserTypeEnum.USER -> UserSimpleDto.fromEntity(createdByUser!!)
                else -> throw IllegalStateException("Unknown user type")
            }
}
