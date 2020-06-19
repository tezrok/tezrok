package io.tezrok.spring.util

import io.tezrok.api.model.node.EntityNode
import io.tezrok.api.model.node.FieldNode

object NameUtil {
    fun getTableName(entity: EntityNode): String {
        return camelCaseToUnderscoreName(entity.name)
    }

    fun fieldName(fieldNode: FieldNode): String {
        return String.format("%s.%s", fieldNode.parent.name, fieldNode.name)
    }

    fun camelCaseToUnderscoreName(name: String): String {
        return name.replace(CAMEL_CASE_PATTERN, "$1_$2").toLowerCase()
    }

    private val CAMEL_CASE_PATTERN = Regex("([a-z])([A-Z])")
}
