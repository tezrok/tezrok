package io.tezrok.api

import io.tezrok.api.builder.Builder
import io.tezrok.api.builder.type.Type
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode

/**
 * Used while generating
 */
interface ExecuteContext {
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
     * Returns Type by specified class
     */
    fun ofType(clazz: Class<*>): Type

    /**
     * Returns Type by name with current module's package
     */
    fun ofType(name: String): Type

    /**
     * Gets instance of specified class
     */
    fun <T> getInstance(clazz: Class<T>): T

    /**
     * Render specified builder
     */
    fun render(builder: Builder)

    val overwriteIfExists: Boolean
}

enum class Phase {
    Init,

    Generate
}
