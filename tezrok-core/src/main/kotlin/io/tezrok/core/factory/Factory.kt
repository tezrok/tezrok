package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.Service
import io.tezrok.api.service.Visitor
import java.io.File

/**
 * Creates instances of all classes
 */
interface Factory {
    fun <T> getInstance(clazz: Class<T>): T

    fun <T> getInstance(clazz: Class<T>, context: ExecuteContext): T

    fun createService(className: String): Service

    fun getProject(): ProjectNode

    fun getTargetDir(): File

    fun resolveType(name: String, module: ModuleNode): Type

    fun resolveType(field: FieldNode): Type

    fun <T : Service> getServiceList(clazz: Class<T>): Set<T>

    fun <T : Visitor> applyVisitors(clazz: Class<T>, action: (T) -> Unit)
}
