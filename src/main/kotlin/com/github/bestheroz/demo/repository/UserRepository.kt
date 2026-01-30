package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository :
    JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {
    @EntityGraph(attributePaths = ["createdByAdmin", "createdByUser", "updatedByAdmin", "updatedByUser"])
    override fun findAll(
        spec: Specification<User>,
        pageable: Pageable,
    ): Page<User>

    @EntityGraph(attributePaths = ["createdByAdmin", "createdByUser", "updatedByAdmin", "updatedByUser"])
    override fun findById(id: Long): Optional<User>

    @EntityGraph(attributePaths = ["createdByAdmin", "createdByUser", "updatedByAdmin", "updatedByUser"])
    fun findByLoginIdAndRemovedFlagFalse(loginId: String): Optional<User>

    @EntityGraph(attributePaths = ["createdByAdmin", "createdByUser", "updatedByAdmin", "updatedByUser"])
    fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Optional<User>
}

object UserSpecification {
    fun removedFlagIsFalse(): Specification<User> =
        Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("removedFlag"), false)
        }

    fun equalId(id: Long): Specification<User> =
        Specification { root, _, cb ->
            cb.equal(root.get<Long>("id"), id)
        }

    fun containsLoginId(loginId: String): Specification<User> =
        Specification { root, _, cb ->
            cb.like(root.get<String>("loginId"), "%$loginId%")
        }

    fun containsName(name: String): Specification<User> =
        Specification { root, _, cb ->
            cb.like(root.get<String>("name"), "%$name%")
        }

    fun equalUseFlag(useFlag: Boolean): Specification<User> =
        Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("useFlag"), useFlag)
        }
}
