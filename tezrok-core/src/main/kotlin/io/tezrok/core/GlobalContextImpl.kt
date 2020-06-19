package io.tezrok.core

import io.tezrok.api.GlobalContext
import io.tezrok.api.builder.type.Type
import io.tezrok.api.error.TezrokException
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.service.Service
import io.tezrok.core.factory.Factory

internal class GlobalContextImpl(private val factory: Factory) : GlobalContext {
    override fun <T : Service> getServiceList(clazz: Class<T>): Set<T> = factory.getServiceList(clazz)

    override fun resolveType(name: String): Type = factory.resolveType(name,
            factory.getProject().modules().firstOrNull() ?: throw TezrokException("Any module not found"))

    override fun resolveType(field: FieldNode): Type = factory.resolveType(field)

    override fun <T> getInstance(clazz: Class<T>): T = factory.getInstance(clazz)
}
