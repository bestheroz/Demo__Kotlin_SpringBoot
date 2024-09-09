package com.github.bestheroz.demo.entity

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.time.Instant

@Entity
@DiscriminatorValue("admin")
class Admin : IdCreatedUpdated {
    @Column(nullable = false)
    var loginId: String

    var password: String? = null
    var token: String? = null

    @Column(nullable = false)
    var name: String

    @Column(nullable = false)
    var useFlag: Boolean

    @Column(nullable = false)
    var managerFlag: Boolean

    @Convert(converter = AuthorityEnum.AuthorityEnumListConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    private var _authorities: List<AuthorityEnum>

    var changePasswordAt: Instant? = null
    var latestActiveAt: Instant? = null

    var joinedAt: Instant? = null

    @Column(nullable = false)
    var removedFlag: Boolean = false

    var removedAt: Instant? = null

    val type: UserTypeEnum
        get() = UserTypeEnum.ADMIN

    var authorities: List<AuthorityEnum>
        get() = if (managerFlag) AuthorityEnum.entries else _authorities
        set(value) {
            _authorities = value
        }

    constructor(
        loginId: String,
        password: String,
        name: String,
        useFlag: Boolean,
        managerFlag: Boolean,
        authorities: List<AuthorityEnum>,
        operator: Operator,
    ) {
        val now = Instant.now()
        this.loginId = loginId
        this.password = PasswordUtil.getPasswordHash(password)
        this.name = name
        this.useFlag = useFlag
        this.managerFlag = managerFlag
        this._authorities = authorities
        this.joinedAt = now
        this.removedFlag = false
        setCreatedBy(operator, now)
        setUpdatedBy(operator, now)
    }

    constructor(
        id: Long,
        loginId: String,
        name: String,
        managerFlag: Boolean,
    ) {
        this.id = id
        this.loginId = loginId
        this.name = name
        this.useFlag = false
        this.managerFlag = managerFlag
        this._authorities = emptyList()
    }

    companion object {
        fun fromOperator(operator: Operator) =
            Admin(
                operator.id,
                operator.loginId,
                operator.name,
                operator.managerFlag,
            )
    }

    fun update(
        loginId: String,
        password: String?,
        name: String,
        useFlag: Boolean,
        managerFlag: Boolean,
        authorities: List<AuthorityEnum>,
        operator: Operator,
    ) {
        this.loginId = loginId
        this.name = name
        this.useFlag = useFlag
        this.managerFlag = managerFlag
        this._authorities = authorities
        val now = Instant.now()
        setUpdatedBy(operator, now)
        if (!password.isNullOrEmpty()) {
            this.password = PasswordUtil.getPasswordHash(password)
            this.changePasswordAt = now
        }
    }

    fun changePassword(
        password: String,
        operator: Operator,
    ) {
        this.password = PasswordUtil.getPasswordHash(password)
        val now = Instant.now()
        this.changePasswordAt = now
        setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        removedFlag = true
        val now = Instant.now()
        removedAt = now
        setUpdatedBy(operator, now)
    }

    fun renewToken(token: String) {
        this.token = token
        latestActiveAt = Instant.now()
    }

    fun logout() {
        token = null
    }
}
