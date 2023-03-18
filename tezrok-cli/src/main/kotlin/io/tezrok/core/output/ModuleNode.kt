package io.tezrok.core.output

import io.tezrok.core.common.BaseNode

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
class ModuleNode(name: String, parent: BaseNode?) : BaseNode(name, parent) {
    private val resource: ResourceNode = ResourceNode()
    private val entities: MutableList<EntityNode> = mutableListOf()

    fun getEntities(): List<EntityNode> = entities

    fun getResource(): ResourceNode = resource

    fun addEntity(name: String) {
        // TODO: check if entity already exists
        entities.add(EntityNode(name))
    }
}
