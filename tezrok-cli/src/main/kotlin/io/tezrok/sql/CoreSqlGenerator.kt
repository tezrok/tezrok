package io.tezrok.sql

import io.tezrok.api.GeneratorContext
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.SchemaElem
import io.tezrok.api.model.SqlScript
import io.tezrok.api.sql.SqlGenerator
import io.tezrok.util.camelCaseToSqlCase
import io.tezrok.util.isSerialEffective
import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory

/**
 * Generates SQL from [SchemaElem]
 */
class CoreSqlGenerator(private val intent: String = "  ") : SqlGenerator {
    /**
     * Generates SQL from [SchemaElem]
     * TODO: Postgres support
     * TODO: Sqlite support
     */
    override fun generate(schema: SchemaElem, context: GeneratorContext): SqlScript {
        val sb = StringBuilder()
        // TODO: generate enum tables
        val entities = schema.entities ?: emptyList()
        entities.forEachIndexed { index, entity ->
            generateTable(entity, sb)
            if (index < entities.size - 1) {
                addNewline(sb)
            }
        }

        val foreignKeys = calculateForeignKeys(entities)
        if (foreignKeys.isNotEmpty()) {
            addNewline(sb)
            sb.append("-- foreign keys")
            addNewline(sb)
            // generate foreign keys
            foreignKeys.forEach { foreignKey ->
                val fkName = "fk_${foreignKey.sourceTable}_${foreignKey.sourceColumn}".replace("\"", "")
                sb.append("ALTER TABLE ${foreignKey.schema}.${foreignKey.sourceTable} ADD CONSTRAINT $fkName FOREIGN KEY(${foreignKey.sourceColumn}) REFERENCES ${foreignKey.schema}.${foreignKey.targetTable}(${foreignKey.targetColumn});")
                addNewline(sb)
            }
        }

        // TODO: Add comment on columns

        return SqlScript("schema", sb.toString())
    }

    /**
     * Generates a table from root schema definition
     */
    private fun generateTable(
        entity: EntityElem,
        sb: StringBuilder
    ) {
        Validate.notBlank(entity.name, "Schema table is blank")
        val tableName = toTableName(entity.name)
        val fields = entity.fields
        // TODO: Validate table name
        // TODO: Validate properties

        sb.append("CREATE TABLE ")
        sb.append(tableName)
        sb.append(" (")
        addNewline(sb)

        val primaryFields = fields.filter { it.primary == true }

        check(primaryFields.isNotEmpty()) { "Primary field not found in entity: ${entity.name}" }

        var colCount = 0

        fields.filter { it.logicField != true }.forEach { field ->
            if (colCount > 0) {
                sb.append(",")
                addNewline(sb)
            }
            generateColumn(field, sb, primaryFields.size == 1)
            colCount++
        }

        if (primaryFields.size > 1) {
            sb.append(",")
            addNewline(sb)
            sb.append(intent)
            sb.append("PRIMARY KEY (")
            primaryFields.forEachIndexed { index, field ->
                if (index > 0) {
                    sb.append(", ")
                }
                sb.append(toColumnName(field.name))
            }
            sb.append(")")
        }

        fields.filter { it.uniqueGroup != null }
            .groupBy { it.uniqueGroup }
            .forEach { (_, fields) ->
                sb.append(",")
                addNewline(sb)
                sb.append(intent)
                sb.append("UNIQUE (")
                fields.forEachIndexed { index, field ->
                    if (index > 0) {
                        sb.append(", ")
                    }
                    sb.append(toColumnName(field.name))
                }
                sb.append(")")
            }

        addNewline(sb)
        sb.append(");")
        addNewline(sb)
    }

    private fun toTableName(tableName: String, withSchema: Boolean = true): String {
        val tableNameFinal = if (postgresKeywords.contains(tableName.lowercase())) {
            "\"$tableName\""
        } else {
            tableName
        }

        // TODO: get schema from context
        return if (withSchema)
            "public." + tableNameFinal.camelCaseToSqlCase()
        else
            tableNameFinal.camelCaseToSqlCase()
    }

    private fun generateColumn(field: FieldElem, sb: StringBuilder, singlePrimary: Boolean) {
        sb.append(intent)
        // TODO: Validate column name
        sb.append(toColumnName(field.name))
        sb.append(" ")
        sb.append(getSqlType(field, singlePrimary))
        // if field is serial, by default it's not null
        if (field.required == true && !field.isSerialEffective(singlePrimary)) {
            sb.append(" NOT NULL")
        }
        if (field.primary == true && singlePrimary) {
            // for composite primary key, primary key is added at the end of the table
            sb.append(" PRIMARY KEY")
        } else if (field.unique == true) {
            sb.append(" UNIQUE")
        }
    }

    private fun getSqlType(field: FieldElem, singlePrimary: Boolean): String {
        return when (field.type) {
            "string" -> getStringBasedType(field)
            "date" -> "DATE"
            "dateTime" -> "TIMESTAMP"
            "integer" -> if (field.isSerialEffective(singlePrimary)) "SERIAL" else "INT"
            "long" -> if (field.isSerialEffective(singlePrimary)) "BIGSERIAL" else "BIGINT"
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

    private fun calculateForeignKeys(entities: List<EntityElem>): List<ForeignKey> {
        val foreignKeys = mutableListOf<ForeignKey>()
        val entityNames = entities.associateBy { it.name }

        entities.forEach { entity ->
            entity.fields.filter { it.foreignField?.isNotBlank() == true }
                .forEach { field ->
                    val (entityName, fieldName) = field.foreignField!!.split(".")
                    val targetEntity = entityNames[entityName] ?: error("Entity not found: $entityName")
                    val targetField = targetEntity.fields.find { it.name == fieldName }
                        ?: error("Field not found: ${field.foreignField})")
                    val sourceTableName = toTableName(entity.name, false)
                    val targetTableName = toTableName(targetEntity.name, false)

                    val foreignKey = ForeignKey(
                        schema = "public",
                        sourceTable = sourceTableName,
                        sourceColumn = toColumnName(field.name),
                        targetTable = targetTableName,
                        targetColumn = toColumnName(targetField.name)
                    )

                    foreignKeys.add(foreignKey)
                }
        }

        return foreignKeys
    }

    /**
     * Converts a field name to a column name
     *
     * Example: "firstName" -> "first_name"
     */
    private fun toColumnName(name: String) = name.camelCaseToSqlCase()

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

    data class ForeignKey(
        val sourceColumn: String,
        val sourceTable: String,
        val targetColumn: String,
        val targetTable: String,
        val schema: String = "public"
    )
}
