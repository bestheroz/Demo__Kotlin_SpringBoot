package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoticeRepository : JpaRepository<Notice, Long> {
    fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<Notice>
}
