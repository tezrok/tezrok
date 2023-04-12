package io.tezrok.api.maven

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
open class ModuleNode(name: String, parent: BaseNode?) : DirectoryNode(name, parent) {
    private val resources: ResourcesNode = ResourcesNode(this)

    /**
     * Returns the "resources" node
     */
    fun getResources(): ResourcesNode = resources

    override fun getFiles(): List<BaseFileNode> = listOf(resources)
}
