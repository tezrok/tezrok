package io.tezrok.util

import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem

/**
 * Utility class for SQL.
 */
object SqlUtil {
    /**
     * Checks and converts CSV data to SQL data for insert.
     */
    fun convertCsvDataAsSql(records: List<List<String>>, entity: EntityElem): SqlInitData {
        val fields = entity.fields.filter { isMandatoryField(it) }
        val result = mutableListOf<List<String>>()

        records.forEach { record ->
            if (record.size != fields.size) {
                throw IllegalArgumentException("Record size (${record.size}) doesn't match entity fields size (${fields.size}) in entity ${entity.name}")
            }

            val convertedRecord = mutableListOf<String>()
            for (i in 0 until record.size) {
                val field = fields[i]
                val value = record[i]
                convertedRecord.add(fieldValueToSql(field, value))
            }

            result.add(convertedRecord)
        }

        return SqlInitData("", fields, result)
    }

    /**
     * Converts field value to SQL value.
     */
    fun fieldValueToSql(field: FieldElem, value: String): String {
        return when (field.type) {
            ModelTypes.STRING -> "'$value'"
            ModelTypes.DATE -> if (value == "now()") "CURRENT_DATE" else value
            ModelTypes.DATETIME -> if (value == "now()") "CURRENT_TIMESTAMP" else value
            ModelTypes.INTEGER -> value.toIntOrNull()?.toString()
                ?: throw IllegalArgumentException(INVALID_VALUE + value)

            ModelTypes.LONG -> value.toLongOrNull()?.toString()
                ?: throw IllegalArgumentException(INVALID_VALUE + value)

            ModelTypes.FLOAT -> value.toFloatOrNull()?.toString()
                ?: throw IllegalArgumentException(INVALID_VALUE + value)

            ModelTypes.DOUBLE -> value.toDoubleOrNull()?.toString()
                ?: throw IllegalArgumentException(INVALID_VALUE + value)

            ModelTypes.DECIMAL -> value.toDoubleOrNull()?.toString()
                ?: throw IllegalArgumentException(INVALID_VALUE + value)

            ModelTypes.BOOLEAN -> value.toBooleanStrict().toString()
            else -> throw IllegalArgumentException("Unsupported type: ${field.type}")
        }
    }

    private fun isMandatoryField(field: FieldElem): Boolean {
        return field.logicField != true && field.primary != true && field.defValue == null
    }

    private const val INVALID_VALUE = "Invalid value: "
}

data class SqlInitData(
    val tableName: String,
    val fields: List<FieldElem>,
    val values: List<List<String>>
)
