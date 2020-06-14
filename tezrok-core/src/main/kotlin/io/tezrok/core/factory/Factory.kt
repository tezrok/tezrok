package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.Service
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

    fun resolveType(name: String, context: ExecuteContext): Type
}
