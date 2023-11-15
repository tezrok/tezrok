package io.tezrok.util

import com.github.javaparser.ast.type.ClassOrInterfaceType
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem

/**
 * Returns true if field is eventually serial
 *
 * If serial not defined and only single field is primary, then it's serial
 */
fun FieldElem.isSerialEffective(singlePrimary: Boolean) = this.serial ?: (singlePrimary && (this.primary ?: false))

/**
 * Return type of field as Java type
 *
 * @param tryPrimitive if true, then return int instead of Integer, long instead of Long, etc.
 */
fun FieldElem.asJavaType(tryPrimitive: Boolean = false): String {
    return when (this.type) {
        ModelTypes.STRING -> "String"
        ModelTypes.INTEGER -> if (tryPrimitive) "int" else "Integer"
        ModelTypes.LONG -> if (tryPrimitive) "long" else "Long"
        ModelTypes.BOOLEAN -> if (tryPrimitive) "boolean" else "Boolean"
        ModelTypes.DATE -> "LocalDate"
        ModelTypes.DATETIME -> "LocalDateTime"
        ModelTypes.FLOAT -> if (tryPrimitive) "float" else "Float"
        ModelTypes.DOUBLE -> if (tryPrimitive) "double" else "Double"
        ModelTypes.DECIMAL -> "BigDecimal"
        else -> error("Unknown type: " + this.type)
    }
}

fun FieldElem.isBaseType(): Boolean {
    return when (this.type) {
        ModelTypes.STRING -> true
        ModelTypes.INTEGER -> true
        ModelTypes.LONG -> true
        ModelTypes.BOOLEAN -> true
        ModelTypes.DATE -> true
        ModelTypes.DATETIME -> true
        ModelTypes.FLOAT -> true
        ModelTypes.DOUBLE -> true
        ModelTypes.DECIMAL -> true
        else -> false
    }
}

fun FieldElem.getGetterName() = "get${name.upperFirst()}"

fun FieldElem.getSetterName() = "set${name.upperFirst()}"

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

fun EntityElem.getRepositoryName(): String = "${name}Repository"

fun EntityElem.getMapperName(): String = "${name}Mapper"

fun EntityElem.getDtoName(): String = "${name}Dto"

fun EntityElem.getFullDtoName(): String = "${name}FullDto"

fun EntityElem.asType(): ClassOrInterfaceType = ClassOrInterfaceType(getFullDtoName())

/**
 * Make method name to `findAllIdFieldsByPrimaryIdIn(Collection<ID> ids, Class<T> type)`
 */
fun EntityElem.getFindAllIdFieldsByPrimaryIdIn(): String {
    val allIds = getIdFields().joinToString("") { it.name.upperFirst() }

    return "find${allIds}By${getPrimaryField().name.upperFirst()}In"
}
