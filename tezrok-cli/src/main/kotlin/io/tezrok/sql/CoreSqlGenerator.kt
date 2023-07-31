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
     * TODO: own data model for sql tables
     * TODO: Postgres support
     * TODO: Sqlite support
     */
    override fun generate(schema: SchemaElem, context: GeneratorContext): SqlScript {
        val sb = StringBuilder()
        // TODO: generate enum tables
        val entityNames = schema.entities?.associate { it.name to it } ?: emptyMap()
        val entities = schema.entities?.sortedWith { a, b -> sortEntities(a, b, entityNames) } ?: emptyList()
        val targetEntities = mutableListOf<Pair<EntityElem, EntityElem>>()
        val foreignKeys = mutableListOf<ForeignKey>()

        entities.forEachIndexed { index, entity ->
            generateTable(entity, sb, targetEntities, foreignKeys, entityNames)
            if (index < entities.size - 1) {
                addNewline(sb)
            }
        }

        if (foreignKeys.isNotEmpty()) {
            addNewline(sb)
            sb.append("-- foreign keys")
            addNewline(sb)
            // generate foreign keys
            foreignKeys.forEach { foreignKey ->
                val fkName = "fk_${foreignKey.sourceTable}_${foreignKey.sourceColumn}".replace("\"", "")
                sb.append("ALTER TABLE ${foreignKey.sourceTable} ADD CONSTRAINT $fkName FOREIGN KEY(${foreignKey.sourceColumn}) REFERENCES ${foreignKey.targetTable}(${foreignKey.targetColumn});")
                addNewline(sb)
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
        targetEntities: MutableList<Pair<EntityElem, EntityElem>>,
        foreignKeys: MutableList<ForeignKey>,
        entityMap: Map<String, EntityElem>
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

        val primaryFieldCount = fields.count { it.primary == true }

        check(primaryFieldCount != 0) { "Primary field not found in entity: ${entity.name}" }
        check(primaryFieldCount == 1) { "Multiple primary fields not supported yet, in entity: ${entity.name}" }

        var colCount = 0

        fields.forEach { field ->
            if (field.refEntity == false) {
                if (colCount > 0) {
                    sb.append(",")
                    addNewline(sb)
                }
                generateColumn(field, sb)
                colCount++
            } else {
                val targetEntity = entityMap[field.type] ?: error("Entity ${field.type} not found")
                when (val relation = field.relation ?: error("Relation is not defined for field ${field.name}")) {
                    EntityRelation.OneToOne,
                    EntityRelation.ManyToMany -> {
                        if (colCount > 0) {
                            sb.append(",")
                            addNewline(sb)
                        }
                        val foreignKey = addRefColumn(field, targetEntity, sb, entity)
                        foreignKeys.add(foreignKey)
                        colCount++
                    }

                    EntityRelation.OneToMany -> targetEntities.add(targetEntity to entity)
                    else -> error("Unknown relation type: $relation")
                }
            }
        }

        // add synthetic columns for one-to-many relations
        targetEntities.filter { it.first == entity }
            .map { it.second }
            .forEach { targetEntity ->
                if (colCount > 0) {
                    sb.append(",")
                    addNewline(sb)
                }
                val foreignKey = addExtraRefColumn(targetEntity, sb, entity)
                foreignKeys.add(foreignKey)
                colCount++
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
            "public." + tableNameFinal.camelCaseToSnakeCase()
        else
            tableNameFinal.camelCaseToSnakeCase()
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
        sb: StringBuilder,
        entity: EntityElem
    ): ForeignKey {
        sb.append(intent)
        // TODO: Validate column name
        val fieldName = toColumnName(field.name) + "_id"
        sb.append(fieldName)
        sb.append(" ")
        val targetField = getPrimaryField(targetEntity)
        sb.append(getTargetRefType(targetField))
        // if field is serial, by default it's not null
        if (field.required == true && !field.isSerialEffective()) {
            sb.append(" NOT NULL")
        }

        return ForeignKey(
            fieldName,
            toTableName(entity.name, false),
            toColumnName(targetField.name),
            toTableName(targetEntity.name, false)
        )
    }

    /**
     * Adds synthetic column to target table because of one-to-many relation on target table
     */
    private fun addExtraRefColumn(
        targetEntity: EntityElem,
        sb: StringBuilder,
        entity: EntityElem
    ): ForeignKey {
        sb.append(intent)
        // TODO: Validate column name
        val fieldName = toColumnName(targetEntity.name) + "_id"
        sb.append(fieldName)
        sb.append(" ")
        val targetField = getPrimaryField(targetEntity)
        sb.append(getTargetRefType(targetField))
        sb.append(" NOT NULL")

        return ForeignKey(
            fieldName,
            toTableName(entity.name, false),
            toColumnName(targetField.name),
            toTableName(targetEntity.name, false)
        )
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
     * Returns the type of the reference column by the primary key of the target entity
     */
    private fun getTargetRefType(field: FieldElem): String {
        return when (field.type) {
            "integer" -> "INT"
            "long" -> "BIGINT"
            else -> error("Unsupported type: ${field.type}")
        }
    }

    private fun getPrimaryField(targetEntity: EntityElem): FieldElem =
        targetEntity.fields.find { it.primary == true }
            ?: error("Primary key is not defined for entity ${targetEntity.name}")

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

    private fun sortEntities(entity1: EntityElem, entity2: EntityElem, entityMap: Map<String, EntityElem>): Int {
        val refFields1 = entity1.fields.filter { it.refEntity == true && entityMap[it.type] == entity2 }
        val refFields2 = entity2.fields.filter { it.refEntity == true && entityMap[it.type] == entity1 }

        if (refFields1.isEmpty() && refFields2.isEmpty()) {
            return 0
        }

        if (refFields1.isEmpty()) {
            return 1
        }

        if (refFields2.isEmpty()) {
            return -1
        }

        return 0
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
        val targetTable: String
    )
}
