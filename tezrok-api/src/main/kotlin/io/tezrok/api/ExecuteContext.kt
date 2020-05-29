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
    fun getPhase(): Phase

    /**
     * Gets project
     */
    fun getProject(): ProjectNode

    /**
     * Gets current generating module
     */
    fun getModule(): ModuleNode

    /**
     * Generate comment if possible with time of generation
     */
    fun isGenerateTime(): Boolean

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

    fun overwriteIfExists(): Boolean
}

enum class Phase {
    Init,

    Generate
}
