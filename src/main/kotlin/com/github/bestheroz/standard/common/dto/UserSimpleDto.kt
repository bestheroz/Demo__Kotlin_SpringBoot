package com.github.bestheroz.standard.common.dto

import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

data class UserSimpleDto(
    @Schema(description = "ID(KEY)", requiredMode = Schema.RequiredMode.REQUIRED)
    val id: Long,
    @Schema(description = "관리자 or 유저", requiredMode = Schema.RequiredMode.REQUIRED)
    val type: UserTypeEnum,
    @Schema(description = "관리자 ID or 유저 계정 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    val loginId: String,
    @Schema(description = "관리자 이름 or 유저 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
) {
    companion object {
        @JvmStatic
        fun fromEntity(entity: Admin): UserSimpleDto =
            UserSimpleDto(
                entity.getId(),
                entity.type,
                entity.getLoginId(),
                entity.getName(),
            )

        @JvmStatic
        fun fromEntity(entity: User): UserSimpleDto =
            UserSimpleDto(
                entity.getId(),
                entity.type,
                entity.getLoginId(),
                entity.getName(),
            )
    }
}
