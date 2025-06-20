package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository :
    JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {
    fun findByLoginIdAndRemovedFlagFalse(loginId: String): Optional<User>

    fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Optional<User>
}

object UserSpecification {
    fun removedFlagIsFalse(): org.springframework.data.jpa.domain.Specification<User> =
        org.springframework.data.jpa.domain.Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("removedFlag"), false)
        }

    fun equalId(id: Long): org.springframework.data.jpa.domain.Specification<User> =
        org.springframework.data.jpa.domain.Specification { root, _, cb ->
            cb.equal(root.get<Long>("id"), id)
        }

    fun containsLoginId(loginId: String): org.springframework.data.jpa.domain.Specification<User> =
        org.springframework.data.jpa.domain.Specification { root, _, cb ->
            cb.like(root.get<String>("loginId"), "%$loginId%")
        }

    fun containsName(name: String): org.springframework.data.jpa.domain.Specification<User> =
        org.springframework.data.jpa.domain.Specification { root, _, cb ->
            cb.like(root.get<String>("name"), "%$name%")
        }

    fun equalUseFlag(useFlag: Boolean): org.springframework.data.jpa.domain.Specification<User> =
        org.springframework.data.jpa.domain.Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("useFlag"), useFlag)
        }
}
