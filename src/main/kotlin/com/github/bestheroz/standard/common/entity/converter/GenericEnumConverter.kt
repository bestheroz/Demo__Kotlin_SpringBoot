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

    override fun convertToDatabaseColumn(attribute: T?): String? =
        attribute?.let {
            return@convertToDatabaseColumn it.name.lowercase(Locale.getDefault())
        }

    override fun convertToEntityAttribute(dbData: String?): T? =
        dbData?.let {
            return@convertToEntityAttribute Stream
                .of(*enumClass.enumConstants)
                .filter { e: T -> e.name.lowercase(Locale.getDefault()) == it }
                .findFirst()
                .orElseThrow {
                    IllegalArgumentException(
                        "Unknown enum value: $it",
                    )
                }
        }
}
