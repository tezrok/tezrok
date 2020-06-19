package io.tezrok.api.model.node

/**
 * Entity with fields
 */
class EntityNode(name: String, parent: ModuleNode, description: String = "") : Node(name, KIND, description, parent) {
    fun fields(): List<FieldNode> = children().filterIsInstance<FieldNode>()

    fun add(field: FieldNode): EntityNode {
        super.add(field)
        return this
    }

    fun getPrimaryField(): FieldNode {
        val field = fields()
                .stream()
                .filter { f -> f.primary }
                .findFirst()

        if (field.isPresent) {
            return field.get()
        }

        throw IllegalStateException("Primary field not found in entity '$name'")
    }


    override val parent: ModuleNode
        get() = super.parent as ModuleNode

    companion object {
        const val KIND = "Entity"
    }
}
