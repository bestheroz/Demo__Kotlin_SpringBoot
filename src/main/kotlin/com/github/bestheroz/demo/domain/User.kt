package com.github.bestheroz.demo.domain

import com.github.bestheroz.standard.common.domain.IdCreatedUpdated
import com.github.bestheroz.standard.common.domain.converter.JsonAttributeConverter
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.enums.AuthorityEnum.AuthorityEnumListConverter
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import com.github.bestheroz.standard.common.security.Operator
import com.github.bestheroz.standard.common.util.PasswordUtil.getPasswordHash
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.time.Instant

@Entity(name = "user")
@DiscriminatorValue("user")
data class User(
    @Column(nullable = false) var loginId: String = "",
    var password: String? = null,
    var token: String? = null,
    @Column(nullable = false) var name: String = "",
    @Column(nullable = false) var useFlag: Boolean = false,
    @Convert(converter = AuthorityEnumListConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    var authorities: List<AuthorityEnum> = mutableListOf(),
    var changePasswordAt: Instant? = null,
    var latestActiveAt: Instant? = null,
    var joinedAt: Instant? = null,
    @Convert(converter = JsonAttributeConverter::class)
    @Column(columnDefinition = "json", nullable = false)
    var additionalInfo: Map<String, Any> = mutableMapOf(),
    @Column(nullable = false) var removedFlag: Boolean = false,
    var removedAt: Instant? = null,
) : IdCreatedUpdated() {
    fun getType(): UserTypeEnum = UserTypeEnum.USER

    companion object {
        fun of(
            loginId: String,
            password: String,
            name: String,
            useFlag: Boolean,
            authorities: List<AuthorityEnum>,
            operator: Operator,
        ) = User(
            loginId = loginId,
            name = name,
            useFlag = useFlag,
            authorities = authorities,
            additionalInfo = mapOf(),
        ).apply {
            val now = Instant.now()
            this.password = getPasswordHash(password)
            this.joinedAt = now
            this.removedFlag = false
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }

        fun of(operator: Operator) =
            User(
                loginId = operator.loginId,
                name = operator.name,
                useFlag = false,
                authorities = listOf(),
                additionalInfo = mapOf(),
            ).apply { this.id = operator.id }
    }

    fun update(
        loginId: String,
        password: String?,
        name: String,
        useFlag: Boolean,
        authorities: List<AuthorityEnum>,
        operator: Operator,
    ) {
        this.loginId = loginId
        this.name = name
        this.useFlag = useFlag
        this.authorities = authorities
        val now = Instant.now()
        this.setUpdatedBy(operator, now)
        password?.let {
            this.password = getPasswordHash(it)
            this.changePasswordAt = now
        }
    }

    fun changePassword(
        password: String,
        operator: Operator,
    ) {
        this.password = getPasswordHash(password)
        val now = Instant.now()
        this.changePasswordAt = now
        this.setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        this.removedFlag = true
        val now = Instant.now()
        this.removedAt = now
        this.setUpdatedBy(operator, now)
    }

    fun renewToken(token: String?) {
        this.token = token
        this.latestActiveAt = Instant.now()
    }

    fun logout() {
        this.token = null
    }
}
