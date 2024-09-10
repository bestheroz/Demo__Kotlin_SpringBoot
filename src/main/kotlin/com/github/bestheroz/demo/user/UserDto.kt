package com.github.bestheroz.demo.user

import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.dto.IdCreatedUpdatedDto
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import com.github.bestheroz.standard.common.enums.AuthorityEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

class UserDto {
    data class Request(
        @Schema(description = "페이지 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        val page: Int,
        @Schema(description = "페이지 크기", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        val pageSize: Int,
    )

    data class Response(
        @Schema(description = "로그인 아이디", requiredMode = Schema.RequiredMode.REQUIRED)
        val loginId: String,
        @Schema(description = "유저 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        val name: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
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
            fun of(user: User): Response =
                Response(
                    id = user.id!!,
                    loginId = user.loginId,
                    name = user.name,
                    useFlag = user.useFlag,
                    authorities = user.authorities,
                    joinedAt = user.joinedAt,
                    latestActiveAt = user.latestActiveAt,
                    changePasswordAt = user.changePasswordAt,
                    createdAt = user.createdAt,
                    createdBy = user.createdBy,
                    updatedAt = user.updatedAt,
                    updatedBy = user.updatedBy,
                )
        }
    }
}
