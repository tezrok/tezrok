package io.tezrok.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Utility class for JSON
 */
object JsonUtil {
    /**
     * The default [ObjectMapper]
     */
    internal val mapper = createMapper()

    /**
     * Creates a new [ObjectMapper]
     */
    fun createMapper(): ObjectMapper =
        ObjectMapper().registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        ).apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    /**
     * Compares two JSON strings and returns true if they semantically match (ignoring whitespace and order of keys)
     */
    fun compareJsons(expected: String, actual: String): Boolean {
        val expectedJson = createMapper().readTree(expected)
        val actualJson = createMapper().readTree(actual)
        return expectedJson == actualJson
    }
}
