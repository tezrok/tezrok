package io.tezrok.api

import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.service.Service

interface GlobalContext {
    /**
     * Resolves type by name
     */
    fun resolveType(name: String): Type

    /**
     * Resolves field type
     */
    fun resolveType(field: FieldNode): Type

    /**
     * Gets instance of specified class
     */
    fun <T> getInstance(clazz: Class<T>): T

    /**
     * Gets list of an Type. Used for getting visitors list
     */
    fun <T : Service> getServiceList(clazz: Class<T>): Set<T>
}
