package com.github.bestheroz.demo.dtos.user

import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class UserCreateDto {
    data class Request(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotBlank(message = "로그인 아이디는 필수입니다")
        @field:Size(min = 3, max = 50, message = "로그인 아이디는 3-50자여야 합니다")
        @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "로그인 아이디는 영문, 숫자, 언더스코어만 사용 가능합니다")
        val loginId: String,
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotBlank(message = "비밀번호는 필수입니다")
        @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
        val password: String,
        @Schema(description = "유저 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotBlank(message = "유저 이름은 필수입니다")
        @field:Size(min = 2, max = 50, message = "유저 이름은 2-50자여야 합니다")
        val name: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotNull(message = "사용 여부는 필수입니다")
        val useFlag: Boolean,
        @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @field:NotEmpty(message = "권한 목록은 필수입니다")
        val authorities: List<AuthorityEnum>,
    ) {
        fun toEntity(operator: Operator): User = User.of(this.loginId, this.password, this.name, this.useFlag, this.authorities, operator)
    }
}
