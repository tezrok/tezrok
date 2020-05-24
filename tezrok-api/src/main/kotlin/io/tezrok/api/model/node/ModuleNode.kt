package io.tezrok.api.model.node

class ModuleNode(name: String, description: String = "") : Node(name, KIND, description) {
    fun entities(): List<EntityNode> = children().filterIsInstance<EntityNode>()

    fun services(): List<ServiceNode> = children().filterIsInstance<ServiceNode>()

    fun enums(): List<EnumNode> = children().filterIsInstance<EnumNode>()

    companion object {
        const val KIND = "Module"
    }
}
