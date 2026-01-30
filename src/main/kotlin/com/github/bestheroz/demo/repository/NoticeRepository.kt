package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NoticeRepository :
    JpaRepository<Notice, Long>,
    JpaSpecificationExecutor<Notice> {
    @EntityGraph(attributePaths = ["createdByAdmin", "createdByUser", "updatedByAdmin", "updatedByUser"])
    override fun findAll(
        spec: Specification<Notice>,
        pageable: Pageable,
    ): Page<Notice>

    @EntityGraph(attributePaths = ["createdByAdmin", "createdByUser", "updatedByAdmin", "updatedByUser"])
    override fun findById(id: Long): Optional<Notice>
}

object NoticeSpecification {
    fun removedFlagIsFalse(): Specification<Notice> =
        Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("removedFlag"), false)
        }

    fun equalId(id: Long): Specification<Notice> =
        Specification { root, _, cb ->
            cb.equal(root.get<Long>("id"), id)
        }

    fun containsTitle(title: String): Specification<Notice> =
        Specification { root, _, cb ->
            cb.like(root.get<String>("title"), "%$title%")
        }

    fun equalUseFlag(useFlag: Boolean): Specification<Notice> =
        Specification { root, _, cb ->
            cb.equal(root.get<Boolean>("useFlag"), useFlag)
        }
}
