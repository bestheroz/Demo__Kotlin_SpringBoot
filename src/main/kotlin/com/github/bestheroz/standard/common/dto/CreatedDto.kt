package com.github.bestheroz.standard.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

open class CreatedDto(
    @Schema(
        description = "생성일시",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    open val createdAt: Instant,
    @Schema(
        description = "생성자",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    open val createdBy: UserSimpleDto,
)
