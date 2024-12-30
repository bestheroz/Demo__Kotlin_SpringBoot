package com.github.bestheroz.standard.common.enums

import com.github.bestheroz.standard.common.domain.converter.GenericEnumConverter
import jakarta.persistence.Converter

enum class UserTypeEnum(
    private val value: String,
) {
    ADMIN("ADMIN"),
    USER("USER"),
    ;

    @Converter(autoApply = true)
    class EnumConverter : GenericEnumConverter<UserTypeEnum>(UserTypeEnum::class.java)
}
