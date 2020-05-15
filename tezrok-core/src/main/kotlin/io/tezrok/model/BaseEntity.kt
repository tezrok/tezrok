package io.tezrok.model

import com.fasterxml.jackson.databind.ObjectMapper

open class BaseEntity(val name: String,
                      val description: String?,
                      val kind: String) {
    operator fun get(name: String): Any? {
        // TODO: optimize
        val map = objectMapper.readValue(objectMapper.writeValueAsBytes(this), Map::class.java)

        return map[name]
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
