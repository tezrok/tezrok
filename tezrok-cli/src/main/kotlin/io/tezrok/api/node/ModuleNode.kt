package io.tezrok.api.node

import io.tezrok.api.Schema

/**
 * Represents a module. Which represents a separate maven module
 * which can be a library or a web application
 */
class ModuleNode(name: String, parent: BaseNode?) : BaseNode(name, parent) {
    private val resources: ResourcesNode = ResourcesNode(this)
    private val entities: MutableList<EntityNode> = mutableListOf()

    // TODO: find appropriate place for schema
    var schema: Schema? = null

    fun getEntities(): List<EntityNode> = entities

    /**
     * Returns the "resources" node
     */
    fun getResources(): ResourcesNode = resources

    fun addEntity(name: String) {
        // TODO: check if entity already exists
        entities.add(EntityNode(name, this))
    }
}
