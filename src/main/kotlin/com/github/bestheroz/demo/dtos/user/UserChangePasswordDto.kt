package com.github.bestheroz.demo.dtos.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class UserChangePasswordDto {
    data class Request(
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotBlank(message = "기존 비밀번호는 필수입니다")
        val oldPassword: String,
        @Schema(description = "새 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotBlank(message = "새 비밀번호는 필수입니다")
        @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
        val newPassword: String,
    )
}
