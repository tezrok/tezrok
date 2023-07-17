package io.tezrok.sql

import io.tezrok.api.GeneratorContext
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.SchemaElem
import io.tezrok.api.model.SqlScript
import io.tezrok.api.sql.SqlGenerator
import io.tezrok.util.camelCaseToSnakeCase
import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory

class CoreSqlGenerator(private val intent: String = "  ") : SqlGenerator {
    /**
     * Generates SQL from a JSON schema
     *
     * TODO: Postgres support
     * TODO: Sqlite support
     */
    override fun generate(schema: SchemaElem, context: GeneratorContext): SqlScript {
        val sb = StringBuilder()
        val entities = schema.entities ?: emptyList()

        entities.forEachIndexed { index, entity ->
            generateTable(entity, sb)
            if (index < entities.size - 1) {
                addNewline(sb)
            }
        }

        return SqlScript("schema", sb.toString())
    }

    /**
     * Generates a table from root schema definition
     */
    private fun generateTable(entity: EntityElem, sb: StringBuilder) {
        Validate.notBlank(entity.name, "Schema table is blank")
        val tableName = toTableName(entity.name)
        val fields = entity.fields
        // TODO: Validate table name
        // TODO: Validate properties

        sb.append("CREATE TABLE ")
        sb.append(tableName)
        sb.append(" (")
        addNewline(sb)

        if (fields.find { it.name == "id" } == null) {
            // TODO: not add automatic id if it's not a root schema
            sb.append(intent)
            sb.append("id SERIAL PRIMARY KEY")
            if (fields.isNotEmpty()) {
                sb.append(",")
            }
            addNewline(sb)
        }

        var colCount = 0

        fields.forEach { field ->
            // TODO: implement "array" in another place depending on the relation type
            if (field.ref == null) {
                if (colCount > 0) {
                    sb.append(",")
                    addNewline(sb)
                }
                generateColumn(field, sb)
                colCount++
            } else {
                log.error("Not implemented")
            }
        }
        addNewline(sb)
        sb.append(");")
        addNewline(sb)
    }

    private fun toTableName(tableName: String): String {
        val tableNameFinal = if (postgresKeywords.contains(tableName.lowercase())) {
            "\"$tableName\""
        } else {
            tableName
        }

        // TODO: get schema from context
        return "public." + tableNameFinal.camelCaseToSnakeCase()
    }

    private fun generateColumn(field: FieldElem, sb: StringBuilder) {
        sb.append(intent)
        // TODO: Validate column name
        // TODO: Convert to underscore case
        sb.append(field.name)
        sb.append(" ")
        sb.append(getSqlType(field))
        // if field is serial, by default it's not null
        if (field.required == true && !field.isSerialEffective()) {
            sb.append(" NOT NULL")
        }
        if (field.primary == true) {
            sb.append(" PRIMARY KEY")
        }
    }

    private fun getSqlType(field: FieldElem): String {
        return when (field.type) {
            "string" -> getStringBasedType(field)
            "date" -> "DATE"
            "dateTime" -> "TIMESTAMP"
            "integer" -> if (field.isSerialEffective()) "SERIAL" else "INT"
            "long" -> if (field.isSerialEffective()) "BIGSERIAL" else "BIGINT"
            "number" -> "FLOAT"
            "boolean" -> "BOOLEAN"
            // TODO: Create new ref column on target table, or new table with ref columns to both tables
            "array" -> throw IllegalArgumentException("Array type is implemented in another way")
            else -> throw IllegalArgumentException("Unsupported type: ${field.type}")
        }
    }

    private fun getStringBasedType(definition: FieldElem) =
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
        val postgresKeywords = setOf("order")
        val log = LoggerFactory.getLogger(CoreSqlGenerator::class.java)!!
    }
}
