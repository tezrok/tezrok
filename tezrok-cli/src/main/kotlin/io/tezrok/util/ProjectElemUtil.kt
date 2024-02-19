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
        ModelTypes.DATETIME_TZ -> "OffsetDateTime"
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
        ModelTypes.DATETIME_TZ -> true
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
    const val DATETIME_TZ = "DateTimeTZ"
    const val FLOAT = "Float"
    const val DOUBLE = "Double"
    const val DECIMAL = "Decimal"
}

fun EntityElem.getPrimaryFieldType(tryPrimitive: Boolean = false): String = getPrimaryField().asJavaType(tryPrimitive)

/**
 * Returns non-synthetic unique fields
 */
fun EntityElem.getUniqueFields(): List<FieldElem> = fields.filter { it.unique == true && it.isNotSynthetic() }

fun EntityElem.getUniqueGroups(logic: Boolean): Map<String, List<FieldElem>> =
    fields.filter { it.hasUniqueGroup() }
        .filter { if (logic) it.isNotSynthetic() else it.isNotLogic() }
        .groupBy { it.uniqueGroup!! }

fun EntityElem.getRepositoryName(): String = "${name}Repository"

fun EntityElem.getMapperName(): String = "${name}Mapper"

fun EntityElem.getDtoName(): String = "${name}Dto"

fun EntityElem.getFullDtoName(): String = "${name}FullDto"

fun EntityElem.asType(): ClassOrInterfaceType = ClassOrInterfaceType(getFullDtoName())

/**
 * Make method name to `findAllIdFieldsByPrimaryIdIn(Collection<ID> ids, Class<T> type)`
 */
fun EntityElem.getFindIdFieldsByPrimaryIdIn(): String {
    return "findIdFieldsBy${getPrimaryField().name.upperFirst()}In"
}

/**
 * Make method name to `getIdFieldsByPrimaryId(ID id, Class<T> type)`
 */
fun EntityElem.getGetIdFieldsByPrimaryId(): String {
    return "getIdFieldsBy${getPrimaryField().name.upperFirst()}"
}

/**
 * Make method name to `getIdFieldsByUniqueName(String name, Class<T> type)`
 */
fun EntityElem.getGetIdFieldsByUniqueField(field: FieldElem): String {
    check(field.unique == true) { "Field ${field.name} is not unique" }
    check(getUniqueFields().contains(field)) { "Field ${field.name} is not found in entity $name" }

    return "getIdFieldsBy${field.name.upperFirst()}"
}

/**
 * Make method name to `getPrimaryIdFieldByUniqueName(String name)`
 */
fun EntityElem.getGetPrimaryIdFieldByUniqueField(field: FieldElem): String {
    check(field.unique == true) { "Field ${field.name} is not unique" }
    check(getUniqueFields().contains(field)) { "Field ${field.name} is not found in entity $name" }

    return "get${getPrimaryField().name.upperFirst()}By${field.name.upperFirst()}"
}

fun EntityElem.getGetPrimaryIdFieldByGroupFields(fields: List<FieldElem>): String {
    val uniqueGroup = fields.first().uniqueGroup
        ?: error("Unique group not found for fields: ${fields.map { it.name }}")
    fields.forEach { field ->
        check(field.hasUniqueGroup()) { "Field ${field.name} is not in unique group" }
        check(field.uniqueGroup == uniqueGroup) { "Field ${field.name} is not in unique group $uniqueGroup" }
    }
    val fieldsPart = fields.joinToString(separator = "And")
    { field -> field.name.upperFirst() + if (field.isLogic()) "Id" else "" }
    return "get${getPrimaryField().name.upperFirst()}By$fieldsPart"
}

/**
 * Make method name to `getIdFieldsByUniqueName(String name, Class<T> type)`
 */
fun EntityElem.getGetIdFieldsByGroupFields(fields: List<FieldElem>): String {
    val uniqueGroup = fields.first().uniqueGroup
        ?: error("Unique group not found for fields: ${fields.map { it.name }}")
    fields.forEach { field ->
        check(field.hasUniqueGroup()) { "Field ${field.name} is not in unique group" }
        check(field.uniqueGroup == uniqueGroup) { "Field ${field.name} is not in unique group $uniqueGroup" }
    }
    val fieldsPart = fields.joinToString(separator = "And")
    { field -> field.name.upperFirst() + if (field.isLogic()) "Id" else "" }
    return "getIdFieldsBy$fieldsPart"
}
