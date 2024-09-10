package com.github.bestheroz.standard.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

open class CreatedUpdatedDto(
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
    open val updatedAt: Instant,
    @Schema(description = "수정자", requiredMode = Schema.RequiredMode.REQUIRED)
    open val updatedBy: UserSimpleDto,
) : CreatedDto(createdAt, createdBy)
