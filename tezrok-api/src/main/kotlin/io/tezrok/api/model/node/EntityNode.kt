package io.tezrok.api.model.node

/**
 * Entity with fields
 */
class EntityNode(name: String, description: String = "") : Node(name, KIND, description) {
    fun fields(): List<FieldNode> = children().filterIsInstance<FieldNode>()

    fun add(field: FieldNode): EntityNode {
        super.add(field)
        return this
    }

    companion object {
        const val KIND = "Entity"
    }
}
