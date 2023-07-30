package io.tezrok.sql

import io.tezrok.api.GeneratorContext
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.EntityRelation
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
        // TODO: sort entities by dependencies
        val entities = schema.entities ?: emptyList()
        val targetEntities = mutableListOf<Pair<EntityElem, EntityElem>>()

        entities.forEachIndexed { index, entity ->
            generateTable(entity, sb, targetEntities)
            if (index < entities.size - 1) {
                addNewline(sb)
            }
        }

        return SqlScript("schema", sb.toString())
    }

    /**
     * Generates a table from root schema definition
     */
    private fun generateTable(entity: EntityElem, sb: StringBuilder, targetEntities:  MutableList<Pair<EntityElem, EntityElem>>) {
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
            sb.append("id BIGSERIAL PRIMARY KEY")
            if (fields.isNotEmpty()) {
                sb.append(",")
            }
            addNewline(sb)
        }

        var colCount = 0

        fields.forEach { field ->
            if (field.ref == null) {
                if (colCount > 0) {
                    sb.append(",")
                    addNewline(sb)
                }
                generateColumn(field, sb)
                colCount++
            } else {
                val ref = field.ref as EntityElem
                when (val relation = field.relation ?: error("Relation is not defined for field ${field.name}")) {
                    EntityRelation.OneToOne,
                    EntityRelation.ManyToMany -> {
                        if (colCount > 0) {
                            sb.append(",")
                            addNewline(sb)
                        }
                        addRefColumn(field, ref, sb)
                    }
                    EntityRelation.OneToMany -> targetEntities.add(ref to entity)
                    else -> error("Unknown relation type: $relation")
                }
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
        sb.append(toColumnName(field.name))
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

    private fun addRefColumn(
        field: FieldElem,
        targetEntity: EntityElem,
        sb: StringBuilder
    ) {
        sb.append(intent)
        // TODO: Validate column name
        sb.append(toColumnName(field.name) + "_id")
        sb.append(" ")
        sb.append(getTargetRefType(targetEntity))
        // if field is serial, by default it's not null
        if (field.required == true && !field.isSerialEffective()) {
            sb.append(" NOT NULL")
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

    /**
     * Returns the type of the reference column
     */
    private fun getTargetRefType(targetEntity: EntityElem): String {
        return targetEntity.fields.find { it.primary == true }?.let { field ->
            if (field.type == "integer") {
                "INT"
            } else {
                "BIGINT"
            }
        } ?: "BIGINT"
    }

    /**
     * Converts a field name to a column name
     *
     * Example: "firstName" -> "first_name"
     */
    private fun toColumnName(name: String) = name.camelCaseToSnakeCase().lowercase()

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
