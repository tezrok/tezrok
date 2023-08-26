package io.tezrok.api.maven

import io.tezrok.api.input.ModuleElem
import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.DirectoryNode
import java.util.Collections

/**
 * Represents a model of maven project generation
 */
open class ProjectNode(name: String) : DirectoryNode(name, null) {
    private val modules: MutableList<ModuleNode> = mutableListOf()

    val pom: PomNode = PomNode(artifactId = name, parent = this)

    init {
        pom.dependencyId = pom.dependencyId.withPackaging("pom")
    }

    fun getModules(): List<ModuleNode> = Collections.unmodifiableList(modules.toList())

    override fun getFiles(): List<BaseFileNode> = Collections.unmodifiableList(listOf(pom) + modules + super.getFiles())

    fun addModule(moduleElem: ModuleElem): ModuleNode {
        // TODO: check if module already exists
        // TODO: validate module name
        val module = ModuleNode(moduleElem.name, this, moduleElem)
        modules.add(module)
        pom.getModulesRefNode().addModule(moduleElem.name)
        return module
    }

    /**
     * Removes modules from the project
     */
    fun removeModules(list: List<String>): Boolean {
        val success = modules.removeAll { list.contains(it.getName()) }
        val modulesRefNode = pom.getModulesRefNode()
        list.forEach { modulesRefNode.removeModule(it) }

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
