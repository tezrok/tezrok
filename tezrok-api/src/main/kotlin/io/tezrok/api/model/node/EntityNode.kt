package io.tezrok.api.model.node

/**
 * Entity with fields
 */
class EntityNode(name: String, description: String = "") : Node(name, KIND, description) {
    fun fields(): List<FieldNode> = children().filterIsInstance<FieldNode>()

    companion object {
        const val KIND = "Entity"
    }
}
