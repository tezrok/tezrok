package com.tezrok.api.node

/**
 * Interface representing a project node
 */
interface ProjectNode : Node {
    /**
     * Get all modules of this project
     */
    fun getModules(): ModuleNodes

    /**
     * Global settings of this project
     */
    fun getSettings(): ProjectSettingsNode
}
