package com.github.bestheroz.standard.common.entity.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter

@jakarta.persistence.Converter
class JsonAttributeConverter(
    private val objectMapper: ObjectMapper,
) : AttributeConverter<Any, String> {
    override fun convertToDatabaseColumn(attribute: Any): String {
        try {
            return objectMapper.writeValueAsString(attribute)
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException(e)
        }
    }

    override fun convertToEntityAttribute(dbData: String): Any? {
        try {
            if (io.micrometer.common.util.StringUtils
                    .isEmpty(dbData)
            ) {
                return null
            }
            if (dbData.startsWith("[")) {
                return objectMapper.readValue<List<Any>>(
                    dbData,
                )
            } else if (dbData.startsWith("{")) {
                return objectMapper.readValue<Map<String, Any>>(
                    dbData,
                )
            }
            return dbData
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException(e)
        }
    }
}
