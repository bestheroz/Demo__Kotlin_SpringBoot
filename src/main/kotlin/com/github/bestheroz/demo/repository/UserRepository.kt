package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findAllByRemovedFlagIsFalse(pageable: Pageable): Page<User>

    fun findByLoginIdAndRemovedFlagFalse(loginId: String): Optional<User>

    fun findByLoginIdAndRemovedFlagFalseAndIdNot(
        loginId: String,
        id: Long?,
    ): Optional<User>
}
