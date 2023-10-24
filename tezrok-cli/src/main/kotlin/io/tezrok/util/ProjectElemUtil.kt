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
        ModelTypes.STRING -> "String"
        ModelTypes.INTEGER -> "Integer"
        ModelTypes.LONG -> "Long"
        ModelTypes.BOOLEAN -> "Boolean"
        ModelTypes.DATE -> "LocalDate"
        ModelTypes.DATETIME -> "LocalDateTime"
        ModelTypes.FLOAT -> "Float"
        ModelTypes.DOUBLE -> "Double"
        ModelTypes.DECIMAL -> "BigDecimal"
        else -> error("Unknown type: ${this.type}")
    }
}

object ModelTypes {
    const val STRING = "String"
    const val INTEGER = "Integer"
    const val LONG = "Long"
    const val BOOLEAN = "Boolean"
    const val DATE = "Date"
    const val DATETIME = "DateTime"
    const val FLOAT = "Float"
    const val DOUBLE = "Double"
    const val DECIMAL = "Decimal"
}
