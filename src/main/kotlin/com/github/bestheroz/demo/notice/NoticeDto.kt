package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.entity.Notice
import com.github.bestheroz.standard.common.dto.IdCreatedUpdatedDto
import com.github.bestheroz.standard.common.dto.UserSimpleDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

class NoticeDto {
    data class Request(
        @Schema(description = "페이지 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        val page: Int,
        @Schema(description = "페이지 크기", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        val pageSize: Int,
    )

    data class Response(
        @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED)
        val title: String,
        @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED)
        val content: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
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
            fun of(notice: Notice): Response =
                Response(
                    id = notice.id!!,
                    title = notice.title,
                    content = notice.content,
                    useFlag = notice.useFlag,
                    createdAt = notice.createdAt,
                    createdBy = notice.createdBy,
                    updatedAt = notice.updatedAt,
                    updatedBy = notice.updatedBy,
                )
        }
    }
}
