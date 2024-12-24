package com.github.bestheroz.standard.common.entity

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.*
import jakarta.persistence.Transient
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
                    value = "CASE WHEN created_object_type = 'ADMIN' THEN created_object_id ELSE null END",
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
                    value = "CASE WHEN created_object_type = 'USER' THEN created_object_id ELSE null END",
                    referencedColumnName = "id",
                ),
        ),
    )
    var createdByUser: User? = null

    @Transient var creator: Operator? = null

    fun setCreatedBy(
        operator: Operator,
        instant: Instant,
    ) {
        createdAt = instant
        createdObjectId = operator.id
        createdObjectType = operator.type
        creator = operator
    }

    val createdBy: UserSimpleDto
        get() =
            when (createdObjectType) {
                UserTypeEnum.ADMIN ->
                    creator?.let(UserSimpleDto::of)
                        ?: createdByAdmin?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither createdByAdmin nor creator exists")
                UserTypeEnum.USER ->
                    creator?.let(UserSimpleDto::of)
                        ?: createdByUser?.let(UserSimpleDto::of)
                        ?: throw IllegalStateException("Neither createdByUser nor creator exists")
            }
}
