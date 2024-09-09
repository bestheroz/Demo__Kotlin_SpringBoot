package com.github.bestheroz.standard.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

open class IdCreatedDto(
    @Schema(description = "ID(KEY)", requiredMode = Schema.RequiredMode.REQUIRED)
    open val id: Long,
    createdAt: Instant,
    createdBy: UserSimpleDto,
) : CreatedDto(createdAt, createdBy)
