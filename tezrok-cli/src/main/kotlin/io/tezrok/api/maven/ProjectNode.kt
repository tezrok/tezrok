package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode

/**
 * Represents a model of maven project generation
 */
open class ProjectNode(name: String) : BaseNode(name, null) {
    private val modules: MutableList<ModuleNode> = mutableListOf()

    fun getModules(): List<ModuleNode> = modules

    fun addModule(name: String): ModuleNode {
        // TODO: check if module already exists
        // TODO: validate module name
        val module = ModuleNode(name, this)
        modules.add(module)
        return module
    }

    /**
     * Removes modules from the project
     */
    fun removeModules(list: List<String>): Boolean {
        val success = modules.removeAll { list.contains(it.getName()) }
        log.debug("From project {} removed {} modules: {}, success: {}", getName(), list.size, list, success)
        return success
    }
}
