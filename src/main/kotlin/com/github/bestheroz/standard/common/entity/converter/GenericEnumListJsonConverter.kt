package com.github.bestheroz.standard.common.entity.converter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.io.IOException

@Converter
open class GenericEnumListJsonConverter<T : Enum<T>>(
    private val enumClass: Class<T>,
) : AttributeConverter<List<T>?, String> {
    override fun convertToDatabaseColumn(attribute: List<T>?): String {
        try {
            return objectMapper.writeValueAsString(attribute)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Error converting list to JSON", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String): List<T>? {
        try {
            val type: JavaType =
                objectMapper.typeFactory.constructCollectionType(
                    MutableList::class.java,
                    enumClass,
                )
            return objectMapper.readValue(dbData, type)
        } catch (e: IOException) {
            throw RuntimeException("Error converting JSON to list", e)
        }
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
