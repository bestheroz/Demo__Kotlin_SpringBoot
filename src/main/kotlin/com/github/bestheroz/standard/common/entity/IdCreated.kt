package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.*
import java.time.Instant

@MappedSuperclass
data class IdCreated(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null,
    @Column(updatable = false)
    private var createdAt: Instant,
    @Column(name = "created_object_type", updatable = false)
    private var createdObjectType: UserTypeEnum,
    @Column(name = "created_object_id", updatable = false)
    private var createdObjectId: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_object_id", referencedColumnName = "id", insertable = false, updatable = false)
    private var createdByAdmin: Admin? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_object_id", referencedColumnName = "id", insertable = false, updatable = false)
    private var createdByUser: User? = null,
) {
    fun setCreatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        if (operator.type == UserTypeEnum.ADMIN) {
            this.createdObjectType = UserTypeEnum.ADMIN
            this.createdByAdmin = Admin.fromOperator(operator)
        } else if (operator.type == UserTypeEnum.USER) {
            this.createdObjectType = UserTypeEnum.USER
            this.createdByUser = User.fromOperator(operator)
        }
        this.createdAt = instant
        this.createdObjectId = operator.id
        this.createdObjectType = operator.type
    }

    fun getCreatedBy(): UserSimpleDto =
        when (this.createdObjectType) {
            UserTypeEnum.ADMIN -> UserSimpleDto.fromEntity(this.createdByAdmin)
            UserTypeEnum.USER -> UserSimpleDto.fromEntity(this.createdByUser)
        }
}
