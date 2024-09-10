package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.*
import java.time.Instant

@MappedSuperclass
abstract class IdCreated {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false, updatable = false)
    lateinit var createdAt: Instant

    @Column(nullable = false, updatable = false)
    lateinit var createdObjectType: UserTypeEnum

    @Column(name = "created_object_id", nullable = false, updatable = false)
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
                createdByAdmin = Admin.of(operator)
            }
            UserTypeEnum.USER -> {
                createdObjectType = UserTypeEnum.USER
                createdByUser = User.of(operator)
            }
        }
        createdAt = instant
        createdObjectId = operator.id
        createdObjectType = operator.type
    }

    val createdBy: UserSimpleDto
        get() =
            when (createdObjectType) {
                UserTypeEnum.ADMIN -> UserSimpleDto.of(createdByAdmin!!)
                UserTypeEnum.USER -> UserSimpleDto.of(createdByUser!!)
            }
}
