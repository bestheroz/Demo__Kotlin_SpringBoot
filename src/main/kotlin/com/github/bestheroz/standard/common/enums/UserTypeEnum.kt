package com.github.bestheroz.standard.common.enums

import com.github.bestheroz.standard.common.entity.converter.GenericEnumConverter
import jakarta.persistence.Converter

enum class UserTypeEnum(
    private val value: String,
) {
    ADMIN("admin"),
    USER("user"),
    ;

    @Converter(autoApply = true)
    class EnumConverter : GenericEnumConverter<UserTypeEnum>()
}
