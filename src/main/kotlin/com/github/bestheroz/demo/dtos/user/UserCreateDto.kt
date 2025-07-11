package com.github.bestheroz.demo.dtos.user

import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.media.Schema

class UserCreateDto {
    data class Request(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        val loginId: String,
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED) val password: String,
        @Schema(description = "유저 이름", requiredMode = Schema.RequiredMode.REQUIRED) val name: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
        @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        val authorities: List<AuthorityEnum>,
    ) {
        fun toEntity(operator: Operator): User = User.of(this.loginId, this.password, this.name, this.useFlag, this.authorities, operator)
    }
}
