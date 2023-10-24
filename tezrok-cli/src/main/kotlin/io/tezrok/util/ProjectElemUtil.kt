package io.tezrok.util

import io.tezrok.api.input.FieldElem

/**
 * Returns true if field is eventually serial
 *
 * If serial not defined and only single field is primary, then it's serial
 */
fun FieldElem.isSerialEffective(singlePrimary: Boolean) = this.serial ?: (singlePrimary && (this.primary ?: false))

fun FieldElem.asJavaType(): String {
    return when (this.type) {
        "String" -> "String"
        "Integer" -> "Integer"
        "Long" -> "Long"
        "Boolean" -> "Boolean"
        "Date" -> "LocalDate"
        "DateTime" -> "LocalDateTime"
        "Float" -> "Float"
        "Double" -> "Double"
        "Decimal" -> "BigDecimal"
        else -> error("Unknown type: ${this.type}")
    }
}
