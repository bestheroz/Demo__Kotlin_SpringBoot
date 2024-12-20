package com.github.bestheroz.standard.common.entity.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.*

@Converter
open class GenericEnumConverter<T : Enum<T>>(
    private val enumClass: Class<T>,
) : AttributeConverter<T?, String?> {
    override fun convertToDatabaseColumn(attribute: T?): String? = attribute?.name?.lowercase(Locale.getDefault())

    override fun convertToEntityAttribute(dbData: String?): T? =
        dbData?.let {
            enumClass.enumConstants?.firstOrNull { e -> e.name.lowercase(Locale.getDefault()) == it }
                ?: throw IllegalArgumentException("Unknown enum value: $it")
        }
}
