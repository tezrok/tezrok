package io.tezrok.core.factory

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.model.node.ProjectNode
import java.io.File

/**
 * Creates instances of all classes
 */
interface Factory {
    fun <T> getInstance(clazz: Class<T>): T

    fun <T> getInstance(clazz: Class<T>, context: ExecuteContext): T

    fun getGenerator(className: String): Generator

    fun getProject(): ProjectNode

    fun getTargetDir(): File
}
