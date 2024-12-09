package com.github.bestheroz.standard.common.entity.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class JsonAttributeConverter(
    private val objectMapper: ObjectMapper,
) : AttributeConverter<Any, String> {
    override fun convertToDatabaseColumn(attribute: Any): String =
        try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    override fun convertToEntityAttribute(dbData: String?): Any? {
        try {
            if (dbData.isNullOrEmpty()) {
                return null
            }
            return if (dbData.startsWith("[")) {
                objectMapper.readValue<List<Any>>(dbData)
            } else if (dbData.startsWith("{")) {
                objectMapper.readValue<Map<String, Any>>(dbData)
            } else {
                dbData
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
