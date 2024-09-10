package com.github.bestheroz.demo.admin

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.standard.common.dto.IdCreatedUpdatedDto
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

class AdminDto {
    data class Request(
        @Schema(description = "페이지 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        val page: Int,
        @Schema(description = "페이지 크기", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        val pageSize: Int,
    )

    data class Response(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        val loginId: String,
        @Schema(description = "관리자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        val name: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
        @Schema(description = "매니저 여부(모든 권한 소유)", requiredMode = Schema.RequiredMode.REQUIRED)
        val managerFlag: Boolean,
        @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        val authorities: List<AuthorityEnum>,
        @Schema(description = "가입 일시", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        val joinedAt: Instant? = null,
        @Schema(description = "최근 활동 일시", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        val latestActiveAt: Instant? = null,
        @Schema(description = "비밀번호 변경 일시", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        val changePasswordAt: Instant? = null,
        @Schema(description = "ID(KEY)", requiredMode = Schema.RequiredMode.REQUIRED)
        override val id: Long,
        @Schema(
            description = "생성일시",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        override val createdAt: Instant,
        @Schema(
            description = "생성자",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        override val createdBy: UserSimpleDto,
        @Schema(description = "수정일시", requiredMode = Schema.RequiredMode.REQUIRED)
        override val updatedAt: Instant,
        @Schema(description = "수정자", requiredMode = Schema.RequiredMode.REQUIRED)
        override val updatedBy: UserSimpleDto,
    ) : IdCreatedUpdatedDto(id, createdAt, createdBy, updatedAt, updatedBy) {
        companion object {
            fun of(admin: Admin): Response =
                Response(
                    id = admin.id!!,
                    loginId = admin.loginId,
                    name = admin.name,
                    useFlag = admin.useFlag,
                    managerFlag = admin.managerFlag,
                    authorities = admin.authorities,
                    joinedAt = admin.joinedAt,
                    latestActiveAt = admin.latestActiveAt,
                    changePasswordAt = admin.changePasswordAt,
                    createdAt = admin.createdAt,
                    createdBy = admin.createdBy,
                    updatedAt = admin.updatedAt,
                    updatedBy = admin.updatedBy,
                )
        }
    }
}
