package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.entity.Notice
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.media.Schema

class NoticeCreateDto {
    data class Request(
        @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED)
        val title: String,
        @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED)
        val content: String,
        @Schema(description = "사용 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        val useFlag: Boolean,
    ) {
        fun toEntity(operator: Operator): Notice =
            Notice.of(
                title,
                content,
                useFlag,
                operator,
            )
    }
}
