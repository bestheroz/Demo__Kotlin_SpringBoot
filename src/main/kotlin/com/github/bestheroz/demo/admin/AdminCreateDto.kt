package com.github.bestheroz.demo.admin

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.media.Schema

class AdminCreateDto {
    data class Request(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        val loginId: String,
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        val password: String,
        @Schema(description = "관리자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        val name: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
        @Schema(description = "매니저 여부(모든 권한 소유)", requiredMode = Schema.RequiredMode.REQUIRED)
        val managerFlag: Boolean,
        @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        val authorities: List<AuthorityEnum>,
    ) {
        fun toEntity(operator: Operator): Admin =
            Admin.of(
                this.loginId,
                this.password,
                this.name,
                this.useFlag,
                this.managerFlag,
                this.authorities,
                operator,
            )
    }
}
