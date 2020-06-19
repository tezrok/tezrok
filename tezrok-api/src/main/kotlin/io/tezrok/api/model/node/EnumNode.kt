package io.tezrok.api.model.node

/**
 * Enumeration
 */
class EnumNode(name: String, parent: ModuleNode, description: String = "") : Node(name, KIND, description, parent) {
    fun items(): List<EnumItemNode> = children().filterIsInstance<EnumItemNode>()

    override val parent: ModuleNode
        get() = super.parent as ModuleNode

    companion object {
        const val KIND = "Enum"
        const val NAME_MAX_SIZE = 100L
    }
}

/**
 * Enumeration item
 */
class EnumItemNode(name: String, parent: EnumNode, description: String = "") : Node(name, KIND, description, parent) {
    companion object {
        const val KIND = "EnumItem"
    }
}
