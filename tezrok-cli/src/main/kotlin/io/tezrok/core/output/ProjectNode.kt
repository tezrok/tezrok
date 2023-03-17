package io.tezrok.core.output

/**
 * Represents a target file project
 */
class ProjectNode : BaseNode {
    private val modules: MutableList<ModuleNode> = mutableListOf()

    // TODO: edit project name
    override fun getName(): String = "Project"

    fun getModules(): List<ModuleNode> = modules

    fun addModule(name: String) {
        // TODO: check if module already exists
        modules.add(ModuleNode(name))
    }

    /**
     * Removes modules from the project
     */
    fun removeModule(list: List<String>): Boolean {
        TODO("Not yet implemented")
    }
}
