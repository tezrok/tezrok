package io.tezrok.core.output

/**
 * Represents an entity. Which represents a separate type
 */
class EntityNode(private val name: String) : BaseNode {
    override fun getName(): String = name
}
