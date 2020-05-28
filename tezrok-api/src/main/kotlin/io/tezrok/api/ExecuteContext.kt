package io.tezrok.api

import io.tezrok.api.builder.Builder
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.model.node.ProjectNode

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

    fun <T> getInstance(clazz: Class<T>): T

    /**
     * Render specified builder
     */
    fun render(builder: Builder)
}

enum class Phase {
    Init,

    Generate
}
