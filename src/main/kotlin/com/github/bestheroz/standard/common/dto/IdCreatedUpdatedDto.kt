package com.github.bestheroz.standard.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

open class IdCreatedUpdatedDto(
    @Schema(description = "ID(KEY)", requiredMode = Schema.RequiredMode.REQUIRED)
    open val id: Long,
    updatedAt: Instant,
    updatedBy: UserSimpleDto,
    createdAt: Instant,
    createdBy: UserSimpleDto,
) : CreatedUpdatedDto(updatedAt, updatedBy, createdAt, createdBy)
