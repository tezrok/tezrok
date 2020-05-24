package io.tezrok.api.model.node

/**
 * Enumeration
 */
class EnumNode(name: String, description: String = "") : Node(name, KIND, description) {
    fun items(): List<EnumItemNode> = children().filterIsInstance<EnumItemNode>()

    companion object {
        const val KIND = "Enum"
    }
}

/**
 * Enumeration item
 */
class EnumItemNode(name: String, description: String = "") : Node(name, KIND, description) {
    companion object {
        const val KIND = "EnumItem"
    }
}
