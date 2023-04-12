package io.tezrok.api.maven

import io.tezrok.api.node.BaseNode

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
class ModuleNode(name: String, parent: BaseNode?) : BaseNode(name, parent) {
    private val resources: ResourcesNode = ResourcesNode(this)

    /**
     * Returns the "resources" node
     */
    fun getResources(): ResourcesNode = resources
}
