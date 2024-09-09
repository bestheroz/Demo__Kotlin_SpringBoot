package com.github.bestheroz.standard.common.entity.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.lang.reflect.ParameterizedType
import java.util.*
import java.util.stream.Stream

@Converter
open class GenericEnumConverter<T : Enum<T>> : AttributeConverter<T?, String?> {
    private val enumClass =
        (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<T>

    override fun convertToDatabaseColumn(attribute: T?): String? {
        if (attribute == null) {
            return null
        }
        return attribute.name.lowercase(Locale.getDefault())
    }

    override fun convertToEntityAttribute(dbData: String?): T? {
        if (dbData == null) {
            return null
        }
        return Stream
            .of(*enumClass.enumConstants)
            .filter { e: T -> e.name.lowercase(Locale.getDefault()) == dbData }
            .findFirst()
            .orElseThrow {
                IllegalArgumentException(
                    "Unknown enum value: $dbData",
                )
            }
    }
}
