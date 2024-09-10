package com.github.bestheroz.demo.admin

import com.github.bestheroz.standard.common.enums.AuthorityEnum
import io.swagger.v3.oas.annotations.media.Schema

class AdminUpdateDto {
    data class Request(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        val loginId: String,
        @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        val password: String? = null,
        @Schema(description = "관리자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        val name: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
        @Schema(description = "매니저 여부(모든 권한 소유)", requiredMode = Schema.RequiredMode.REQUIRED)
        val managerFlag: Boolean,
        @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        val authorities: List<AuthorityEnum>,
    )
}
