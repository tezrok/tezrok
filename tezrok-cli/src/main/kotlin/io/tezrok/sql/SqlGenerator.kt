package io.tezrok.sql

import io.tezrok.schema.Definition
import io.tezrok.schema.Schema
import org.apache.commons.lang3.Validate

class SqlGenerator(private val intent: String = "  ") {
    /**
     * Generates SQL from a JSON schema
     *
     * TODO: Postgres support
     * TODO: Sqlite support
     */
    fun generateAsString(schema: Schema): String {
        val sb = StringBuilder()
        generateTable(schema, sb)
        return sb.toString()
    }

    private fun generateTable(schema: Schema, sb: StringBuilder) {
        val tableName = Validate.notBlank(schema.title, "Schema title is blank")
        val properties = schema.properties ?: emptyMap()
        // TODO: Validate table name
        // TODO: Validate properties
        // TODO: generates for schema.definitions

        sb.append("CREATE TABLE ")
        sb.append(tableName)
        sb.append(" (")
        addNewline(sb)

        if (!properties.containsKey("id")) {
            sb.append(intent)
            sb.append("id SERIAL PRIMARY KEY")
            if (properties.isNotEmpty()) {
                sb.append(",")
            }
            addNewline(sb)
        }

        properties.keys.toList().forEachIndexed { index, key ->
            val isRequired = schema.required?.contains(key) ?: false
            generateColumn(key, properties[key]!!, isRequired, sb)
            if (index < properties.size - 1) {
                sb.append(",")
            }
            addNewline(sb)
        }
        sb.append(");")
        addNewline(sb)
    }

    private fun generateColumn(name: String, definition: Definition, isRequired: Boolean, sb: StringBuilder) {
        sb.append(intent)
        sb.append(name)
        sb.append(" ")
        sb.append(getSqlType(definition))
        if (isRequired) {
            sb.append(" NOT NULL")
        }
    }

    private fun getSqlType(definition: Definition): String {
        return when (definition.type) {
            "string" -> getSqlVarcharType(definition)
            "integer" -> "INT"
            "number" -> "FLOAT"
            "boolean" -> "BOOLEAN"
            else -> throw IllegalArgumentException("Unsupported type: ${definition.type}")
        }
    }

    private fun getSqlVarcharType(definition: Definition) =
        if (definition.maxLength != null) {
            "VARCHAR(${definition.maxLength})"
        } else {
            "VARCHAR($DEFAULT_VARCHAR_LENGTH)"
        }

    private fun addNewline(sb: StringBuilder, count: Int = 1) {
        for (i in 1..count) {
            sb.append(System.lineSeparator())
        }
    }

    private companion object {
        const val DEFAULT_VARCHAR_LENGTH = 255
    }
}
