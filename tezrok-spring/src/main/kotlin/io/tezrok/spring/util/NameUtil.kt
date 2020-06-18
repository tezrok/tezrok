package io.tezrok.spring.util

import io.tezrok.api.model.node.EntityNode

object NameUtil {
    fun getTableName(entity: EntityNode): String {
        return camelCaseToUnderscoreName(entity.name)
    }

    private fun camelCaseToUnderscoreName(name: String): String {
        return name.replace(CAMEL_CASE_PATTERN, "$1_$2").toLowerCase()
    }

    private val CAMEL_CASE_PATTERN = Regex("([a-z])([A-Z])")
}
