package io.tezrok.schema

import com.fasterxml.jackson.databind.ObjectMapper
import io.tezrok.api.schema.Schema
import io.tezrok.util.JsonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Loads a [Schema] from a file/string/Stream/URL
 */
class SchemaLoader {
    // TODO: Add support for loading from stream, URL

    /**
     * Load a [Schema] from a JSON string
     */
    fun load(json: String): Schema {
        try {
            return mapper.readValue(json, Schema::class.java)
        } catch (e: Exception) {
            log.error("Error loading schema from string: {}", json)
            throw e
        }
    }

    /**
     * Load a [Schema] from a JSON file
     */
    fun load(path: Path): Schema {
        try {
            log.debug("Loading schema from {}", path)
            return mapper.readValue(path.toUri().toURL(), Schema::class.java)
        } catch (e: Exception) {
            log.error("Error loading schema from path: {}", path)
            throw e
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SchemaLoader::class.java)
        val mapper: ObjectMapper = JsonUtil.mapper
    }
}
