package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.*
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
    @JoinColumn(
        name = "updated_object_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
    )
    var updatedByAdmin: Admin? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "updated_object_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false,
    )
    var updatedByUser: User? = null

    fun setUpdatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        when (operator.type) {
            UserTypeEnum.ADMIN -> {
                updatedObjectType = UserTypeEnum.ADMIN
                updatedByAdmin = Admin.of(operator)
            }
            UserTypeEnum.USER -> {
                updatedObjectType = UserTypeEnum.USER
                updatedByUser = User.of(operator)
            }
        }
        updatedAt = instant
        updatedObjectId = operator.id
        updatedObjectType = operator.type
    }

    val updatedBy: UserSimpleDto
        get() =
            when (updatedObjectType) {
                UserTypeEnum.ADMIN -> UserSimpleDto.of(updatedByAdmin!!)
                UserTypeEnum.USER -> UserSimpleDto.of(updatedByUser!!)
            }
}
