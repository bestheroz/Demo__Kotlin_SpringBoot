package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.Notice
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface NoticeRepository :
    JpaRepository<Notice, Long>,
    JpaSpecificationExecutor<Notice>

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
