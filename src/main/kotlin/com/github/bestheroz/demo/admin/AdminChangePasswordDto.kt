package com.github.bestheroz.demo.admin

import io.swagger.v3.oas.annotations.media.Schema

class AdminChangePasswordDto {
    data class Request(
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        val oldPassword: String,
        @Schema(description = "새 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        val newPassword: String,
    )
}
