package io.tezrok.sql

import io.tezrok.api.GeneratorContext
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.SchemaElem
import io.tezrok.api.model.SqlScript
import io.tezrok.api.sql.SqlGenerator
import io.tezrok.util.*
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
        val comments = mutableListOf<CommentOn>()
        val inits = mutableListOf<SqlInitData>()
        val entities = schema.entities ?: emptyList()
        val schemaName = schema.schemaName

        if (schemaName != DEFAULT_SCHEMA_NAME) {
            sb.append("CREATE SCHEMA IF NOT EXISTS $schemaName;")
            addNewline(sb, 2)
        }

        entities.forEachIndexed { index, entity ->
            generateTable(entity, sb, schemaName, comments, inits)
            if (index < entities.size - 1) {
                addNewline(sb)
            }
        }

        val foreignKeys = calculateForeignKeys(entities, schemaName)
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

        if (comments.isNotEmpty()) {
            addNewline(sb)
            sb.append("-- comments")
            addNewline(sb)
            comments.forEach { comment ->
                // TODO: escape single quotes in comment
                sb.append("COMMENT ON ${comment.type} ${comment.name} IS '${comment.comment.trim()}';")
                addNewline(sb)
            }
        }

        if (inits.isNotEmpty()) {
            addNewline(sb)
            sb.append("-- data")
            addNewline(sb)

            inits.forEach { data ->
                val fields = data.fields
                val columns = fields.joinToString(", ") { toColumnName(it.name) }
                data.values.forEach { record ->
                    val values = record.joinToString(", ")
                    sb.append("INSERT INTO ${data.tableName} ($columns) VALUES ($values);")
                    addNewline(sb)
                }
            }
        }

        return SqlScript("schema", sb.toString())
    }

    /**
     * Generates a table from root schema definition
     */
    private fun generateTable(
        entity: EntityElem,
        sb: StringBuilder,
        schemaName: String,
        comments: MutableList<CommentOn>,
        inits: MutableList<SqlInitData>
    ) {
        Validate.notBlank(entity.name, "Schema table is blank")
        val tableName = toTableName(entity.name, schemaName)
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

        if (entity.description?.isNotBlank() == true) {
            comments.add(
                CommentOn(
                    type = SqlObjectType.TABLE,
                    name = tableName,
                    comment = entity.description
                )
            )
        }

        fields.filter { it.logicField != true }.forEach { field ->
            if (colCount > 0) {
                sb.append(",")
                addNewline(sb)
            }
            generateColumn(field, sb, primaryFields.size == 1, tableName, comments)
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

        if (entity.init?.isNotBlank() == true) {
            val records = CsvUtil.fromString(entity.init)
            val sqlInitData = SqlUtil.convertCsvDataAsSql(records, entity).copy(tableName = tableName)
            inits.add(sqlInitData)
        }

        fields.filter { it.isNotLogic() && it.hasUniqueGroup() }
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

    private fun toTableName(tableName: String, schemaName: String = ""): String {
        val tableNameFinal = if (postgresKeywords.contains(tableName.lowercase())) {
            "\"$tableName\""
        } else {
            tableName
        }

        return if (schemaName.isNotBlank())
            "$schemaName." + tableNameFinal.camelCaseToSqlCase()
        else
            tableNameFinal.camelCaseToSqlCase()
    }

    private fun generateColumn(
        field: FieldElem,
        sb: StringBuilder,
        singlePrimary: Boolean,
        tableName: String,
        comments: MutableList<CommentOn>
    ) {
        sb.append(intent)
        // TODO: Validate column name
        val columnName = toColumnName(field.name)
        sb.append(columnName)
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
        if (field.defValue?.isNotBlank() == true) {
            sb.append(" DEFAULT ")
            sb.append(getSqlDefault(field))
        }

        if (field.description?.isNotBlank() == true) {
            comments.add(
                CommentOn(
                    type = SqlObjectType.COLUMN,
                    name = "${tableName}.$columnName",
                    comment = field.description
                )
            )
        }
    }

    private fun getSqlType(field: FieldElem, singlePrimary: Boolean): String {
        return when (field.type) {
            ModelTypes.STRING -> getStringBasedType(field)
            ModelTypes.DATE -> "DATE"
            ModelTypes.DATETIME -> "TIMESTAMP"
            ModelTypes.DATETIME_TZ -> "TIMESTAMP WITH TIME ZONE"
            ModelTypes.INTEGER -> if (field.isSerialEffective(singlePrimary)) "SERIAL" else "INT"
            ModelTypes.LONG -> if (field.isSerialEffective(singlePrimary)) "BIGSERIAL" else "BIGINT"
            ModelTypes.DOUBLE -> "FLOAT8"
            ModelTypes.FLOAT -> "FLOAT4"
            ModelTypes.DECIMAL -> "NUMERIC"
            ModelTypes.BOOLEAN -> "BOOLEAN"
            // TODO: Create new ref column on target table, or new table with ref columns to both tables
            "array" -> throw IllegalArgumentException("Array type is implemented in another way")
            else -> throw IllegalArgumentException("Unsupported type: ${field.type}")
        }
    }

    private fun getSqlDefault(field: FieldElem): String {
        val defValue = field.defValue ?: throw IllegalArgumentException("Default value is not set")
        return SqlUtil.fieldValueToSql(field, defValue)
    }

    private fun getStringBasedType(definition: FieldElem) =
        if (definition.maxLength != null) {
            "VARCHAR(${definition.maxLength})"
        } else {
            "VARCHAR($DEFAULT_VARCHAR_LENGTH)"
        }

    private fun calculateForeignKeys(entities: List<EntityElem>, schemaName: String): List<ForeignKey> {
        val foreignKeys = mutableListOf<ForeignKey>()
        val entityNames = entities.associateBy { it.name }

        entities.forEach { entity ->
            entity.fields.filter { it.foreignField?.isNotBlank() == true }
                .forEach { field ->
                    val (entityName, fieldName) = field.foreignField!!.split(".")
                    val targetEntity = entityNames[entityName] ?: error("Entity not found: $entityName")
                    val targetField = targetEntity.fields.find { it.name == fieldName }
                        ?: error("Field not found: ${field.foreignField})")
                    val sourceTableName = toTableName(entity.name)
                    val targetTableName = toTableName(targetEntity.name)

                    val foreignKey = ForeignKey(
                        schema = schemaName,
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
        const val DEFAULT_SCHEMA_NAME = "public"
        val postgresKeywords = setOf("order")
        val log = LoggerFactory.getLogger(CoreSqlGenerator::class.java)!!
    }

    data class ForeignKey(
        val sourceColumn: String,
        val sourceTable: String,
        val targetColumn: String,
        val targetTable: String,
        val schema: String
    ) {
        init {
            check(schema.isNotBlank()) { "Schema is blank" }
        }
    }

    data class CommentOn(
        // table, column, index, sequence, etc.
        val type: SqlObjectType,
        val name: String,
        val comment: String
    )

    enum class SqlObjectType {
        TABLE,
        COLUMN
    }
}
