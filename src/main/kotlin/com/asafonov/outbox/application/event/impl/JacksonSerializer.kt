package com.asafonov.outbox.application.event.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


class JacksonSerializer {

    companion object {
        private val mapper = jacksonObjectMapper()

        fun serialize(obj:Any): String {
            return mapper.writeValueAsString(obj)
        }

        fun <T> deserialize(value: String, valueType: Class<T> ): T {
            return mapper.readValue(value, valueType)
        }
    }
}
