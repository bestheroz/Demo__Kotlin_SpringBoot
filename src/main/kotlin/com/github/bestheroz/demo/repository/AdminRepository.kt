package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.Admin
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AdminRepository : JpaRepository<Admin, Long> {
    fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<Admin>

    fun findByLoginIdAndRemovedFlagFalse(loginId: String): Optional<Admin>

    fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Optional<Admin>
}
