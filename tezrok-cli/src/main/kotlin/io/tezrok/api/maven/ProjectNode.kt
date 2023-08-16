package io.tezrok.api.maven

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.DirectoryNode
import java.util.Collections

/**
 * Represents a model of maven project generation
 */
open class ProjectNode(name: String) : DirectoryNode(name, null) {
    private val modules: MutableList<ModuleNode> = mutableListOf()

    fun getModules(): List<ModuleNode> = Collections.unmodifiableList(modules.toList())

    override fun getFiles(): List<BaseFileNode> = Collections.unmodifiableList(modules + super.getFiles())

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

    /**
     * TODO: remove this temporal method when multiple modules are supported
     */
    fun getSingleModule(): ModuleNode {
        check(modules.isNotEmpty()) { "No modules found" }
        check(modules.size == 1) { "TODO: Support multiple modules" }
        return modules.first()
    }
}
