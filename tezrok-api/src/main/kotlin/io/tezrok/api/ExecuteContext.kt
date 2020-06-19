package io.tezrok.api

import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.service.Service
import io.tezrok.api.service.Visitor

/**
 * Used while generating
 */
interface ExecuteContext : GlobalContext {
    /**
     * Get current phase
     */
    val phase: Phase

    /**
     * Gets project
     */
    val project: ProjectNode

    /**
     * Gets current generating module
     */
    val module: ModuleNode

    /**
     * Generate comment if possible with time of generation
     */
    val generateTime: Boolean

    /**
     * Overwrite target file if it already exists
     */
    val overwriteIfExists: Boolean

    /**
     * Returns Type by specified class
     */
    fun ofType(clazz: Class<*>): Type

    /**
     * Returns Type by name with current module's package
     */
    fun ofType(name: String, subPath: String = ""): Type

    /**
     * Call specified visitors
     */
    fun <T : Visitor> applyVisitors(clazz: Class<T>, action: (T) -> Unit)

    /**
     * Render specified builder
     */
    fun render(builder: Builder)
}

enum class Phase {
    Init,

    Generate
}
