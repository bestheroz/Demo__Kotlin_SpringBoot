package com.github.bestheroz.standard.common.util

import org.springframework.security.crypto.bcrypt.BCrypt

object PasswordUtil {
    fun verifyPassword(
        plainPassword: String,
        hashedPassword: String,
    ): Boolean = BCrypt.checkpw(plainPassword, hashedPassword)

    fun getPasswordHash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
}
