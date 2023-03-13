package io.tezrok.cli

import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SchemaLoader {
    fun load(path: String) {
        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        val jsonSchema: JsonSchema = factory.getSchema(javaClass.getResourceAsStream(path))
        val node = jsonSchema.schemaNode as ObjectNode
        val properties = node.get("properties") as ObjectNode

        for (key in properties.fieldNames()) {
            log.info("key: {}", key)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SchemaLoader::class.java)
    }
}
