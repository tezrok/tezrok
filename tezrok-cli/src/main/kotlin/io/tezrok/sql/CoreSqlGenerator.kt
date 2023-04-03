package io.tezrok.sql

import io.tezrok.api.GeneratorContext
import io.tezrok.api.model.SqlScript
import io.tezrok.api.schema.Definition
import io.tezrok.api.schema.Schema
import io.tezrok.api.sql.SqlGenerator
import org.apache.commons.lang3.Validate

class CoreSqlGenerator(private val intent: String = "  ") : SqlGenerator {
    /**
     * Generates SQL from a JSON schema
     *
     * TODO: Postgres support
     * TODO: Sqlite support
     */
    override fun generate(schema: Schema, context: GeneratorContext): SqlScript {
        val sb = StringBuilder()
        val definitions = schema.definitions ?: emptyMap()

        if (schema.title != null) {
            generateTable(schema.title, schema, sb)
            if (definitions.isNotEmpty()) {
                addNewline(sb)
            }
        }

        definitions.keys.toList().forEachIndexed { index, name ->
            val definition = definitions[name]!!
            generateTable(name, definition, sb)
            if (index < definitions.size - 1) {
                addNewline(sb)
            }
        }

        return SqlScript(schema.title ?: "schema", sb.toString())
    }

    /**
     * Generates a table from root schema definition
     */
    private fun generateTable(tableName: String, schema: Definition, sb: StringBuilder) {
        Validate.notBlank(tableName, "Schema table is blank")
        val properties = schema.properties ?: emptyMap()
        // TODO: Validate table name
        // TODO: Validate properties

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
            val definition = properties[key]!!
            // TODO: implement "array" in another place depending on the relation type
            if (!definition.isArray()) {
                generateColumn(key, definition, isRequired, sb)
                if (index < properties.size - 1) {
                    sb.append(",")
                }
                addNewline(sb)
            }
        }
        sb.append(");")
        addNewline(sb)
    }

    private fun generateColumn(name: String, definition: Definition, isRequired: Boolean, sb: StringBuilder) {
        sb.append(intent)
        // TODO: Validate column name
        // TODO: Convert to underscore case
        sb.append(name)
        sb.append(" ")
        sb.append(getSqlType(definition))
        if (isRequired) {
            sb.append(" NOT NULL")
        }
    }

    private fun getSqlType(definition: Definition): String {
        return when (definition.type) {
            "string" -> getStringBasedType(definition)
            "integer" -> "INT"
            "number" -> "FLOAT"
            "boolean" -> "BOOLEAN"
            "long" -> "BIGINT"
            // TODO: Create new ref column on target table, or new table with ref columns to both tables
            "array" -> throw IllegalArgumentException("Array type is implemented in another way")
            else -> throw IllegalArgumentException("Unsupported type: ${definition.type}")
        }
    }

    private fun getStringBasedType(definition: Definition) =
        if (definition.format == "date") {
            "DATE"
        } else if (definition.format == "date-time") {
            "DATETIME"
        } else if (definition.maxLength != null) {
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
