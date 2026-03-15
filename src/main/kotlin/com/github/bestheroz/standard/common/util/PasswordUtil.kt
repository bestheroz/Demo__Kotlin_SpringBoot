package com.github.bestheroz.standard.common.util

import org.springframework.security.crypto.bcrypt.BCrypt

object PasswordUtil {
    fun isPasswordValid(
        plainPassword: String,
        hashedPassword: String,
    ): Boolean = BCrypt.checkpw(plainPassword.take(72), hashedPassword)

    fun getPasswordHash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
}
