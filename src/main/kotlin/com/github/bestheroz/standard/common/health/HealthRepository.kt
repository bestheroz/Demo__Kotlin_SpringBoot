package com.github.bestheroz.standard.common.health

import com.github.bestheroz.demo.domain.Admin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface HealthRepository : JpaRepository<Admin, Long> {
    @Query(value = "select now()", nativeQuery = true)
    fun selectNow()
}
