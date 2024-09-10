package com.github.bestheroz.demo.user

import io.swagger.v3.oas.annotations.media.Schema

class UserChangePasswordDto {
    data class Request(
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        val oldPassword: String,
        @Schema(description = "새 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        val newPassword: String,
    )
}
