package com.github.bestheroz.standard.common.domain.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class JsonAttributeConverter(
    private val objectMapper: ObjectMapper,
) : AttributeConverter<Any?, String?> {
    override fun convertToDatabaseColumn(attribute: Any?): String? {
        if (attribute == null) {
            return null
        }
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            throw RuntimeException("JSON serialization error: ${e.message}", e)
        }
    }

    // DB에서 꺼낸 컬럼 값(dbData)이 null 또는 빈 문자열이면 null 반환
    override fun convertToEntityAttribute(dbData: String?): Any? {
        if (dbData.isNullOrEmpty()) {
            return null
        }

        return try {
            when {
                dbData.startsWith("[") -> objectMapper.readValue<List<Any>>(dbData)
                dbData.startsWith("{") -> objectMapper.readValue<Map<String, Any>>(dbData)
                else -> dbData
            }
        } catch (e: Exception) {
            throw RuntimeException("JSON deserialization error: ${e.message}", e)
        }
    }
}
